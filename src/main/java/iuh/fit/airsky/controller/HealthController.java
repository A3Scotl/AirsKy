package iuh.fit.airsky.controller;

@RestController
public class HealthController {
  @GetMapping("/health")
  public String health() {
    return "OK";
  }
}
