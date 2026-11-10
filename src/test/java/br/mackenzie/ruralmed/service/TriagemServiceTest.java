package br.mackenzie.ruralmed.service;

import br.mackenzie.ruralmed.model.*;
import br.mackenzie.ruralmed.regras.DadosTriagem;
import br.mackenzie.ruralmed.regras.RegraNegocioException;
import br.mackenzie.ruralmed.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Sincronizacao e encaminhamento de triagens")
class TriagemServiceTest {
    private final DossieRepository dossies = mock(DossieRepository.class);
    private final PacienteRepository pacientes = mock(PacienteRepository.class);
    private final AcsRepository agentes = mock(AcsRepository.class);
    private final EspecialidadeRepository especialidades = mock(EspecialidadeRepository.class);
    private final MedicoRepository medicos = mock(MedicoRepository.class);
    private final TriagemService service = new TriagemService(dossies, pacientes, agentes,
            especialidades, medicos, new ObjectMapper());
    private AuthService.Sessao sessao;
    private Paciente paciente;

    @BeforeEach
    void prepararAcsEPaciente() {
        Usuario usuario = new Usuario();
        usuario.setId(3L);
        usuario.setPerfil(Perfil.ACS);
        sessao = new AuthService.Sessao("teste", usuario, 1L, null);
        Microarea area = new Microarea();
        area.setId(1L);
        Acs acs = new Acs();
        acs.setUsuario(usuario);
        acs.setMicroarea(area);
        paciente = new Paciente();
        paciente.setMicroarea(area);
        paciente.setDataNascimento(java.time.LocalDate.of(1990, 1, 1));
        paciente.setConsentimentoEm(OffsetDateTime.parse("2026-01-01T00:00:00Z"));
        when(agentes.findById(3L)).thenReturn(Optional.of(acs));
    }

    private TriagemService.TriagemRecebida recebida(Long pacienteId, DadosTriagem dados) {
        return new TriagemService.TriagemRecebida(UUID.randomUUID(), pacienteId,
                OffsetDateTime.parse("2026-11-03T10:00:00Z"), dados, "");
    }

    private DadosTriagem dadosLeves() {
        return new DadosTriagem("Coceira", 36.5, 120, 80, 80, 98, 1, List.of());
    }

    @Nested
    @DisplayName("Sincronizacao por lote")
    class Sincronizacao {
        @Test
        void deveSalvarTriagemComEscoreEEstadoInicial() {
            when(pacientes.findById(10L)).thenReturn(Optional.of(paciente));
            var recebida = recebida(10L, dadosLeves());
            var captor = org.mockito.ArgumentCaptor.forClass(Dossie.class);

            var resultados = service.sincronizar(List.of(recebida), sessao);

            assertThat(resultados).containsExactly(
                    new TriagemService.Resultado(recebida.idLocal(), "CRIADO", "Escore 0"));
            verify(dossies).save(captor.capture());
            Dossie salvo = captor.getValue();
            assertThat(salvo.getIdLocal()).isEqualTo(recebida.idLocal());
            assertThat(salvo.getEscore()).isEqualByComparingTo("0");
            assertThat(salvo.getStatus()).isEqualTo(StatusDossie.SINCRONIZADO);
            assertThat(salvo.getPrioritario()).isFalse();
        }

        @Test
        void deveReconhecerReenvioSemDuplicar() {
            var recebida = recebida(10L, dadosLeves());
            when(dossies.findByIdLocal(recebida.idLocal())).thenReturn(Optional.of(new Dossie()));

            var resultados = service.sincronizar(List.of(recebida), sessao);

            assertThat(resultados.get(0).situacao()).isEqualTo("JA_EXISTIA");
            verify(dossies, never()).save(any());
            verifyNoInteractions(pacientes);
        }

        @Test
        void deveContinuarLoteAposPacienteInexistente() {
            when(pacientes.findById(10L)).thenReturn(Optional.empty());
            when(pacientes.findById(11L)).thenReturn(Optional.of(paciente));

            var resultados = service.sincronizar(List.of(
                    recebida(10L, dadosLeves()), recebida(11L, dadosLeves())), sessao);

            assertThat(resultados).extracting(TriagemService.Resultado::situacao)
                    .containsExactly("REJEITADO", "CRIADO");
            assertThat(resultados.get(0).mensagem()).isEqualTo("Paciente não encontrado");
            verify(dossies, times(1)).save(any());
        }

        @Test
        void deveRecusarSemConsentimento() {
            paciente.setConsentimentoEm(null);
            when(pacientes.findById(10L)).thenReturn(Optional.of(paciente));

            var resultado = service.sincronizar(List.of(recebida(10L, dadosLeves())), sessao).get(0);

            assertThat(resultado.situacao()).isEqualTo("REJEITADO");
            assertThat(resultado.mensagem()).contains("ainda não consentiu");
            verify(dossies, never()).save(any());
        }

        @Test
        void deveRecusarTriagemIncompleta() {
            when(pacientes.findById(10L)).thenReturn(Optional.of(paciente));

            var resultado = service.sincronizar(List.of(recebida(10L, null)), sessao).get(0);

            assertThat(resultado.situacao()).isEqualTo("REJEITADO");
            assertThat(resultado.mensagem()).contains("RN03");
            verify(dossies, never()).save(any());
        }

        @Test
        void deveRecusarTriagemDeOutraMicroarea() {
            Microarea outra = new Microarea();
            outra.setId(2L);
            paciente.setMicroarea(outra);
            when(pacientes.findById(10L)).thenReturn(Optional.of(paciente));

            var resultado = service.sincronizar(List.of(recebida(10L, dadosLeves())), sessao).get(0);

            assertThat(resultado.situacao()).isEqualTo("REJEITADO");
            assertThat(resultado.mensagem()).contains("RN02");
            verify(dossies, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Encaminhamento e filas")
    class Encaminhamento {
        private Dossie prepararDossie() {
            Dossie dossie = new Dossie();
            Especialidade especialidade = new Especialidade();
            especialidade.setId(7L);
            when(dossies.findById(20L)).thenReturn(Optional.of(dossie));
            when(especialidades.findById(7L)).thenReturn(Optional.of(especialidade));
            when(dossies.save(any(Dossie.class))).thenAnswer(i -> i.getArgument(0));
            return dossie;
        }

        @Test
        void deveEncaminharEIniciarPrazoQuandoHaMedico() {
            prepararDossie();
            when(medicos.findByEspecialidadeIdAndCrmAtivoTrue(7L)).thenReturn(List.of(new Medico()));

            Dossie salvo = service.encaminhar(20L, 7L);

            assertThat(salvo.getStatus()).isEqualTo(StatusDossie.ENCAMINHADO);
            assertThat(salvo.getEspecialidade().getId()).isEqualTo(7L);
            assertThat(salvo.getEncaminhadoEm()).isNotNull();
        }

        @Test
        void deveAguardarQuandoNaoHaMedico() {
            prepararDossie();
            when(medicos.findByEspecialidadeIdAndCrmAtivoTrue(7L)).thenReturn(List.of());

            Dossie salvo = service.encaminhar(20L, 7L);

            assertThat(salvo.getStatus()).isEqualTo(StatusDossie.AGUARDANDO_ENCAMINHAMENTO);
            assertThat(salvo.getEncaminhadoEm()).isNull();
        }

        @Test
        void deveRecusarEncaminhamentoDeCasoComLaudo() {
            prepararDossie().setStatus(StatusDossie.LAUDO_EMITIDO);

            assertThatThrownBy(() -> service.encaminhar(20L, 7L))
                    .isInstanceOf(RegraNegocioException.class).hasMessage("Este caso já tem laudo");
            verify(dossies, never()).save(any());
        }

        @Test
        void deveConsultarFilaPelaEspecialidadeDoMedico() {
            var medico = new AuthService.Sessao("teste", new Usuario(), null, 7L);
            Dossie caso = new Dossie();
            when(dossies.findByStatusAndEspecialidadeIdOrderByEscoreDescEncaminhadoEmAsc(
                    StatusDossie.ENCAMINHADO, 7L)).thenReturn(List.of(caso));

            assertThat(service.fila(medico)).containsExactly(caso);
        }

        @Test
        void deveListarSomenteTriagensDoProprioAcs() {
            Dossie caso = new Dossie();
            when(dossies.findByAcsUsuarioIdOrderByColetadoEmDesc(3L)).thenReturn(List.of(caso));

            assertThat(service.listar(sessao, null)).containsExactly(caso);
            verify(dossies, never()).findAllByOrderByColetadoEmDesc();
        }
    }
}
