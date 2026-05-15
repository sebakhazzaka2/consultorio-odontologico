package com.consultorio.service;

import com.consultorio.dto.PublicServicioResponse;
import com.consultorio.dto.ServicioRequest;
import com.consultorio.dto.ServicioResponse;
import com.consultorio.exception.ResourceNotFoundException;
import com.consultorio.model.Servicio;
import com.consultorio.repository.ServicioRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ServicioService {

  private static final Logger log = LoggerFactory.getLogger(ServicioService.class);

  private final ServicioRepository servicioRepository;
  private final FileStorageService fileStorageService;

  public ServicioService(ServicioRepository servicioRepository,
      FileStorageService fileStorageService) {
    this.servicioRepository = servicioRepository;
    this.fileStorageService = fileStorageService;
  }

  public List<ServicioResponse> findAll() {
    return servicioRepository.findAll().stream().map(this::toResponse).toList();
  }

  public List<ServicioResponse> findAllActivos() {
    return servicioRepository.findByActivoTrue().stream().map(this::toResponse).toList();
  }

  public List<PublicServicioResponse> findPublicActivos() {
    List<PublicServicioResponse> activos = servicioRepository
        .findByActivoTrueOrderByNombreAsc()
        .stream()
        .map(this::toPublicResponse)
        .toList();
    log.info("Listando servicios públicos: {}", activos.size());
    return activos;
  }

  public ServicioResponse findById(Long id) {
    Servicio servicio =
        servicioRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("Servicio no encontrado con id: " + id));
    return toResponse(servicio);
  }

  public ServicioResponse create(ServicioRequest request) {
    Servicio servicio = new Servicio();
    servicio.setNombre(request.getNombre());
    servicio.setDescripcion(request.getDescripcion());
    servicio.setPrecio(request.getPrecio());
    servicio.setActivo(request.getActivo() != null ? request.getActivo() : true);
    servicio.setFotoUrl(request.getFotoUrl());
    servicio.setDuracionMinutos(request.getDuracionMinutos());

    Servicio creado = servicioRepository.save(servicio);
    log.info("Servicio creado — id: {}, nombre: '{}', precio: {}", creado.getId(), creado.getNombre(), creado.getPrecio());
    return toResponse(creado);
  }

  public ServicioResponse update(Long id, ServicioRequest request) {
    Servicio existente =
        servicioRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("Servicio no encontrado con id: " + id));

    existente.setNombre(request.getNombre());
    existente.setDescripcion(request.getDescripcion());
    existente.setPrecio(request.getPrecio());
    existente.setActivo(request.getActivo());
    existente.setDuracionMinutos(request.getDuracionMinutos());

    Servicio actualizado = servicioRepository.save(existente);
    log.info("Servicio actualizado — id: {}, nombre: '{}'", actualizado.getId(), actualizado.getNombre());
    return toResponse(actualizado);
  }

  public ServicioResponse toggleActivo(Long id) {
    Servicio servicio =
        servicioRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("Servicio no encontrado con id: " + id));

    servicio.setActivo(!servicio.getActivo());
    Servicio actualizado = servicioRepository.save(servicio);
    log.info("Servicio id {} '{}' marcado como {}", actualizado.getId(), actualizado.getNombre(),
        actualizado.getActivo() ? "activo" : "inactivo");
    return toResponse(actualizado);
  }

  public void delete(Long id) {
    findById(id);
    servicioRepository.deleteById(id);
    log.info("Servicio eliminado — id: {}", id);
  }

  public ServicioResponse uploadFoto(Long id, MultipartFile foto) {
    Servicio servicio = servicioRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + id));
    fileStorageService.deleteFile(servicio.getFotoUrl());
    String fotoUrl = fileStorageService.storeServicioFoto(foto);
    servicio.setFotoUrl(fotoUrl);
    Servicio actualizado = servicioRepository.save(servicio);
    log.info("Foto actualizada — servicio id: {}, url: {}", id, fotoUrl);
    return toResponse(actualizado);
  }

  private PublicServicioResponse toPublicResponse(Servicio servicio) {
    return new PublicServicioResponse(
        servicio.getId(),
        servicio.getNombre(),
        servicio.getDescripcion(),
        servicio.getPrecio(),
        servicio.getFotoUrl(),
        servicio.getDuracionMinutos());
  }

  private ServicioResponse toResponse(Servicio servicio) {
    return new ServicioResponse(
        servicio.getId(),
        servicio.getNombre(),
        servicio.getDescripcion(),
        servicio.getPrecio(),
        servicio.getActivo(),
        servicio.getFotoUrl(),
        servicio.getDuracionMinutos(),
        servicio.getCreatedAt());
  }
}
