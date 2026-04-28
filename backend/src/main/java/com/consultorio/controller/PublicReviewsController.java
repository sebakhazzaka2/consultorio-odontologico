package com.consultorio.controller;

import com.consultorio.dto.ReviewResponse;
import com.consultorio.service.GooglePlacesService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public")
public class PublicReviewsController {

  private final GooglePlacesService googlePlacesService;

  public PublicReviewsController(GooglePlacesService googlePlacesService) {
    this.googlePlacesService = googlePlacesService;
  }

  @GetMapping("/reviews")
  public ResponseEntity<List<ReviewResponse>> getReviews() {
    return ResponseEntity.ok(googlePlacesService.getReviews());
  }
}
