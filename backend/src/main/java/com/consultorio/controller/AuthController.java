package com.consultorio.controller;

import com.consultorio.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest body) {
    return new ResponseEntity<>(
        authService.login(body.getEmail(), body.getPassword()), HttpStatus.OK);
  }

  @PostMapping("/register")
  public ResponseEntity<Map<String, String>> register(@RequestBody LoginRequest body) {
    return new ResponseEntity<>(
        authService.register(body.getEmail(), body.getPassword()), HttpStatus.CREATED);
  }

  @PatchMapping("/change-password")
  public ResponseEntity<Map<String, String>> changePassword(
      @AuthenticationPrincipal UserDetails principal,
      @Valid @RequestBody ChangePasswordRequest body) {
    authService.changePassword(principal.getUsername(), body.getCurrentPassword(), body.getNewPassword());
    return ResponseEntity.ok(Map.of("message", "Contraseña actualizada exitosamente"));
  }

  public static class LoginRequest {

    private String email;
    private String password;

    public LoginRequest() {}

    public LoginRequest(String email, String password) {
      this.email = email;
      this.password = password;
    }

    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }
  }

  public static class ChangePasswordRequest {

    @NotBlank(message = "La contraseña actual es requerida")
    private String currentPassword;

    @NotBlank(message = "La nueva contraseña es requerida")
    @Size(min = 8, message = "La nueva contraseña debe tener al menos 8 caracteres")
    private String newPassword;

    public ChangePasswordRequest() {}

    public String getCurrentPassword() {
      return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
      this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
      return newPassword;
    }

    public void setNewPassword(String newPassword) {
      this.newPassword = newPassword;
    }
  }
}
