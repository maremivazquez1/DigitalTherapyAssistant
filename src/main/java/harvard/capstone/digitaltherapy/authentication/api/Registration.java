package harvard.capstone.digitaltherapy.authentication.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class Registration {

    @GetMapping("/register")
    public String sayHello() {
        return "Registration implementation coming soon!";
    }
}
