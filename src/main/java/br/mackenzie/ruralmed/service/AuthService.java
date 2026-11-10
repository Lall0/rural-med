package br.mackenzie.ruralmed.service;

import br.mackenzie.ruralmed.model.Medico;
import br.mackenzie.ruralmed.model.Perfil;
import br.mackenzie.ruralmed.model.Usuario;
import br.mackenzie.ruralmed.regras.AcessoNegadoException;
import br.mackenzie.ruralmed.regras.RegraNegocioException;
import br.mackenzie.ruralmed.regras.RegrasDeNegocio;
import br.mackenzie.ruralmed.repository.AcsRepository;
import br.mackenzie.ruralmed.repository.MedicoRepository;
import br.mackenzie.ruralmed.repository.UsuarioRepository;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/** Sessoes em memoria: reiniciar o servidor invalida os tokens. */
@Service
public class AuthService {

    public record Sessao(String token, Usuario usuario, Long microareaId, Long especialidadeId) {
    }

    private final Map<String, Sessao> sessoes = new ConcurrentHashMap<>();
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    private final UsuarioRepository usuarioRepository;
    private final AcsRepository acsRepository;
    private final MedicoRepository medicoRepository;

    public AuthService(UsuarioRepository usuarioRepository, AcsRepository acsRepository,
                       MedicoRepository medicoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.acsRepository = acsRepository;
        this.medicoRepository = medicoRepository;
    }

    public Sessao login(String login, String senha) {
        if (login == null || login.isBlank() || senha == null || senha.isBlank()) {
            throw new RegraNegocioException("E-mail/CPF ou senha inválidos");
        }
        Usuario usuario = (login.contains("@")
                ? usuarioRepository.findByEmail(login.trim().toLowerCase())
                : usuarioRepository.findByCpf(login.replaceAll("\\D", "")))
                .orElseThrow(() -> new RegraNegocioException("E-mail/CPF ou senha inválidos"));

        if (!bcrypt.matches(senha, usuario.getSenhaHash())) {
            throw new RegraNegocioException("E-mail/CPF ou senha inválidos");
        }
        if (!usuario.getAtivo()) {
            throw new RegraNegocioException("Usuário inativo");
        }

        Long microareaId = null;
        Long especialidadeId = null;
        if (usuario.getPerfil() == Perfil.ACS) {
            microareaId = acsRepository.findById(usuario.getId()).orElseThrow().getMicroarea().getId();
        }
        if (usuario.getPerfil() == Perfil.MEDICO) {
            Medico medico = medicoRepository.findById(usuario.getId()).orElseThrow();
            RegrasDeNegocio.verificarCrmAtivo(medico); // RN04: sem CRM ativo, não entra
            especialidadeId = medico.getEspecialidade().getId();
        }

        String token = UUID.randomUUID().toString();
        Sessao sessao = new Sessao(token, usuario, microareaId, especialidadeId);
        sessoes.put(token, sessao);
        return sessao;
    }

    public void sair(String token) {
        sessoes.remove(token);
    }

    /** Sem perfis informados, exige apenas uma sessao valida. */
    public Sessao exigir(String token, Perfil... perfisPermitidos) {
        Sessao sessao = token == null ? null : sessoes.get(token);
        if (sessao == null) {
            throw new AcessoNegadoException("Faça login para continuar");
        }
        if (perfisPermitidos.length > 0 && !Arrays.asList(perfisPermitidos).contains(sessao.usuario().getPerfil())) {
            throw new AcessoNegadoException("Seu perfil não tem acesso a esta funcionalidade");
        }
        return sessao;
    }

    public String gerarHash(String senha) {
        return bcrypt.encode(senha);
    }
}
