package br.mackenzie.ruralmed.service;

import br.mackenzie.ruralmed.model.*;
import br.mackenzie.ruralmed.regras.RegraNegocioException;
import br.mackenzie.ruralmed.regras.RegrasDeNegocio;
import br.mackenzie.ruralmed.regras.ValidadoresDocumentos;
import br.mackenzie.ruralmed.repository.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private static final Set<String> UFS = Set.of("AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA",
            "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO");

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
        if (form == null) throw new RegraNegocioException("Informe os dados do usuário");
        String nome = form.nome() == null ? "" : form.nome().trim();
        if (nome.length() < 2 || nome.length() > 120) {
            throw new RegraNegocioException("O nome deve ter entre 2 e 120 caracteres");
        }
        String email = form.email() == null ? "" : form.email().trim().toLowerCase(Locale.ROOT);
        if (email.length() > 254 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new RegraNegocioException("Informe um e-mail válido");
        }
        String cpf = form.cpf() == null ? "" : form.cpf().replaceAll("[.\\-\\s]", "");
        if (!ValidadoresDocumentos.cpfValido(cpf)) {
            throw new RegraNegocioException("Informe um CPF válido");
        }
        if (usuarioRepository.findByCpf(cpf).isPresent()) {
            throw new RegraNegocioException("Este CPF já está vinculado a outro usuário");
        }
        if (form.perfil() == null) throw new RegraNegocioException("Escolha o perfil do usuário");
        if (form.senha() == null || form.senha().length() < 6) {
            throw new RegraNegocioException("A senha precisa ter pelo menos 6 caracteres");
        }
        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw new RegraNegocioException("Este e-mail já está cadastrado");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setCpf(cpf);
        usuario.setPerfil(form.perfil());
        usuario.setSenhaHash(authService.gerarHash(form.senha()));
        usuarioRepository.save(usuario);

        if (form.perfil() == Perfil.ACS) {
            if (form.microareaId() == null) throw new RegraNegocioException("Escolha a microárea do ACS");
            Acs acs = new Acs();
            acs.setUsuario(usuario);
            acs.setMicroarea(microareaRepository.findById(form.microareaId())
                    .orElseThrow(() -> new RegraNegocioException("Escolha a microárea do ACS")));
            acsRepository.save(acs);
        }

        if (form.perfil() == Perfil.MEDICO) {
            String ufCrm = form.ufCrm() == null ? "" : form.ufCrm().trim().toUpperCase(Locale.ROOT);
            if (form.crm() == null || !form.crm().trim().matches("\\d{1,10}") || !UFS.contains(ufCrm)) {
                throw new RegraNegocioException("Informe um CRM e uma UF válidos");
            }
            if (form.especialidadeId() == null) {
                throw new RegraNegocioException("Escolha a especialidade do médico");
            }
            if (form.valorPorLaudo() != null && form.valorPorLaudo().signum() < 0) {
                throw new RegraNegocioException("O valor por laudo não pode ser negativo");
            }
            Medico medico = new Medico();
            medico.setUsuario(usuario);
            medico.setEspecialidade(especialidadeRepository.findById(form.especialidadeId())
                    .orElseThrow(() -> new RegraNegocioException("Escolha a especialidade do médico")));
            if (form.clinicaId() != null) {
                medico.setClinica(clinicaRepository.findById(form.clinicaId()).orElse(null));
            }
            medico.setCrm(form.crm().trim());
            medico.setUfCrm(ufCrm);
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
