import os
import pg8000
from queue import Queue, Empty
import threading

class SimpleConnectionPool:
    def __init__(self, min_conn, max_conn, host, port, database, user, password):
        self.min_conn = min_conn
        self.max_conn = max_conn
        self.host = host
        self.port = port
        self.database = database
        self.user = user
        self.password = password
        self.pool = Queue(max_conn)
        self.lock = threading.Lock()
        
        for _ in range(min_conn):
            conn = self._create_connection()
            self.pool.put(conn)
    
    def _create_connection(self):
        return pg8000.connect(
            host=self.host,
            port=self.port,
            database=self.database,
            user=self.user,
            password=self.password
        )
    
    def getconn(self):
        try:
            conn = self.pool.get(block=False)
            try:
                conn.run("SELECT 1")
                return conn
            except:
                conn = self._create_connection()
                return conn
        except Empty:
            with self.lock:
                if self.pool.qsize() < self.max_conn:
                    return self._create_connection()
            return self.pool.get(block=True, timeout=10)
    
    def putconn(self, conn):
        try:
            self.pool.put_nowait(conn)
        except:
            conn.close()
    
    def close(self):
        while not self.pool.empty():
            try:
                conn = self.pool.get_nowait()
                conn.close()
            except Empty:
                break


_connection_pool = None


def get_pool():
    return _connection_pool


def get_db_pool():
    return _connection_pool


def init_db():
    global _connection_pool
    
    db_host = os.getenv("DB_HOST", "localhost")
    db_port = os.getenv("DB_PORT", "5432")
    db_name = os.getenv("DB_NAME", "beauty_db")
    db_user = os.getenv("DB_USER", "postgres")
    db_password = os.getenv("DB_PASSWORD", "postgres")
    
    for attempt in range(5):
        try:
            _connection_pool = SimpleConnectionPool(
                min_conn=1,
                max_conn=3,
                host=db_host,
                port=int(db_port),
                database=db_name,
                user=db_user,
                password=db_password
            )
            break
        except Exception as e:
            print(f"Attempt {attempt+1} failed: {e}")
            if attempt < 4:
                import time
                time.sleep(3)
            else:
                raise e
    
    conn = _connection_pool.getconn()
    cursor = conn.cursor()
    
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS products (
            id SERIAL PRIMARY KEY,
            name VARCHAR(255) NOT NULL,
            what_is_it TEXT,
            brand VARCHAR(255),
            product_type VARCHAR(50),
            for_whom VARCHAR(50),
            purpose VARCHAR(255),
            skin_type VARCHAR(50),
            application_time VARCHAR(50),
            area VARCHAR(50),
            active_ingredient TEXT,
            volume VARCHAR(50),
            segment VARCHAR(50),
            composition TEXT,
            application_info TEXT,
            country VARCHAR(100),
            country_origin VARCHAR(100),
            manufacturer VARCHAR(255),
            description TEXT,
            photos JSONB,
            has_video BOOLEAN DEFAULT FALSE,
            video BYTEA,
            created_at TIMESTAMP DEFAULT NOW()
        );
    """)
    
    try:
        cursor.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS country_origin VARCHAR(100)")
    except:
        pass
    
    # Fix video column type from BYTEA to VARCHAR for file paths
    try:
        cursor.execute("ALTER TABLE products ALTER COLUMN video TYPE VARCHAR(500) USING CASE WHEN video IS NOT NULL THEN 'legacy' ELSE NULL END")
    except:
        pass
    
    _seed_dictionary(cursor, "brands", [
        'Aura', 'La Roche-Posay', 'Vichy', 'Bioderma', 'CeraVe', 
        'The Ordinary', "Paula's Choice", 'Cosrx', 'Eucerin', 'Nivea'
    ])
    
    # Add description column to brands if not exists
    try:
        cursor.execute("ALTER TABLE brands ADD COLUMN IF NOT EXISTS description TEXT")
    except:
        pass
    
    try:
        cursor.execute("ALTER TABLE brands ADD COLUMN IF NOT EXISTS country VARCHAR(100)")
    except:
        pass
    
    try:
        cursor.execute("ALTER TABLE brands ADD COLUMN IF NOT EXISTS country_origin VARCHAR(100)")
    except:
        pass
    
    try:
        cursor.execute("ALTER TABLE brands ADD COLUMN IF NOT EXISTS manufacturer VARCHAR(255)")
    except:
        pass
    _seed_dictionary(cursor, "categories", [
        'Очищение', 'Увлажнение', 'Сыворотки', 'SPF', 'Уход', 'Маска', 'Тоник', 'Крем', 'Масло'
    ])
    _seed_dictionary(cursor, "segments", [
        'Бюджетная', 'Люкс', 'Профессиональная', 'Космецевтика'
    ])
    _seed_dictionary(cursor, "volumes", [
        '15мл', '30мл', '50мл', '75мл', '100мл', '150мл', '200мл', '250мл', '500мл', '1л'
    ])
    _seed_dictionary(cursor, "procedure_categories", [
        'Аппаратная косметология', 'Инъекционная косметология', 'Эстетическая косметология'
    ])
    _seed_dictionary(cursor, "content_categories", [
        'Уход за кожей', 'Ингредиенты', 'Защита', 'Процедуры', 'Питание', 'Образ жизни'
    ])
    _seed_dictionary(cursor, "user_roles", [
        'Пользователь', 'Косметолог', 'Менеджер', 'Администратор'
    ])
    _seed_dictionary(cursor, "skin_types", [
        'Нормальная', 'Сухая', 'Жирная', 'Комбинированная', 'Чувствительная'
    ])
    _seed_dictionary(cursor, "product_types", [
        'Крем', 'Сыворотка', 'Лосьон', 'Тоник', 'Маска', 'Масло', 'Спрей', 'Гель', 'Эмульсия', 'Бальзам'
    ])
    _seed_dictionary(cursor, "for_whom", [
        'Универсальный', 'Мужчинам', 'Женщинам', 'Детям', 'Беременным'
    ])
    _seed_dictionary(cursor, "purposes", [
        'Увлажнение', 'Питание', 'Очищение', 'Омоложение', 'Отбеливание', 'Защита от солнца', 'Против акне', 'Восстановление'
    ])
    _seed_dictionary(cursor, "application_times", [
        'Утро', 'Вечер', 'Утро и вечер', 'По необходимости'
    ])
    _seed_dictionary(cursor, "areas", [
        'Лицо', 'Тело', 'Волосы', 'Губы', 'Глаза', 'Шея', 'Руки', 'Ноги'
    ])
    _seed_dictionary(cursor, "countries", [
        'Франция', 'Корея', 'Япония', 'США', 'Германия', 'Швейцария', 'Россия', 'Италия', 'Испания', 'Израиль'
    ])

    # Seed procedure dictionaries
    def _seed_procedure_dict(cursor, table: str, values: list):
        try:
            cursor.execute(f"SELECT COUNT(*) FROM {table}")
            count = cursor.fetchone()[0]
            if count == 0:
                for value in values:
                    cursor.execute(f"INSERT INTO {table} (value) VALUES (%s)", (value,))
        except:
            pass  # Table might not exist yet

    # Skip procedure dictionary seeding since tables don't exist in main.py
    # They will be created manually or via separate migration
    
    try:
        cursor.execute("ALTER TABLE procedures ADD COLUMN IF NOT EXISTS direction VARCHAR(50)")
    except: pass

    try:
        cursor.execute("ALTER TABLE procedures ADD COLUMN IF NOT EXISTS method_type VARCHAR(100)")
    except: pass

    try:
        cursor.execute("ALTER TABLE procedures ADD COLUMN IF NOT EXISTS duration VARCHAR(50)")
    except: pass

    try:
        cursor.execute("ALTER TABLE procedures ADD COLUMN IF NOT EXISTS equipment VARCHAR(255)")
    except: pass

    try:
        cursor.execute("ALTER TABLE procedures ADD COLUMN IF NOT EXISTS zones JSONB")
    except: pass

    try:
        cursor.execute("ALTER TABLE procedures ADD COLUMN IF NOT EXISTS effects JSONB")
    except: pass

    try:
        cursor.execute("ALTER TABLE procedures ADD COLUMN IF NOT EXISTS problems JSONB")
    except: pass

    try:
        cursor.execute("ALTER TABLE procedures ADD COLUMN IF NOT EXISTS procedure_about TEXT")
    except: pass

    try:
        cursor.execute("ALTER TABLE procedures ADD COLUMN IF NOT EXISTS advantages TEXT")
    except: pass

    try:
        cursor.execute("ALTER TABLE procedures ADD COLUMN IF NOT EXISTS indications TEXT")
    except: pass

    try:
        cursor.execute("ALTER TABLE procedures ADD COLUMN IF NOT EXISTS principle TEXT")
    except: pass

    try:
        cursor.execute("ALTER TABLE procedures ADD COLUMN IF NOT EXISTS how_it_goes TEXT")
    except: pass

    try:
        cursor.execute("ALTER TABLE procedures ADD COLUMN IF NOT EXISTS for_whom TEXT")
    except: pass

    try:
        cursor.execute("ALTER TABLE procedures ADD COLUMN IF NOT EXISTS problems_solved TEXT")
    except: pass

    try:
        cursor.execute("ALTER TABLE procedures ADD COLUMN IF NOT EXISTS contraindications_full TEXT")
    except: pass

    try:
        cursor.execute("ALTER TABLE procedures ADD COLUMN IF NOT EXISTS preparation TEXT")
    except: pass

    try:
        cursor.execute("ALTER TABLE procedures ADD COLUMN IF NOT EXISTS recommended_course TEXT")
    except: pass

    try:
        cursor.execute("ALTER TABLE procedures ADD COLUMN IF NOT EXISTS rehabilitation TEXT")
    except: pass

    try:
        cursor.execute("ALTER TABLE procedures ADD COLUMN IF NOT EXISTS post_care TEXT")
    except: pass

    try:
        cursor.execute("ALTER TABLE procedures ADD COLUMN IF NOT EXISTS side_effects TEXT")
    except: pass

    try:
        cursor.execute("ALTER TABLE procedures ADD COLUMN IF NOT EXISTS photos JSONB")
    except: pass
    
    conn.commit()
    cursor.close()
    _connection_pool.putconn(conn)
    
    print("Database initialized successfully")


def _seed_dictionary(cursor, table: str, values: list):
    cursor.execute(f"SELECT COUNT(*) FROM {table}")
    count = cursor.fetchone()[0]
    if count == 0:
        for value in values:
            cursor.execute(f"INSERT INTO {table} (value) VALUES (%s)", (value,))
