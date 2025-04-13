package harvard.capstone.digitaltherapy.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {

  @GetMapping("/")
  public String home() {
    return "Spring Boot is running!";

  }
}