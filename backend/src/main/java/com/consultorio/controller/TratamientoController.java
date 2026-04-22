package com.consultorio.controller;

import com.consultorio.dto.TratamientoRequest;
import com.consultorio.dto.TratamientoResponse;
import com.consultorio.service.TratamientoService;
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
@RequestMapping("/tratamientos")
public class TratamientoController {

  private final TratamientoService tratamientoService;

  public TratamientoController(TratamientoService tratamientoService) {
    this.tratamientoService = tratamientoService;
  }

  @GetMapping
  public ResponseEntity<List<TratamientoResponse>> findAll() {
    return new ResponseEntity<>(tratamientoService.findAll(), HttpStatus.OK);
  }

  @GetMapping("/activos")
  public ResponseEntity<List<TratamientoResponse>> findAllActivos() {
    return new ResponseEntity<>(tratamientoService.findAllActivos(), HttpStatus.OK);
  }

  @GetMapping("/{id}")
  public ResponseEntity<TratamientoResponse> findById(@PathVariable("id") Long id) {
    return new ResponseEntity<>(tratamientoService.findById(id), HttpStatus.OK);
  }

  @PostMapping
  public ResponseEntity<TratamientoResponse> create(
      @Valid @RequestBody TratamientoRequest tratamientoRequest) {
    return new ResponseEntity<>(tratamientoService.create(tratamientoRequest), HttpStatus.CREATED);
  }

  @PutMapping("/{id}")
  public ResponseEntity<TratamientoResponse> update(
      @PathVariable("id") Long id, @Valid @RequestBody TratamientoRequest tratamientoRequest) {
    return new ResponseEntity<>(
        tratamientoService.update(id, tratamientoRequest), HttpStatus.OK);
  }

  @PatchMapping("/{id}/toggle")
  public ResponseEntity<TratamientoResponse> toggleActivo(@PathVariable("id") Long id) {
    return new ResponseEntity<>(tratamientoService.toggleActivo(id), HttpStatus.OK);
  }

  @PatchMapping(value = "/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<TratamientoResponse> uploadFoto(
      @PathVariable("id") Long id,
      @RequestParam("foto") MultipartFile foto) {
    return ResponseEntity.ok(tratamientoService.uploadFoto(id, foto));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
    tratamientoService.delete(id);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }
}
