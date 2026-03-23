package com.consultorio.controller;

import com.consultorio.service.AuthService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
}
