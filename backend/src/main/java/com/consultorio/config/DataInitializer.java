package com.consultorio.config;

import com.consultorio.model.User;
import com.consultorio.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final String adminEmail;
  private final String adminPassword;

  public DataInitializer(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      @Value("${app.admin.email}") String adminEmail,
      @Value("${app.admin.password}") String adminPassword) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.adminEmail = adminEmail;
    this.adminPassword = adminPassword;
  }

  @Override
  public void run(ApplicationArguments args) {
    User admin = userRepository.findByEmail(adminEmail)
        .orElseGet(() -> new User(adminEmail, null, "ADMIN"));
    admin.setPassword(passwordEncoder.encode(adminPassword));
    userRepository.save(admin);
    log.info("Admin user synced: {}", adminEmail);
  }
}
