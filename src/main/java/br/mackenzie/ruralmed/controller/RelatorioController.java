package br.mackenzie.ruralmed.controller;

import br.mackenzie.ruralmed.model.Perfil;
import br.mackenzie.ruralmed.service.AuthService;
import br.mackenzie.ruralmed.service.RelatorioService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/relatorios")
public class RelatorioController {

    private final AuthService authService;
    private final RelatorioService relatorioService;

    public RelatorioController(AuthService authService, RelatorioService relatorioService) {
        this.authService = authService;
        this.relatorioService = relatorioService;
    }

    @GetMapping("/atendimentos")
    public RelatorioService.Relatorio atendimentos(@RequestParam LocalDate inicio, @RequestParam LocalDate fim,
                                                   @RequestHeader(value = "Authorization", required = false) String token) {
        authService.exigir(token, Perfil.GESTOR, Perfil.UBS);
        return relatorioService.atendimentos(inicio, fim);
    }

    @GetMapping("/repasses")
    public List<RelatorioService.LinhaExtrato> repasses(@RequestParam int ano, @RequestParam int mes,
                                                        @RequestHeader(value = "Authorization", required = false) String token) {
        authService.exigir(token, Perfil.GESTOR, Perfil.UBS);
        return relatorioService.repasses(ano, mes);
    }
}
