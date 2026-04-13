package com.consultorio.service;

import com.consultorio.model.User;
import com.consultorio.repository.UserRepository;
import com.consultorio.security.JwtService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  private static final Logger log = LoggerFactory.getLogger(AuthService.class);

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;

  public AuthService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      AuthenticationManager authenticationManager,
      JwtService jwtService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.authenticationManager = authenticationManager;
    this.jwtService = jwtService;
  }

  public Map<String, String> login(String email, String password) {
    log.info("Intento de login — usuario: {}", email);
    try {
      authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(email, password));
    } catch (Exception e) {
      log.warn("Credenciales inválidas para el usuario: {}", email);
      throw new RuntimeException("Credenciales inválidas");
    }
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Credenciales inválidas"));
    String token = jwtService.generateToken(user);
    log.info("Login exitoso — usuario: {}", email);
    return Map.of("token", token);
  }

  public Map<String, String> register(String email, String password) {
    if (userRepository.findByEmail(email).isPresent()) {
      log.warn("Intento de registro con email ya existente: {}", email);
      throw new IllegalArgumentException();
    }
    User user = new User(email, passwordEncoder.encode(password), "ADMIN");
    userRepository.save(user);
    log.info("Nuevo usuario registrado — email: {}", email);
    return Map.of("message", "Usuario registrado exitosamente");
  }
}
