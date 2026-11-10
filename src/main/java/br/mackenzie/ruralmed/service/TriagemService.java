package br.mackenzie.ruralmed.service;

import br.mackenzie.ruralmed.model.*;
import br.mackenzie.ruralmed.regras.DadosTriagem;
import br.mackenzie.ruralmed.regras.RegraNegocioException;
import br.mackenzie.ruralmed.regras.RegrasDeNegocio;
import br.mackenzie.ruralmed.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Fluxo: sincronizacao do ACS -> encaminhamento pela UBS -> fila do medico. */
@Service
public class TriagemService {

    public record TriagemRecebida(UUID idLocal, Long pacienteId, OffsetDateTime coletadoEm,
                                  DadosTriagem dados, String relato) {
    }

    public record Resultado(UUID idLocal, String situacao, String mensagem) {
    }

    private final DossieRepository dossieRepository;
    private final PacienteRepository pacienteRepository;
    private final AcsRepository acsRepository;
    private final EspecialidadeRepository especialidadeRepository;
    private final MedicoRepository medicoRepository;
    private final ObjectMapper json;

    public TriagemService(DossieRepository dossieRepository, PacienteRepository pacienteRepository,
                          AcsRepository acsRepository, EspecialidadeRepository especialidadeRepository,
                          MedicoRepository medicoRepository, ObjectMapper json) {
        this.dossieRepository = dossieRepository;
        this.pacienteRepository = pacienteRepository;
        this.acsRepository = acsRepository;
        this.especialidadeRepository = especialidadeRepository;
        this.medicoRepository = medicoRepository;
        this.json = json;
    }

    /** Uma rejeicao de negocio nao interrompe as demais triagens do lote. */
    public List<Resultado> sincronizar(List<TriagemRecebida> triagens, AuthService.Sessao sessao) {
        Acs acs = acsRepository.findById(sessao.usuario().getId()).orElseThrow();
        List<Resultado> resultados = new ArrayList<>();
        for (TriagemRecebida t : triagens) {
            try {
                resultados.add(salvarTriagem(t, acs));
            } catch (RegraNegocioException e) {
                resultados.add(new Resultado(t.idLocal(), "REJEITADO", e.getMessage()));
            }
        }
        return resultados;
    }

    private Resultado salvarTriagem(TriagemRecebida t, Acs acs) {
        // RNF02: se o celular reenviar a mesma triagem (queda de internet), não duplica
        if (dossieRepository.findByIdLocal(t.idLocal()).isPresent()) {
            return new Resultado(t.idLocal(), "JA_EXISTIA", "Triagem já recebida antes");
        }

        Paciente paciente = pacienteRepository.findById(t.pacienteId())
                .orElseThrow(() -> new RegraNegocioException("Paciente não encontrado"));

        RegrasDeNegocio.verificarMicroarea(acs.getMicroarea().getId(), paciente);
        RegrasDeNegocio.verificarConsentimento(paciente);
        RegrasDeNegocio.validarTriagem(t.dados());
        RegrasDeNegocio.Escore escore =
                RegrasDeNegocio.calcularEscore(t.dados(), paciente.getIdade(), t.relato());

        Dossie dossie = new Dossie();
        dossie.setIdLocal(t.idLocal());
        dossie.setPaciente(paciente);
        dossie.setAcs(acs);
        dossie.setDadosTriagem(paraJson(t.dados()));
        dossie.setRelato(t.relato());
        dossie.setEscore(BigDecimal.valueOf(escore.pontos()));
        dossie.setPrioritario(escore.prioritario());
        dossie.setFatoresEscore(paraJson(escore.fatores()));
        dossie.setColetadoEm(t.coletadoEm());
        dossieRepository.save(dossie);

        return new Resultado(t.idLocal(), "CRIADO", "Escore " + escore.pontos()
                + (escore.prioritario() ? " - caso prioritário" : ""));
    }

    public List<Dossie> listar(AuthService.Sessao sessao, StatusDossie status) {
        if (sessao.usuario().getPerfil() == Perfil.ACS) {
            return dossieRepository.findByAcsUsuarioIdOrderByColetadoEmDesc(sessao.usuario().getId());
        }
        if (status != null) {
            return dossieRepository.findByStatusOrderByEscoreDesc(status);
        }
        return dossieRepository.findAllByOrderByColetadoEmDesc();
    }

    public List<Dossie> fila(AuthService.Sessao sessao) {
        return dossieRepository.findByStatusAndEspecialidadeIdOrderByEscoreDescEncaminhadoEmAsc(
                StatusDossie.ENCAMINHADO, sessao.especialidadeId());
    }

    public List<Dossie> historico(Long pacienteId) {
        return dossieRepository.findByPacienteIdOrderByColetadoEmAsc(pacienteId);
    }

    /** RF02: a UBS escolhe a especialidade. Sem médico disponível, o caso fica aguardando. */
    public Dossie encaminhar(Long dossieId, Long especialidadeId) {
        Dossie dossie = dossieRepository.findById(dossieId)
                .orElseThrow(() -> new RegraNegocioException("Dossiê não encontrado"));
        if (dossie.getStatus() == StatusDossie.LAUDO_EMITIDO) {
            throw new RegraNegocioException("Este caso já tem laudo");
        }
        Especialidade especialidade = especialidadeRepository.findById(especialidadeId)
                .orElseThrow(() -> new RegraNegocioException("Especialidade não encontrada"));

        dossie.setEspecialidade(especialidade);
        boolean temMedico = !medicoRepository.findByEspecialidadeIdAndCrmAtivoTrue(especialidadeId).isEmpty();
        if (temMedico) {
            dossie.setStatus(StatusDossie.ENCAMINHADO);
            dossie.setEncaminhadoEm(OffsetDateTime.now()); // começa a contar o prazo da RN06
        } else {
            dossie.setStatus(StatusDossie.AGUARDANDO_ENCAMINHAMENTO);
        }
        return dossieRepository.save(dossie);
    }

    private String paraJson(Object objeto) {
        try {
            return json.writeValueAsString(objeto);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
