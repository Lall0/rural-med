package br.mackenzie.ruralmed.service;

import br.mackenzie.ruralmed.model.*;
import br.mackenzie.ruralmed.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Indicadores e repasses")
class RelatorioServiceTest {
    private final DossieRepository dossies = mock(DossieRepository.class);
    private final LaudoRepository laudos = mock(LaudoRepository.class);
    private final RelatorioService service = new RelatorioService(dossies, laudos);
    private final OffsetDateTime inicio = OffsetDateTime.parse("2026-11-01T00:00:00-03:00");
    private final OffsetDateTime fim = OffsetDateTime.parse("2026-12-01T00:00:00-03:00");

    @Nested
    @DisplayName("Resultados calculados")
    class Sucesso {
        @Test
        void deveSomarTriagensPrioritariosEEconomiaPorMunicipio() {
            Dossie prioritario = new Dossie();
            prioritario.setPrioritario(true);
            when(dossies.findByColetadoEmBetween(inicio, fim))
                    .thenReturn(List.of(prioritario, new Dossie()));
            when(laudos.findByStatusAndEmitidoEmBetween(StatusLaudo.EMITIDO, inicio, fim))
                    .thenReturn(List.of(laudoComCusto("80.50"), laudoComCusto("120.00")));

            var resultado = service.atendimentos(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));

            assertThat(resultado.triagens()).isEqualTo(2);
            assertThat(resultado.prioritarias()).isEqualTo(1);
            assertThat(resultado.laudosEmitidos()).isEqualTo(2);
            assertThat(resultado.viagensTfdEvitadas()).isEqualTo(2);
            assertThat(resultado.economiaEstimada()).isEqualByComparingTo("200.50");
        }

        @Test
        void deveAgruparRepassesPorMedicoComValoresDecimaisExatos() {
            Medico medico = new Medico();
            Usuario usuario = new Usuario();
            usuario.setId(6L);
            usuario.setNome("Medico de teste");
            medico.setUsuario(usuario);
            medico.setUsuarioId(6L);
            Especialidade especialidade = new Especialidade();
            especialidade.setNome("Cardiologia");
            medico.setEspecialidade(especialidade);
            medico.setValorPorLaudo(new BigDecimal("150.25"));
            Laudo primeiro = new Laudo();
            primeiro.setMedico(medico);
            Laudo segundo = new Laudo();
            segundo.setMedico(medico);
            when(laudos.findByStatusAndEmitidoEmBetween(StatusLaudo.EMITIDO, inicio, fim))
                    .thenReturn(List.of(primeiro, segundo));

            var extrato = service.repasses(2026, 11);

            assertThat(extrato).singleElement().satisfies(linha -> {
                assertThat(linha.medico()).isEqualTo("Medico de teste");
                assertThat(linha.especialidade()).isEqualTo("Cardiologia");
                assertThat(linha.laudos()).isEqualTo(2);
                assertThat(linha.valorPorLaudo()).isEqualByComparingTo("150.25");
                assertThat(linha.total()).isEqualByComparingTo("300.50");
            });
        }
    }

    @Nested
    @DisplayName("Periodos sem movimento")
    class SemMovimento {
        @Test
        void deveRetornarIndicadoresZerados() {
            var resultado = service.atendimentos(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));

            assertThat(resultado).isEqualTo(new RelatorioService.Relatorio(0, 0, 0, 0, BigDecimal.ZERO));
        }

        @Test
        void deveRetornarExtratoVazio() {
            assertThat(service.repasses(2026, 11)).isEmpty();
        }
    }

    private Laudo laudoComCusto(String custo) {
        Municipio municipio = new Municipio();
        municipio.setCustoMedioViagemTfd(new BigDecimal(custo));
        Ubs ubs = new Ubs();
        ubs.setMunicipio(municipio);
        Microarea area = new Microarea();
        area.setUbs(ubs);
        Paciente paciente = new Paciente();
        paciente.setMicroarea(area);
        Dossie dossie = new Dossie();
        dossie.setPaciente(paciente);
        Laudo laudo = new Laudo();
        laudo.setDossie(dossie);
        return laudo;
    }
}
