package com.consultorio.service;

import com.consultorio.dto.PublicTratamientoResponse;
import com.consultorio.dto.TratamientoRequest;
import com.consultorio.dto.TratamientoResponse;
import com.consultorio.exception.ResourceNotFoundException;
import com.consultorio.model.Tratamiento;
import com.consultorio.repository.TratamientoRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TratamientoService {

  private static final Logger log = LoggerFactory.getLogger(TratamientoService.class);

  private final TratamientoRepository tratamientoRepository;
  private final FileStorageService fileStorageService;

  public TratamientoService(TratamientoRepository tratamientoRepository,
      FileStorageService fileStorageService) {
    this.tratamientoRepository = tratamientoRepository;
    this.fileStorageService = fileStorageService;
  }

  public List<TratamientoResponse> findAll() {
    return tratamientoRepository.findAll().stream().map(this::toResponse).toList();
  }

  public List<TratamientoResponse> findAllActivos() {
    return tratamientoRepository.findByActivoTrue().stream().map(this::toResponse).toList();
  }

  public List<PublicTratamientoResponse> findPublicActivos() {
    List<PublicTratamientoResponse> activos = tratamientoRepository
        .findByActivoTrueOrderByNombreAsc()
        .stream()
        .map(this::toPublicResponse)
        .toList();
    log.info("Listando tratamientos públicos: {}", activos.size());
    return activos;
  }

  public TratamientoResponse findById(Long id) {
    Tratamiento tratamiento =
        tratamientoRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("Tratamiento no encontrado con id: " + id));
    return toResponse(tratamiento);
  }

  public TratamientoResponse create(TratamientoRequest request) {
    Tratamiento tratamiento = new Tratamiento();
    tratamiento.setNombre(request.getNombre());
    tratamiento.setDescripcion(request.getDescripcion());
    tratamiento.setPrecio(request.getPrecio());
    tratamiento.setActivo(request.getActivo() != null ? request.getActivo() : true);
    tratamiento.setFotoUrl(request.getFotoUrl());

    Tratamiento creado = tratamientoRepository.save(tratamiento);
    log.info("Tratamiento creado — id: {}, nombre: '{}', precio: {}", creado.getId(), creado.getNombre(), creado.getPrecio());
    return toResponse(creado);
  }

  public TratamientoResponse update(Long id, TratamientoRequest request) {
    Tratamiento existente =
        tratamientoRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("Tratamiento no encontrado con id: " + id));

    existente.setNombre(request.getNombre());
    existente.setDescripcion(request.getDescripcion());
    existente.setPrecio(request.getPrecio());
    existente.setActivo(request.getActivo());
    existente.setFotoUrl(request.getFotoUrl());

    Tratamiento actualizado = tratamientoRepository.save(existente);
    log.info("Tratamiento actualizado — id: {}, nombre: '{}'", actualizado.getId(), actualizado.getNombre());
    return toResponse(actualizado);
  }

  public TratamientoResponse toggleActivo(Long id) {
    Tratamiento tratamiento =
        tratamientoRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("Tratamiento no encontrado con id: " + id));

    tratamiento.setActivo(!tratamiento.getActivo());
    Tratamiento actualizado = tratamientoRepository.save(tratamiento);
    log.info("Tratamiento id {} '{}' marcado como {}", actualizado.getId(), actualizado.getNombre(),
        actualizado.getActivo() ? "activo" : "inactivo");
    return toResponse(actualizado);
  }

  public void delete(Long id) {
    findById(id);
    tratamientoRepository.deleteById(id);
    log.info("Tratamiento eliminado — id: {}", id);
  }

  public TratamientoResponse uploadFoto(Long id, MultipartFile foto) {
    Tratamiento tratamiento = tratamientoRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Tratamiento no encontrado con id: " + id));
    fileStorageService.deleteFile(tratamiento.getFotoUrl());
    String fotoUrl = fileStorageService.storeTratamientoFoto(foto);
    tratamiento.setFotoUrl(fotoUrl);
    Tratamiento actualizado = tratamientoRepository.save(tratamiento);
    log.info("Foto actualizada — tratamiento id: {}, url: {}", id, fotoUrl);
    return toResponse(actualizado);
  }

  private PublicTratamientoResponse toPublicResponse(Tratamiento tratamiento) {
    return new PublicTratamientoResponse(
        tratamiento.getId(),
        tratamiento.getNombre(),
        tratamiento.getDescripcion(),
        tratamiento.getPrecio(),
        tratamiento.getFotoUrl());
  }

  private TratamientoResponse toResponse(Tratamiento tratamiento) {
    return new TratamientoResponse(
        tratamiento.getId(),
        tratamiento.getNombre(),
        tratamiento.getDescripcion(),
        tratamiento.getPrecio(),
        tratamiento.getActivo(),
        tratamiento.getFotoUrl(),
        tratamiento.getCreatedAt());
  }
}
