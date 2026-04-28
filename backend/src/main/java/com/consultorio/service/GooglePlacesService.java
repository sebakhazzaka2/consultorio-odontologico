package com.consultorio.service;

import com.consultorio.dto.ReviewResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GooglePlacesService {

  private static final Logger log = LoggerFactory.getLogger(GooglePlacesService.class);
  private static final long CACHE_TTL_MS = 24 * 60 * 60 * 1000L; // 24 horas
  private static final String PLACES_URL =
      "https://places.googleapis.com/v1/places/{placeId}?languageCode=es";

  private final String apiKey;
  private final String placeId;
  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  private volatile List<ReviewResponse> cache = List.of();
  private volatile long cacheTime = 0;

  public GooglePlacesService(
      @Value("${google.places.api-key:}") String apiKey,
      @Value("${clinic.google-place-id:}") String placeId) {
    this.apiKey = apiKey;
    this.placeId = placeId;
    this.restTemplate = new RestTemplate();
    this.objectMapper = new ObjectMapper();
  }

  public List<ReviewResponse> getReviews() {
    if (apiKey.isBlank() || placeId.isBlank()) return List.of();

    long now = System.currentTimeMillis();
    if (now - cacheTime < CACHE_TTL_MS) return cache;

    try {
      HttpHeaders headers = new HttpHeaders();
      headers.set("X-Goog-Api-Key", apiKey);
      headers.set("X-Goog-FieldMask", "reviews");
      HttpEntity<Void> request = new HttpEntity<>(headers);
      ResponseEntity<String> response = restTemplate.exchange(
          PLACES_URL, HttpMethod.GET, request, String.class, placeId);
      List<ReviewResponse> fetched = parse(response.getBody());
      cache = fetched;
      cacheTime = now;
      log.info("Reseñas de Google actualizadas: {} reseñas", fetched.size());
      return cache;
    } catch (Exception e) {
      log.warn("Error al obtener reseñas de Google Places: {}", e.getMessage());
      return cache;
    }
  }

  private List<ReviewResponse> parse(String json) throws Exception {
    JsonNode root = objectMapper.readTree(json);
    JsonNode reviews = root.path("reviews");
    if (!reviews.isArray()) return List.of();

    List<ReviewResponse> result = new ArrayList<>();
    for (JsonNode r : reviews) {
      int rating = r.path("rating").asInt(0);
      if (rating < 4) continue;
      result.add(new ReviewResponse(
          r.path("authorAttribution").path("displayName").asText(""),
          rating,
          r.path("text").path("text").asText(""),
          r.path("relativePublishTimeDescription").asText(""),
          r.path("authorAttribution").path("photoUri").asText("")));
    }
    return result;
  }
}
