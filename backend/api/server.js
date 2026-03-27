const express = require('express');
const cors = require('cors');
const { Pool } = require('pg');

const app = express();
const PORT = process.env.PORT || 3001;

app.use(cors());
app.use(express.json({ limit: '10mb' }));

const pool = new Pool({
  host: process.env.DB_HOST || 'localhost',
  port: process.env.DB_PORT || 5432,
  database: process.env.DB_NAME || 'aura',
  user: process.env.DB_USER || 'aura_user',
  password: process.env.DB_PASSWORD || 'aura_password'
});

pool.on('connect', (client) => {
  client.query("SET client_encoding = 'UTF8'");
});

async function query(...args) {
  const client = await pool.connect();
  try {
    await client.query("SET client_encoding = 'UTF8'");
    return await client.query(...args);
  } finally {
    client.release();
  }
}

const initDB = async () => {
  const client = await pool.connect();
  try {
    await client.query(`
      CREATE TABLE IF NOT EXISTS products (
        id SERIAL PRIMARY KEY,
        name VARCHAR(255) NOT NULL,
        brand VARCHAR(255),
        category VARCHAR(255),
        description TEXT,
        images TEXT[],
        volume VARCHAR(50),
        segment VARCHAR(50),
        created_at TIMESTAMP DEFAULT NOW()
      );

      CREATE TABLE IF NOT EXISTS procedures (
        id SERIAL PRIMARY KEY,
        name VARCHAR(255) NOT NULL,
        category VARCHAR(255),
        duration INTEGER,
        price DECIMAL(10,2),
        description TEXT,
        contraindications TEXT,
        created_at TIMESTAMP DEFAULT NOW()
      );

      CREATE TABLE IF NOT EXISTS content (
        id SERIAL PRIMARY KEY,
        title VARCHAR(255) NOT NULL,
        type VARCHAR(50),
        body TEXT,
        image_url VARCHAR(500),
        published BOOLEAN DEFAULT false,
        created_at TIMESTAMP DEFAULT NOW()
      );

      CREATE TABLE IF NOT EXISTS users (
        id SERIAL PRIMARY KEY,
        name VARCHAR(255) NOT NULL,
        email VARCHAR(255) UNIQUE NOT NULL,
        role VARCHAR(50) DEFAULT 'user',
        avatar VARCHAR(500),
        created_at TIMESTAMP DEFAULT NOW()
      );

      CREATE TABLE IF NOT EXISTS brands (
        id SERIAL PRIMARY KEY,
        value VARCHAR(255) NOT NULL UNIQUE
      );

      CREATE TABLE IF NOT EXISTS categories (
        id SERIAL PRIMARY KEY,
        value VARCHAR(255) NOT NULL UNIQUE
      );

      CREATE TABLE IF NOT EXISTS segments (
        id SERIAL PRIMARY KEY,
        value VARCHAR(255) NOT NULL UNIQUE
      );

      CREATE TABLE IF NOT EXISTS volumes (
        id SERIAL PRIMARY KEY,
        value VARCHAR(255) NOT NULL UNIQUE
      );

      CREATE TABLE IF NOT EXISTS procedure_categories (
        id SERIAL PRIMARY KEY,
        value VARCHAR(255) NOT NULL UNIQUE
      );

      CREATE TABLE IF NOT EXISTS content_categories (
        id SERIAL PRIMARY KEY,
        value VARCHAR(255) NOT NULL UNIQUE
      );

      CREATE TABLE IF NOT EXISTS user_roles (
        id SERIAL PRIMARY KEY,
        value VARCHAR(255) NOT NULL UNIQUE
      );

      CREATE TABLE IF NOT EXISTS skin_types (
        id SERIAL PRIMARY KEY,
        value VARCHAR(255) NOT NULL UNIQUE
      );
    `);

    const brandsCount = await client.query('SELECT COUNT(*) FROM brands');
    if (parseInt(brandsCount.rows[0].count) === 0) {
      const defaultBrands = ['Aura', 'La Roche-Posay', 'Vichy', 'Bioderma', 'CeraVe', 'The Ordinary', "Paula's Choice", 'Cosrx', 'Eucerin', 'Nivea'];
      for (const value of defaultBrands) {
        await client.query('INSERT INTO brands (value) VALUES ($1)', [value]);
      }
    }

    const categoriesCount = await client.query('SELECT COUNT(*) FROM categories');
    if (parseInt(categoriesCount.rows[0].count) === 0) {
      const defaultCategories = ['Очищение', 'Увлажнение', 'Сыворотки', 'SPF', 'Уход', 'Маска', 'Тоник', 'Крем', 'Масло'];
      for (const value of defaultCategories) {
        await client.query('INSERT INTO categories (value) VALUES ($1)', [value]);
      }
    }

    const segmentsCount = await client.query('SELECT COUNT(*) FROM segments');
    if (parseInt(segmentsCount.rows[0].count) === 0) {
      const defaultSegments = ['Бюджетная', 'Люкс', 'Профессиональная', 'Космецевтика'];
      for (const value of defaultSegments) {
        await client.query('INSERT INTO segments (value) VALUES ($1)', [value]);
      }
    }

    const volumesCount = await client.query('SELECT COUNT(*) FROM volumes');
    if (parseInt(volumesCount.rows[0].count) === 0) {
      const defaultVolumes = ['15мл', '30мл', '50мл', '75мл', '100мл', '150мл', '200мл', '250мл', '500мл', '1л'];
      for (const value of defaultVolumes) {
        await client.query('INSERT INTO volumes (value) VALUES ($1)', [value]);
      }
    }

    const procedureCategoriesCount = await client.query('SELECT COUNT(*) FROM procedure_categories');
    if (parseInt(procedureCategoriesCount.rows[0].count) === 0) {
      const defaultProcedureCategories = ['Чистка', 'Увлажнение', 'Инъекции', 'Эпиляция', 'Массаж', 'Пилинг', 'Уход'];
      for (const value of defaultProcedureCategories) {
        await client.query('INSERT INTO procedure_categories (value) VALUES ($1)', [value]);
      }
    }

    const contentCategoriesCount = await client.query('SELECT COUNT(*) FROM content_categories');
    if (parseInt(contentCategoriesCount.rows[0].count) === 0) {
      const defaultContentCategories = ['Уход за кожей', 'Ингредиенты', 'Защита', 'Процедуры', 'Питание', 'Образ жизни'];
      for (const value of defaultContentCategories) {
        await client.query('INSERT INTO content_categories (value) VALUES ($1)', [value]);
      }
    }

    const userRolesCount = await client.query('SELECT COUNT(*) FROM user_roles');
    if (parseInt(userRolesCount.rows[0].count) === 0) {
      const defaultUserRoles = ['Пользователь', 'Косметолог', 'Менеджер', 'Администратор'];
      for (const value of defaultUserRoles) {
        await client.query('INSERT INTO user_roles (value) VALUES ($1)', [value]);
      }
    }

    const skinTypesCount = await client.query('SELECT COUNT(*) FROM skin_types');
    if (parseInt(skinTypesCount.rows[0].count) === 0) {
      const defaultSkinTypes = ['Нормальная', 'Сухая', 'Жирная', 'Комбинированная', 'Чувствительная'];
      for (const value of defaultSkinTypes) {
        await client.query('INSERT INTO skin_types (value) VALUES ($1)', [value]);
      }
    }

    console.log('Database initialized successfully');
  } catch (err) {
    console.error('Error initializing database:', err.message);
  } finally {
    client.release();
  }
};

app.get('/api/health', (req, res) => {
  res.json({ status: 'ok' });
});

app.get('/api/products', async (req, res) => {
  try {
    const result = await query('SELECT * FROM products ORDER BY id DESC');
    res.json(result.rows);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/products', async (req, res) => {
  try {
    const { name, brand, category, description, images, volume, segment } = req.body;
    const result = await query(
      'INSERT INTO products (name, brand, category, description, images, volume, segment) VALUES ($1, $2, $3, $4, $5, $6, $7) RETURNING *',
      [name, brand, category, description, images, volume, segment]
    );
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.put('/api/products/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const { name, brand, category, description, images, volume, segment } = req.body;
    const result = await query(
      'UPDATE products SET name=$1, brand=$2, category=$3, description=$4, images=$5, volume=$6, segment=$7 WHERE id=$8 RETURNING *',
      [name, brand, category, description, images, volume, segment, id]
    );
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.delete('/api/products/:id', async (req, res) => {
  try {
    await query('DELETE FROM products WHERE id=$1', [req.params.id]);
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

const dictTableMap = {
  brands: 'brands',
  categories: 'categories',
  segments: 'segments',
  volumes: 'volumes',
  procedureCategories: 'procedure_categories',
  contentCategories: 'content_categories',
  userRoles: 'user_roles',
  skinTypes: 'skin_types'
}

app.get('/api/dictionaries/:key', async (req, res) => {
  try {
    const table = dictTableMap[req.params.key]
    if (!table) {
      return res.status(400).json({ error: 'Unknown dictionary key' })
    }
    const result = await query(`SELECT value FROM ${table} ORDER BY id`);
    res.json(result.rows.map(r => r.value));
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/dictionaries/:key', async (req, res) => {
  try {
    const table = dictTableMap[req.params.key]
    if (!table) {
      return res.status(400).json({ error: 'Unknown dictionary key' })
    }
    const { value } = req.body;
    const result = await query(
      `INSERT INTO ${table} (value) VALUES ($1) RETURNING *`,
      [value]
    );
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.put('/api/dictionaries/:key', async (req, res) => {
  try {
    const table = dictTableMap[req.params.key]
    if (!table) {
      return res.status(400).json({ error: 'Unknown dictionary key' })
    }
    const { oldValue, newValue } = req.body;
    const result = await query(
      `UPDATE ${table} SET value=$1 WHERE value=$2 RETURNING *`,
      [newValue, oldValue]
    );
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.delete('/api/dictionaries/:key/:value', async (req, res) => {
  try {
    const table = dictTableMap[req.params.key]
    if (!table) {
      return res.status(400).json({ error: 'Unknown dictionary key' })
    }
    await query(`DELETE FROM ${table} WHERE value=$1`, [req.params.value]);
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.get('/api/procedures', async (req, res) => {
  try {
    const result = await query('SELECT * FROM procedures ORDER BY id DESC');
    res.json(result.rows);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/procedures', async (req, res) => {
  try {
    const { name, category, duration, price, description, contraindications } = req.body;
    const result = await query(
      'INSERT INTO procedures (name, category, duration, price, description, contraindications) VALUES ($1, $2, $3, $4, $5, $6) RETURNING *',
      [name, category, duration, price, description, contraindications]
    );
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.put('/api/procedures/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const { name, category, duration, price, description, contraindications } = req.body;
    const result = await query(
      'UPDATE procedures SET name=$1, category=$2, duration=$3, price=$4, description=$5, contraindications=$6 WHERE id=$7 RETURNING *',
      [name, category, duration, price, description, contraindications, id]
    );
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.delete('/api/procedures/:id', async (req, res) => {
  try {
    await query('DELETE FROM procedures WHERE id=$1', [req.params.id]);
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.get('/api/content', async (req, res) => {
  try {
    const result = await query('SELECT * FROM content ORDER BY id DESC');
    res.json(result.rows);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/content', async (req, res) => {
  try {
    const { title, type, body, image_url, published } = req.body;
    const result = await query(
      'INSERT INTO content (title, type, body, image_url, published) VALUES ($1, $2, $3, $4, $5) RETURNING *',
      [title, type, body, image_url, published]
    );
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.put('/api/content/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const { title, type, body, image_url, published } = req.body;
    const result = await query(
      'UPDATE content SET title=$1, type=$2, body=$3, image_url=$4, published=$5 WHERE id=$6 RETURNING *',
      [title, type, body, image_url, published, id]
    );
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.delete('/api/content/:id', async (req, res) => {
  try {
    await query('DELETE FROM content WHERE id=$1', [req.params.id]);
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.get('/api/users', async (req, res) => {
  try {
    const result = await query('SELECT * FROM users ORDER BY id DESC');
    res.json(result.rows);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/users', async (req, res) => {
  try {
    const { name, email, role, avatar } = req.body;
    const result = await query(
      'INSERT INTO users (name, email, role, avatar) VALUES ($1, $2, $3, $4) RETURNING *',
      [name, email, role, avatar]
    );
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.put('/api/users/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const { name, email, role, avatar } = req.body;
    const result = await query(
      'UPDATE users SET name=$1, email=$2, role=$3, avatar=$4 WHERE id=$5 RETURNING *',
      [name, email, role, avatar, id]
    );
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.delete('/api/users/:id', async (req, res) => {
  try {
    await query('DELETE FROM users WHERE id=$1', [req.params.id]);
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

initDB().then(() => {
  app.listen(PORT, () => {
    console.log(`API server running on port ${PORT}`);
  });
});
