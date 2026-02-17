const express = require('express');
const router = express.Router();
const citasController = require('../controllers/citas.controller');

router.get('/', citasController.obtenerCitas);
router.post('/', citasController.crearCita);

module.exports = router;
