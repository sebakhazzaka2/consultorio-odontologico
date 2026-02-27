const express = require('express');
const router = express.Router();
const citasController = require('../controllers/citas.controller');

router.get('/', citasController.obtenerCitas);
router.get('/:id', citasController.obtenerCitaPorId);
router.post('/', citasController.crearCita);
router.put('/:id', citasController.actualizarCita);
module.exports = router;
