package br.mackenzie.ruralmed.controller;

import br.mackenzie.ruralmed.model.Perfil;
import br.mackenzie.ruralmed.service.AuthService;
import br.mackenzie.ruralmed.service.PrescricaoService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class PrescricaoController {

    private final AuthService authService;
    private final PrescricaoService prescricaoService;

    public PrescricaoController(AuthService authService, PrescricaoService prescricaoService) {
        this.authService = authService;
        this.prescricaoService = prescricaoService;
    }

    @GetMapping("/medicamentos")
    public List<PrescricaoService.MedicamentoReferencia> buscarMedicamentos(@RequestParam String busca,
            @RequestHeader(value = "Authorization", required = false) String token) {
        authService.exigir(token, Perfil.MEDICO);
        return prescricaoService.buscarMedicamentos(busca);
    }

    @GetMapping("/dossies/{id}/receitas")
    public List<PrescricaoService.ReceitaResumo> listar(@PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String token) {
        AuthService.Sessao sessao = authService.exigir(token, Perfil.ACS, Perfil.UBS, Perfil.MEDICO);
        return prescricaoService.listar(id, sessao);
    }

    @PostMapping("/dossies/{id}/receitas")
    public PrescricaoService.ReceitaResumo prescrever(@PathVariable Long id,
            @RequestBody PrescricaoService.PrescricaoForm form,
            @RequestHeader(value = "Authorization", required = false) String token) {
        AuthService.Sessao sessao = authService.exigir(token, Perfil.MEDICO);
        return prescricaoService.prescrever(id, form, sessao);
    }
}
