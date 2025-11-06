package iuh.fit.airsky.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {
  @GetMapping
  public String health() {
    return "OK";
  }
}
