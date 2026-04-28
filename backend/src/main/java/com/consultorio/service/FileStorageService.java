package com.consultorio.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

  private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
  private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

  private final Path uploadRoot;
  private final String contextPath;

  public FileStorageService(
      @Value("${app.uploads.dir:./uploads}") String uploadDir,
      @Value("${server.servlet.context-path:/api}") String contextPath) {
    this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    this.contextPath = contextPath;
    try {
      Files.createDirectories(this.uploadRoot);
    } catch (IOException e) {
      throw new IllegalStateException("No se pudo crear el directorio de uploads: " + uploadDir, e);
    }
  }

  public String storeTratamientoFoto(MultipartFile file) {
    String ext = extractExtension(file.getOriginalFilename());
    if (!ALLOWED_EXTENSIONS.contains(ext)) {
      throw new IllegalArgumentException(
          "Formato no permitido. Usá: " + String.join(", ", ALLOWED_EXTENSIONS));
    }

    String filename = UUID.randomUUID() + "." + ext;
    Path targetDir = uploadRoot.resolve("tratamientos");
    try {
      Files.createDirectories(targetDir);
      Files.copy(file.getInputStream(), targetDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new RuntimeException("Error al guardar la imagen", e);
    }

    String url = contextPath + "/uploads/tratamientos/" + filename;
    log.info("Foto guardada: {}", url);
    return url;
  }

  public void deleteFile(String fotoUrl) {
    if (fotoUrl == null || fotoUrl.isBlank()) return;
    String prefix = contextPath + "/uploads/";
    if (!fotoUrl.startsWith(prefix)) return;
    Path file = uploadRoot.resolve(fotoUrl.substring(prefix.length()));
    try {
      Files.deleteIfExists(file);
    } catch (IOException e) {
      log.warn("No se pudo eliminar el archivo: {}", fotoUrl, e);
    }
  }

  private String extractExtension(String filename) {
    if (filename == null || !filename.contains(".")) return "";
    return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
  }
}
