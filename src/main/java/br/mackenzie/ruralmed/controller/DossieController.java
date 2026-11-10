package br.mackenzie.ruralmed.controller;

import br.mackenzie.ruralmed.model.Dossie;
import br.mackenzie.ruralmed.model.Laudo;
import br.mackenzie.ruralmed.model.Perfil;
import br.mackenzie.ruralmed.model.StatusDossie;
import br.mackenzie.ruralmed.service.AuthService;
import br.mackenzie.ruralmed.service.LaudoService;
import br.mackenzie.ruralmed.service.TriagemService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class DossieController {

    public record EncaminharForm(Long especialidadeId) {
    }

    private final AuthService authService;
    private final TriagemService triagemService;
    private final LaudoService laudoService;

    public DossieController(AuthService authService, TriagemService triagemService, LaudoService laudoService) {
        this.authService = authService;
        this.triagemService = triagemService;
        this.laudoService = laudoService;
    }

    @PostMapping("/triagens/sincronizar")
    public List<TriagemService.Resultado> sincronizar(@RequestBody List<TriagemService.TriagemRecebida> triagens,
                                                      @RequestHeader(value = "Authorization", required = false) String token) {
        AuthService.Sessao sessao = authService.exigir(token, Perfil.ACS);
        return triagemService.sincronizar(triagens, sessao);
    }

    @GetMapping("/dossies")
    public List<Dossie> listar(@RequestParam(required = false) StatusDossie status,
                               @RequestHeader(value = "Authorization", required = false) String token) {
        AuthService.Sessao sessao = authService.exigir(token, Perfil.ACS, Perfil.UBS);
        return triagemService.listar(sessao, status);
    }

    @PostMapping("/dossies/{id}/encaminhar")
    public Dossie encaminhar(@PathVariable Long id, @RequestBody EncaminharForm form,
                             @RequestHeader(value = "Authorization", required = false) String token) {
        authService.exigir(token, Perfil.UBS);
        return triagemService.encaminhar(id, form.especialidadeId());
    }

    @GetMapping("/fila")
    public List<Dossie> fila(@RequestHeader(value = "Authorization", required = false) String token) {
        AuthService.Sessao sessao = authService.exigir(token, Perfil.MEDICO);
        return triagemService.fila(sessao);
    }

    @PostMapping("/dossies/{id}/laudo")
    public Laudo emitirLaudo(@PathVariable Long id, @RequestBody LaudoService.LaudoForm form,
                             @RequestHeader(value = "Authorization", required = false) String token) {
        AuthService.Sessao sessao = authService.exigir(token, Perfil.MEDICO);
        return laudoService.emitir(id, form, sessao);
    }

    @GetMapping("/laudos")
    public List<Laudo> meusLaudos(@RequestHeader(value = "Authorization", required = false) String token) {
        AuthService.Sessao sessao = authService.exigir(token, Perfil.MEDICO);
        return laudoService.meusLaudos(sessao);
    }
}
