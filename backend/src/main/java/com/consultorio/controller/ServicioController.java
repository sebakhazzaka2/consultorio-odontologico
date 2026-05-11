package com.consultorio.controller;

import com.consultorio.dto.ServicioRequest;
import com.consultorio.dto.ServicioResponse;
import com.consultorio.service.ServicioService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/servicios")
public class ServicioController {

  private final ServicioService servicioService;

  public ServicioController(ServicioService servicioService) {
    this.servicioService = servicioService;
  }

  @GetMapping
  public ResponseEntity<List<ServicioResponse>> findAll() {
    return new ResponseEntity<>(servicioService.findAll(), HttpStatus.OK);
  }

  @GetMapping("/activos")
  public ResponseEntity<List<ServicioResponse>> findAllActivos() {
    return new ResponseEntity<>(servicioService.findAllActivos(), HttpStatus.OK);
  }

  @GetMapping("/{id}")
  public ResponseEntity<ServicioResponse> findById(@PathVariable("id") Long id) {
    return new ResponseEntity<>(servicioService.findById(id), HttpStatus.OK);
  }

  @PostMapping
  public ResponseEntity<ServicioResponse> create(
      @Valid @RequestBody ServicioRequest servicioRequest) {
    return new ResponseEntity<>(servicioService.create(servicioRequest), HttpStatus.CREATED);
  }

  @PutMapping("/{id}")
  public ResponseEntity<ServicioResponse> update(
      @PathVariable("id") Long id, @Valid @RequestBody ServicioRequest servicioRequest) {
    return new ResponseEntity<>(
        servicioService.update(id, servicioRequest), HttpStatus.OK);
  }

  @PatchMapping("/{id}/toggle")
  public ResponseEntity<ServicioResponse> toggleActivo(@PathVariable("id") Long id) {
    return new ResponseEntity<>(servicioService.toggleActivo(id), HttpStatus.OK);
  }

  @PatchMapping(value = "/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ServicioResponse> uploadFoto(
      @PathVariable("id") Long id,
      @RequestParam("foto") MultipartFile foto) {
    return ResponseEntity.ok(servicioService.uploadFoto(id, foto));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
    servicioService.delete(id);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }
}
