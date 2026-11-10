package br.mackenzie.ruralmed.service;

import br.mackenzie.ruralmed.model.Paciente;
import br.mackenzie.ruralmed.model.Perfil;
import br.mackenzie.ruralmed.regras.RegraNegocioException;
import br.mackenzie.ruralmed.regras.RegrasDeNegocio;
import br.mackenzie.ruralmed.regras.ValidadoresDocumentos;
import br.mackenzie.ruralmed.repository.MicroareaRepository;
import br.mackenzie.ruralmed.repository.PacienteRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PacienteService {

    public record PacienteResumo(Long id, String identificadorUnico, String nome, int idade) {
    }

    private final PacienteRepository pacienteRepository;
    private final MicroareaRepository microareaRepository;

    public PacienteService(PacienteRepository pacienteRepository, MicroareaRepository microareaRepository) {
        this.pacienteRepository = pacienteRepository;
        this.microareaRepository = microareaRepository;
    }

    public List<Paciente> listar(AuthService.Sessao sessao) {
        if (sessao.usuario().getPerfil() == Perfil.ACS) {
            return pacienteRepository.findByMicroareaIdAndAtivoTrueOrderByNome(sessao.microareaId());
        }
        return pacienteRepository.findAllByOrderByNome();
    }

    public Paciente buscar(Long id, AuthService.Sessao sessao) {
        Paciente paciente = pacienteRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Paciente não encontrado"));
        if (sessao.usuario().getPerfil() == Perfil.ACS) {
            RegrasDeNegocio.verificarMicroarea(sessao.microareaId(), paciente); // RN02
        }
        return paciente;
    }

    public PacienteResumo localizarParaMedico(String identificador) {
        String termo = identificador == null ? "" : identificador.trim();
        if (termo.isBlank()) {
            throw new RegraNegocioException("Informe o CPF ou o ID RuralMed completo");
        }

        Paciente paciente;
        if (termo.matches("[\\d.\\-\\s]+")) {
            String cpf = termo.replaceAll("[.\\-\\s]", "");
            if (!ValidadoresDocumentos.cpfValido(cpf)) throw new RegraNegocioException("Informe um CPF válido");
            paciente = pacienteRepository.findByCpf(cpf)
                    .orElseThrow(() -> new RegraNegocioException("Paciente não encontrado"));
        } else {
            String id = normalizarIdentificador(termo);
            paciente = pacienteRepository.findByIdentificadorUnico(id)
                    .orElseThrow(() -> new RegraNegocioException("Paciente não encontrado"));
        }

        return new PacienteResumo(paciente.getId(), paciente.getIdentificadorUnico(),
                paciente.getNome(), paciente.getIdade());
    }

    public Paciente criar(Paciente dados, AuthService.Sessao sessao) {
        Paciente paciente = new Paciente();
        copiar(dados, paciente, sessao);
        return pacienteRepository.save(paciente);
    }

    public Paciente atualizar(Long id, Paciente dados, AuthService.Sessao sessao) {
        Paciente paciente = buscar(id, sessao);
        copiar(dados, paciente, sessao);
        return pacienteRepository.save(paciente);
    }

    /** DELETE lógico: o paciente é inativado, para não apagar o histórico de atendimentos. */
    public void inativar(Long id, AuthService.Sessao sessao) {
        Paciente paciente = buscar(id, sessao);
        paciente.setAtivo(false);
        pacienteRepository.save(paciente);
    }

    private void copiar(Paciente dados, Paciente paciente, AuthService.Sessao sessao) {
        if (dados == null) throw new RegraNegocioException("Informe os dados do paciente");
        String nome = dados.getNome() == null ? "" : dados.getNome().trim();
        if (nome.length() < 2 || nome.length() > 120) {
            throw new RegraNegocioException("O nome deve ter entre 2 e 120 caracteres");
        }
        if (dados.getDataNascimento() == null || dados.getDataNascimento().isAfter(LocalDate.now())) {
            throw new RegraNegocioException("Informe uma data de nascimento válida, que não esteja no futuro");
        }
        String cpf = normalizar(dados.getCpf());
        String cns = normalizar(dados.getCns());
        if (cpf != null && !ValidadoresDocumentos.cpfValido(cpf)) {
            throw new RegraNegocioException("Informe um CPF válido");
        }
        if (cns != null && !cns.matches("\\d{15}")) {
            throw new RegraNegocioException("O CNS deve conter 15 números");
        }
        String telefone = normalizarTelefone(dados.getTelefone());
        if (cpf != null && pacienteRepository.findByCpf(cpf)
                .filter(cadastrado -> !java.util.Objects.equals(cadastrado.getId(), paciente.getId())).isPresent()) {
            throw new RegraNegocioException("Este CPF já está vinculado a outro paciente");
        }
        if (sessao.usuario().getPerfil() != Perfil.ACS && dados.getMicroarea() == null) {
            throw new RegraNegocioException("Escolha a microárea do paciente");
        }
        Long microareaId = sessao.usuario().getPerfil() == Perfil.ACS
                ? sessao.microareaId()
                : dados.getMicroarea().getId();
        paciente.setMicroarea(microareaRepository.findById(microareaId)
                .orElseThrow(() -> new RegraNegocioException("Microárea não encontrada")));

        paciente.setNome(nome);
        paciente.setCpf(cpf);
        paciente.setCns(cns);
        if (paciente.getIdentificadorUnico() == null) {
            paciente.setIdentificadorUnico(UUID.randomUUID().toString());
        }
        paciente.setDataNascimento(dados.getDataNascimento());
        paciente.setTelefone(telefone);
        paciente.setConsentimentoEm(dados.getConsentimentoEm());
        if (dados.getAtivo() != null) paciente.setAtivo(dados.getAtivo());
    }

    private static String normalizar(String texto) {
        if (texto == null || texto.isBlank()) return null;
        String normalizado = texto.replaceAll("[.\\-\\s]", "");
        if (!normalizado.matches("\\d+")) {
            throw new RegraNegocioException("Documento deve conter apenas números");
        }
        return normalizado;
    }

    private static String normalizarTelefone(String texto) {
        if (texto == null || texto.isBlank()) return null;
        String telefone = texto.replaceAll("[\\s().+-]", "");
        if (telefone.startsWith("55") && (telefone.length() == 12 || telefone.length() == 13)) {
            telefone = telefone.substring(2);
        }
        if (!telefone.matches("\\d{10,11}")) {
            throw new RegraNegocioException("O telefone deve conter DDD e 8 ou 9 dígitos");
        }
        return telefone;
    }

    private static String normalizarIdentificador(String texto) {
        String id = texto.replaceFirst("(?i)^RM-", "").replace("-", "").toLowerCase(java.util.Locale.ROOT);
        if (!id.matches("(?i)[0-9a-f]{32}")) {
            throw new RegraNegocioException("Use o CPF completo ou o ID RuralMed completo");
        }
        return id.substring(0, 8) + "-" + id.substring(8, 12) + "-"
                + id.substring(12, 16) + "-" + id.substring(16, 20) + "-" + id.substring(20);
    }

}
