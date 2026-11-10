package br.mackenzie.ruralmed.service;

import br.mackenzie.ruralmed.model.*;
import br.mackenzie.ruralmed.regras.RegraNegocioException;
import br.mackenzie.ruralmed.regras.RegrasDeNegocio;
import br.mackenzie.ruralmed.repository.*;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    public record UsuarioForm(String nome, String email, String cpf, String senha, Perfil perfil,
                              Long microareaId,
                              Long especialidadeId, Long clinicaId, String crm, String ufCrm,
                              Boolean crmAtivo, BigDecimal valorPorLaudo) {
    }

    private final UsuarioRepository usuarioRepository;
    private final AcsRepository acsRepository;
    private final MedicoRepository medicoRepository;
    private final MicroareaRepository microareaRepository;
    private final EspecialidadeRepository especialidadeRepository;
    private final ClinicaParceiraRepository clinicaRepository;
    private final AuthService authService;

    public UsuarioService(UsuarioRepository usuarioRepository, AcsRepository acsRepository,
                          MedicoRepository medicoRepository, MicroareaRepository microareaRepository,
                          EspecialidadeRepository especialidadeRepository,
                          ClinicaParceiraRepository clinicaRepository, AuthService authService) {
        this.usuarioRepository = usuarioRepository;
        this.acsRepository = acsRepository;
        this.medicoRepository = medicoRepository;
        this.microareaRepository = microareaRepository;
        this.especialidadeRepository = especialidadeRepository;
        this.clinicaRepository = clinicaRepository;
        this.authService = authService;
    }

    public List<Usuario> listar() {
        return usuarioRepository.findAllByOrderByNome();
    }

    @Transactional
    public Usuario criar(UsuarioForm form) {
        if (form.senha() == null || form.senha().length() < 6) {
            throw new RegraNegocioException("A senha precisa ter pelo menos 6 caracteres");
        }
        if (usuarioRepository.findByEmail(form.email().toLowerCase()).isPresent()) {
            throw new RegraNegocioException("Este e-mail já está cadastrado");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(form.nome());
        usuario.setEmail(form.email().toLowerCase());
        usuario.setCpf(form.cpf());
        usuario.setPerfil(form.perfil());
        usuario.setSenhaHash(authService.gerarHash(form.senha()));
        usuarioRepository.save(usuario);

        if (form.perfil() == Perfil.ACS) {
            Acs acs = new Acs();
            acs.setUsuario(usuario);
            acs.setMicroarea(microareaRepository.findById(form.microareaId())
                    .orElseThrow(() -> new RegraNegocioException("Escolha a microárea do ACS")));
            acsRepository.save(acs);
        }

        if (form.perfil() == Perfil.MEDICO) {
            Medico medico = new Medico();
            medico.setUsuario(usuario);
            medico.setEspecialidade(especialidadeRepository.findById(form.especialidadeId())
                    .orElseThrow(() -> new RegraNegocioException("Escolha a especialidade do médico")));
            if (form.clinicaId() != null) {
                medico.setClinica(clinicaRepository.findById(form.clinicaId()).orElse(null));
            }
            medico.setCrm(form.crm());
            medico.setUfCrm(form.ufCrm());
            medico.setCrmAtivo(Boolean.TRUE.equals(form.crmAtivo()));
            medico.setValorPorLaudo(form.valorPorLaudo() == null ? BigDecimal.ZERO : form.valorPorLaudo());

            RegrasDeNegocio.verificarCrmAtivo(medico); // RN04: só cadastra médico com CRM ativo
            medicoRepository.save(medico);
        }
        return usuario;
    }

    /** DELETE lógico: o usuário é inativado e não consegue mais entrar. */
    public void inativar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Usuário não encontrado"));
        usuario.setAtivo(false);
        usuarioRepository.save(usuario);
    }

    public List<Medico> medicosDisponiveis(Long especialidadeId) {
        return medicoRepository.findByEspecialidadeIdAndCrmAtivoTrue(especialidadeId);
    }
}
