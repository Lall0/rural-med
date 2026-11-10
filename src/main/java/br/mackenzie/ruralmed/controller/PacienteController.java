package br.mackenzie.ruralmed.controller;

import br.mackenzie.ruralmed.model.Dossie;
import br.mackenzie.ruralmed.model.Paciente;
import br.mackenzie.ruralmed.model.Perfil;
import br.mackenzie.ruralmed.service.AuthService;
import br.mackenzie.ruralmed.service.PacienteService;
import br.mackenzie.ruralmed.service.TriagemService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pacientes")
public class PacienteController {

    private final AuthService authService;
    private final PacienteService pacienteService;
    private final TriagemService triagemService;

    public PacienteController(AuthService authService, PacienteService pacienteService, TriagemService triagemService) {
        this.authService = authService;
        this.pacienteService = pacienteService;
        this.triagemService = triagemService;
    }

    @GetMapping
    public List<Paciente> listar(@RequestHeader(value = "Authorization", required = false) String token) {
        AuthService.Sessao sessao = authService.exigir(token, Perfil.ACS, Perfil.UBS);
        return pacienteService.listar(sessao);
    }

    @GetMapping("/{id}")
    public Paciente buscar(@PathVariable Long id,
                           @RequestHeader(value = "Authorization", required = false) String token) {
        AuthService.Sessao sessao = authService.exigir(token, Perfil.ACS, Perfil.UBS);
        return pacienteService.buscar(id, sessao);
    }

    @PostMapping
    public Paciente criar(@Valid @RequestBody Paciente paciente,
                          @RequestHeader(value = "Authorization", required = false) String token) {
        AuthService.Sessao sessao = authService.exigir(token, Perfil.ACS, Perfil.UBS);
        return pacienteService.criar(paciente, sessao);
    }

    @PutMapping("/{id}")
    public Paciente atualizar(@PathVariable Long id, @Valid @RequestBody Paciente paciente,
                              @RequestHeader(value = "Authorization", required = false) String token) {
        AuthService.Sessao sessao = authService.exigir(token, Perfil.ACS, Perfil.UBS);
        return pacienteService.atualizar(id, paciente, sessao);
    }

    @DeleteMapping("/{id}")
    public void inativar(@PathVariable Long id,
                         @RequestHeader(value = "Authorization", required = false) String token) {
        AuthService.Sessao sessao = authService.exigir(token, Perfil.ACS, Perfil.UBS);
        pacienteService.inativar(id, sessao);
    }

    @GetMapping("/{id}/historico")
    public List<Dossie> historico(@PathVariable Long id,
                                  @RequestHeader(value = "Authorization", required = false) String token) {
        AuthService.Sessao sessao = authService.exigir(token, Perfil.ACS, Perfil.UBS, Perfil.MEDICO);
        if (sessao.usuario().getPerfil() != Perfil.MEDICO) {
            pacienteService.buscar(id, sessao); // aplica a RN02 para o ACS
        }
        return triagemService.historico(id);
    }
}
