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

      CREATE TABLE IF NOT EXISTS dictionaries (
        id SERIAL PRIMARY KEY,
        key VARCHAR(100) NOT NULL,
        value VARCHAR(255) NOT NULL,
        UNIQUE(key, value)
      );
    `);

    const dictResult = await client.query('SELECT COUNT(*) FROM dictionaries');
    if (parseInt(dictResult.rows[0].count) === 0) {
      const defaultDicts = [
        { key: 'brands', value: 'Aura' },
        { key: 'brands', value: 'La Roche-Posay' },
        { key: 'brands', value: 'Vichy' },
        { key: 'brands', value: 'Bioderma' },
        { key: 'brands', value: 'CeraVe' },
        { key: 'brands', value: 'The Ordinary' },
        { key: 'brands', value: "Paula's Choice" },
        { key: 'brands', value: 'Cosrx' },
        { key: 'brands', value: 'Eucerin' },
        { key: 'brands', value: 'Nivea' },
        { key: 'categories', value: 'Очищение' },
        { key: 'categories', value: 'Увлажнение' },
        { key: 'categories', value: 'Сыворотки' },
        { key: 'categories', value: 'SPF' },
        { key: 'categories', value: 'Уход' },
        { key: 'categories', value: 'Маска' },
        { key: 'categories', value: 'Тоник' },
        { key: 'categories', value: 'Крем' },
        { key: 'categories', value: 'Масло' },
        { key: 'segments', value: 'Бюджетная' },
        { key: 'segments', value: 'Люкс' },
        { key: 'segments', value: 'Профессиональная' },
        { key: 'segments', value: 'Космецевтика' },
        { key: 'volumes', value: '15мл' },
        { key: 'volumes', value: '30мл' },
        { key: 'volumes', value: '50мл' },
        { key: 'volumes', value: '75мл' },
        { key: 'volumes', value: '100мл' },
        { key: 'volumes', value: '150мл' },
        { key: 'volumes', value: '200мл' },
        { key: 'volumes', value: '250мл' },
        { key: 'volumes', value: '500мл' },
        { key: 'volumes', value: '1л' },
      ];
      for (const dict of defaultDicts) {
        await client.query('INSERT INTO dictionaries (key, value) VALUES ($1, $2)', [dict.key, dict.value]);
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

app.get('/api/dictionaries/:key', async (req, res) => {
  try {
    const result = await query('SELECT value FROM dictionaries WHERE key=$1 ORDER BY id', [req.params.key]);
    res.json(result.rows.map(r => r.value));
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/dictionaries/:key', async (req, res) => {
  try {
    const { value } = req.body;
    const result = await query(
      'INSERT INTO dictionaries (key, value) VALUES ($1, $2) RETURNING *',
      [req.params.key, value]
    );
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.put('/api/dictionaries/:key', async (req, res) => {
  try {
    const { oldValue, newValue } = req.body;
    const result = await query(
      'UPDATE dictionaries SET value=$1 WHERE key=$2 AND value=$3 RETURNING *',
      [newValue, req.params.key, oldValue]
    );
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.delete('/api/dictionaries/:key/:value', async (req, res) => {
  try {
    await query('DELETE FROM dictionaries WHERE key=$1 AND value=$2', [req.params.key, req.params.value]);
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
