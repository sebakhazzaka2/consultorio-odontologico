package com.consultorio.controller;

import com.consultorio.dto.ImportacionResultado;
import com.consultorio.dto.PacienteRequest;
import com.consultorio.dto.PacienteResponse;
import com.consultorio.service.PacienteService;
import com.consultorio.service.importacion.PacienteImportService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/pacientes")
public class PacienteController {

  private final PacienteService pacienteService;
  private final PacienteImportService pacienteImportService;

  public PacienteController(
      PacienteService pacienteService, PacienteImportService pacienteImportService) {
    this.pacienteService = pacienteService;
    this.pacienteImportService = pacienteImportService;
  }

  /**
   * Importa pacientes desde un CSV (campo multipart {@code archivo}). Por defecto es una
   * simulación que no escribe nada; con {@code confirmar=true} inserta las filas OK.
   * Ver docs/IMPORTACION_PACIENTES.md.
   */
  @PostMapping(value = "/importar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ImportacionResultado> importar(
      @RequestParam("archivo") MultipartFile archivo,
      @RequestParam(name = "confirmar", defaultValue = "false") boolean confirmar) {
    return ResponseEntity.ok(pacienteImportService.importar(archivo, confirmar));
  }

  @GetMapping
  public ResponseEntity<List<PacienteResponse>> findAll() {
    return new ResponseEntity<>(pacienteService.findAll(), HttpStatus.OK);
  }

  @GetMapping("/{id}")
  public ResponseEntity<PacienteResponse> findById(@PathVariable("id") Long id) {
    return new ResponseEntity<>(pacienteService.findById(id), HttpStatus.OK);
  }

  @PostMapping
  public ResponseEntity<PacienteResponse> create(@Valid @RequestBody PacienteRequest pacienteRequest) {
    return new ResponseEntity<>(pacienteService.create(pacienteRequest), HttpStatus.CREATED);
  }

  @PutMapping("/{id}")
  public ResponseEntity<PacienteResponse> update(
      @PathVariable("id") Long id, @Valid @RequestBody PacienteRequest pacienteRequest) {
    return new ResponseEntity<>(pacienteService.update(id, pacienteRequest), HttpStatus.OK);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
    pacienteService.delete(id);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }
}
