const db = require('../config/db');
const { validarPaciente } = require('../validators/pacientes.validator');

exports.obtenerPacientes = async (req, res) => {
  try {
    const [rows] = await db.execute(
      'SELECT id, nombre, apellido, telefono, email, fecha_nacimiento, created_at FROM pacientes ORDER BY apellido, nombre'
    );
    res.json(rows);
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'Error al obtener pacientes' });
  }
};

exports.obtenerPacientePorId = async (req, res) => {
  try {
    const id = parseInt(req.params.id, 10);
    if (isNaN(id) || id < 1) {
      return res.status(400).json({ error: 'ID de paciente inválido' });
    }
    const [rows] = await db.execute('SELECT * FROM pacientes WHERE id = ?', [id]);
    if (rows.length === 0) {
      return res.status(404).json({ error: 'Paciente no encontrado' });
    }
    res.json(rows[0]);
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'Error al obtener el paciente' });
  }
};

exports.crearPaciente = async (req, res) => {
  try {
    const resultado = validarPaciente(req.body);
    if (!resultado.valido) {
      return res.status(400).json({
        error: 'Error de validación',
        detalles: resultado.errores
      });
    }

    const { nombre, apellido, telefono, email, fecha_nacimiento } = resultado.bodyNormalizado;

    const sql = `
      INSERT INTO pacientes (nombre, apellido, telefono, email, fecha_nacimiento)
      VALUES (?, ?, ?, ?, ?)
    `;
    const [result] = await db.execute(sql, [nombre, apellido, telefono, email, fecha_nacimiento || null]);

    res.status(201).json({
      message: 'Paciente creado correctamente',
      id: result.insertId
    });
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'Error al crear el paciente' });
  }
};

exports.actualizarPaciente = async (req, res) => {
  try {
    const id = parseInt(req.params.id, 10);
    if (isNaN(id) || id < 1) {
      return res.status(400).json({ error: 'ID de paciente inválido' });
    }

    const resultado = validarPaciente(req.body);
    if (!resultado.valido) {
      return res.status(400).json({
        error: 'Error de validación',
        detalles: resultado.errores
      });
    }

    const [existentes] = await db.execute('SELECT id FROM pacientes WHERE id = ?', [id]);
    if (existentes.length === 0) {
      return res.status(404).json({ error: 'Paciente no encontrado' });
    }

    const { nombre, apellido, telefono, email, fecha_nacimiento } = resultado.bodyNormalizado;

    const sql = `
      UPDATE pacientes SET nombre = ?, apellido = ?, telefono = ?, email = ?, fecha_nacimiento = ?
      WHERE id = ?
    `;
    await db.execute(sql, [nombre, apellido, telefono, email, fecha_nacimiento || null, id]);

    res.json({ message: 'Paciente actualizado correctamente' });
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'Error al actualizar el paciente' });
  }
};

exports.eliminarPaciente = async (req, res) => {
  try {
    const id = parseInt(req.params.id, 10);
    if (isNaN(id) || id < 1) {
      return res.status(400).json({ error: 'ID de paciente inválido' });
    }

    const [citas] = await db.execute('SELECT id FROM citas WHERE paciente_id = ?', [id]);
    if (citas.length > 0) {
      return res.status(409).json({
        error: 'No se puede eliminar el paciente',
        detalles: ['El paciente tiene citas asociadas. Elimine o reasigne las citas primero.']
      });
    }

    const [result] = await db.execute('DELETE FROM pacientes WHERE id = ?', [id]);
    if (result.affectedRows === 0) {
      return res.status(404).json({ error: 'Paciente no encontrado' });
    }

    res.json({ message: 'Paciente eliminado correctamente' });
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'Error al eliminar el paciente' });
  }
};
