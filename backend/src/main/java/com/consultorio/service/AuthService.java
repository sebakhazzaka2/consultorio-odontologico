package com.consultorio.service;

import com.consultorio.model.User;
import com.consultorio.repository.UserRepository;
import com.consultorio.security.JwtService;
import java.util.Map;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

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
    try {
      authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(email, password));
    } catch (Exception e) {
      throw new RuntimeException("Credenciales inválidas");
    }
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Credenciales inválidas"));
    String token = jwtService.generateToken(user);
    return Map.of("token", token);
  }

  public Map<String, String> register(String email, String password) {
    if (userRepository.findByEmail(email).isPresent()) {
      throw new IllegalArgumentException();
    }
    User user = new User(email, passwordEncoder.encode(password), "ADMIN");
    userRepository.save(user);
    return Map.of("message", "Usuario registrado exitosamente");
  }
}
