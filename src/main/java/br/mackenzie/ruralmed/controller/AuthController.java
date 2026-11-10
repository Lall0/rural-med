package br.mackenzie.ruralmed.controller;

import br.mackenzie.ruralmed.service.AuthService;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {

    public record LoginForm(String login, String senha) {
    }

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthService.Sessao login(@RequestBody LoginForm form) {
        return authService.login(form.login(), form.senha());
    }

    @PostMapping("/sair")
    public void sair(@RequestHeader(value = "Authorization", required = false) String token) {
        authService.sair(token);
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
