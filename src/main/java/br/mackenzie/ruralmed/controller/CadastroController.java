package br.mackenzie.ruralmed.controller;

import br.mackenzie.ruralmed.model.*;
import br.mackenzie.ruralmed.regras.RegraNegocioException;
import br.mackenzie.ruralmed.repository.*;
import br.mackenzie.ruralmed.service.AuthService;
import br.mackenzie.ruralmed.service.UsuarioService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CadastroController {

    private final AuthService authService;
    private final UsuarioService usuarioService;
    private final EspecialidadeRepository especialidadeRepository;
    private final ClinicaParceiraRepository clinicaRepository;
    private final MicroareaRepository microareaRepository;
    private final MunicipioRepository municipioRepository;
    private final CidRepository cidRepository;

    public CadastroController(AuthService authService, UsuarioService usuarioService,
                              EspecialidadeRepository especialidadeRepository, ClinicaParceiraRepository clinicaRepository,
                              MicroareaRepository microareaRepository, MunicipioRepository municipioRepository,
                              CidRepository cidRepository) {
        this.authService = authService;
        this.usuarioService = usuarioService;
        this.especialidadeRepository = especialidadeRepository;
        this.clinicaRepository = clinicaRepository;
        this.microareaRepository = microareaRepository;
        this.municipioRepository = municipioRepository;
        this.cidRepository = cidRepository;
    }

    @GetMapping("/usuarios")
    public List<Usuario> listarUsuarios(@RequestHeader(value = "Authorization", required = false) String token) {
        authService.exigir(token, Perfil.UBS);
        return usuarioService.listar();
    }

    @PostMapping("/usuarios")
    public Usuario criarUsuario(@RequestBody UsuarioService.UsuarioForm form,
                                @RequestHeader(value = "Authorization", required = false) String token) {
        authService.exigir(token, Perfil.UBS);
        return usuarioService.criar(form);
    }

    @DeleteMapping("/usuarios/{id}")
    public void inativarUsuario(@PathVariable Long id,
                                @RequestHeader(value = "Authorization", required = false) String token) {
        authService.exigir(token, Perfil.UBS);
        usuarioService.inativar(id);
    }

    @GetMapping("/medicos")
    public List<Medico> medicos(@RequestParam Long especialidadeId,
                                @RequestHeader(value = "Authorization", required = false) String token) {
        authService.exigir(token, Perfil.UBS);
        return usuarioService.medicosDisponiveis(especialidadeId);
    }

    @GetMapping("/especialidades")
    public List<Especialidade> listarEspecialidades(@RequestHeader(value = "Authorization", required = false) String token) {
        authService.exigir(token);
        return especialidadeRepository.findAllByOrderByNome();
    }

    @PostMapping("/especialidades")
    public Especialidade criarEspecialidade(@Valid @RequestBody Especialidade especialidade,
                                            @RequestHeader(value = "Authorization", required = false) String token) {
        authService.exigir(token, Perfil.UBS);
        return especialidadeRepository.save(especialidade);
    }

    @PutMapping("/especialidades/{id}")
    public Especialidade alterarEspecialidade(@PathVariable Long id, @Valid @RequestBody Especialidade dados,
                                              @RequestHeader(value = "Authorization", required = false) String token) {
        authService.exigir(token, Perfil.UBS);
        Especialidade especialidade = especialidadeRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Especialidade não encontrada"));
        especialidade.setNome(dados.getNome());
        return especialidadeRepository.save(especialidade);
    }

    @DeleteMapping("/especialidades/{id}")
    public void excluirEspecialidade(@PathVariable Long id,
                                     @RequestHeader(value = "Authorization", required = false) String token) {
        authService.exigir(token, Perfil.UBS);
        especialidadeRepository.deleteById(id);
    }

    @GetMapping("/clinicas")
    public List<ClinicaParceira> listarClinicas(@RequestHeader(value = "Authorization", required = false) String token) {
        authService.exigir(token, Perfil.UBS);
        return clinicaRepository.findAllByOrderByNome();
    }

    @PostMapping("/clinicas")
    public ClinicaParceira criarClinica(@Valid @RequestBody ClinicaParceira clinica,
                                        @RequestHeader(value = "Authorization", required = false) String token) {
        authService.exigir(token, Perfil.UBS);
        clinica.setMunicipio(municipioRepository.findById(clinica.getMunicipio().getId())
                .orElseThrow(() -> new RegraNegocioException("Município não encontrado")));
        return clinicaRepository.save(clinica);
    }

    @PutMapping("/clinicas/{id}")
    public ClinicaParceira alterarClinica(@PathVariable Long id, @Valid @RequestBody ClinicaParceira dados,
                                          @RequestHeader(value = "Authorization", required = false) String token) {
        authService.exigir(token, Perfil.UBS);
        ClinicaParceira clinica = clinicaRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Clínica não encontrada"));
        clinica.setNome(dados.getNome());
        clinica.setCnpj(dados.getCnpj());
        clinica.setAtivo(dados.getAtivo());
        clinica.setMunicipio(municipioRepository.findById(dados.getMunicipio().getId())
                .orElseThrow(() -> new RegraNegocioException("Município não encontrado")));
        return clinicaRepository.save(clinica);
    }

    @DeleteMapping("/clinicas/{id}")
    public void inativarClinica(@PathVariable Long id,
                                @RequestHeader(value = "Authorization", required = false) String token) {
        authService.exigir(token, Perfil.UBS);
        ClinicaParceira clinica = clinicaRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Clínica não encontrada"));
        clinica.setAtivo(false);
        clinicaRepository.save(clinica);
    }

    @GetMapping("/microareas")
    public List<Microarea> microareas(@RequestHeader(value = "Authorization", required = false) String token) {
        authService.exigir(token);
        return microareaRepository.findAllByOrderByNome();
    }

    @GetMapping("/municipios")
    public List<Municipio> municipios(@RequestHeader(value = "Authorization", required = false) String token) {
        authService.exigir(token);
        return municipioRepository.findAll();
    }

    @GetMapping("/cids")
    public List<Cid> buscarCid(@RequestParam String busca,
                               @RequestHeader(value = "Authorization", required = false) String token) {
        authService.exigir(token, Perfil.MEDICO);
        return cidRepository.findTop20ByCodigoContainingIgnoreCaseOrDescricaoContainingIgnoreCase(busca, busca);
    }
}
