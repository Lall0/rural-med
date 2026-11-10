package br.mackenzie.ruralmed.service;

import br.mackenzie.ruralmed.model.Paciente;
import br.mackenzie.ruralmed.model.Perfil;
import br.mackenzie.ruralmed.regras.RegraNegocioException;
import br.mackenzie.ruralmed.regras.RegrasDeNegocio;
import br.mackenzie.ruralmed.repository.MicroareaRepository;
import br.mackenzie.ruralmed.repository.PacienteRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PacienteService {

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
        if (vazio(dados.getCpf()) && vazio(dados.getCns())) {
            throw new RegraNegocioException("Informe o CPF ou o Cartão Nacional de Saúde (CNS)");
        }
        if (sessao.usuario().getPerfil() != Perfil.ACS && dados.getMicroarea() == null) {
            throw new RegraNegocioException("Escolha a microárea do paciente");
        }
        Long microareaId = sessao.usuario().getPerfil() == Perfil.ACS
                ? sessao.microareaId()
                : dados.getMicroarea().getId();
        paciente.setMicroarea(microareaRepository.findById(microareaId)
                .orElseThrow(() -> new RegraNegocioException("Microárea não encontrada")));

        paciente.setNome(dados.getNome());
        paciente.setCpf(vazio(dados.getCpf()) ? null : dados.getCpf());
        paciente.setCns(vazio(dados.getCns()) ? null : dados.getCns());
        paciente.setDataNascimento(dados.getDataNascimento());
        paciente.setTelefone(dados.getTelefone());
        paciente.setConsentimentoEm(dados.getConsentimentoEm());
        if (dados.getAtivo() != null) paciente.setAtivo(dados.getAtivo());
    }

    private static boolean vazio(String texto) {
        return texto == null || texto.isBlank();
    }
}
