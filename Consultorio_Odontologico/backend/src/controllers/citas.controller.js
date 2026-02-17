const db = require('../config/db');

exports.crearCita = async (req, res) => {
  try {
    const { nombre, telefono, email, fecha, hora, motivo } = req.body;

    const sql = `
      INSERT INTO citas (nombre, telefono, email, fecha, hora, motivo)
      VALUES (?, ?, ?, ?, ?, ?)
    `;

    await db.execute(sql, [nombre, telefono, email, fecha, hora, motivo]);

    res.status(201).json({ message: 'Cita creada correctamente' });
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'Error al crear la cita' });
  }
};

exports.obtenerCitas = async (req, res) => {
  try {
    const [rows] = await db.execute('SELECT * FROM citas ORDER BY created_at DESC');
    res.json(rows);
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'Error al obtener citas' });
  }
};