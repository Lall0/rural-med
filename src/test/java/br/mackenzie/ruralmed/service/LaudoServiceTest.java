package br.mackenzie.ruralmed.service;

import br.mackenzie.ruralmed.model.*;
import br.mackenzie.ruralmed.regras.RegraNegocioException;
import br.mackenzie.ruralmed.repository.*;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Emissao de laudos e receitas")
class LaudoServiceTest {
    private final LaudoRepository laudos = mock(LaudoRepository.class);
    private final ReceitaRepository receitas = mock(ReceitaRepository.class);
    private final DossieRepository dossies = mock(DossieRepository.class);
    private final MedicoRepository medicos = mock(MedicoRepository.class);
    private final CidRepository cids = mock(CidRepository.class);
    private final LaudoService service = new LaudoService(laudos, receitas, dossies, medicos, cids);
    private AuthService.Sessao sessao;
    private Dossie dossie;
    private Medico medico;

    @BeforeEach
    void prepararCasoEncaminhado() {
        Usuario usuario = new Usuario();
        usuario.setId(6L);
        usuario.setPerfil(Perfil.MEDICO);
        sessao = new AuthService.Sessao("teste", usuario, null, 1L);
        dossie = new Dossie();
        dossie.setStatus(StatusDossie.ENCAMINHADO);
        medico = new Medico();
        medico.setCrmAtivo(true);
        medico.setCrm("123456");
        medico.setUfCrm("SP");
        Cid cid = new Cid();
        cid.setCodigo("I20.9");
        when(dossies.findById(1L)).thenReturn(Optional.of(dossie));
        when(medicos.findById(6L)).thenReturn(Optional.of(medico));
        when(cids.findById("I20.9")).thenReturn(Optional.of(cid));
    }

    @Nested
    @DisplayName("Emissao valida")
    class Sucesso {
        @Test
        void deveEmitirLaudoReceitaEConcluirDossie() {
            Laudo laudo = service.emitir(1L,
                    new LaudoService.LaudoForm("Parecer de teste", "I20.9", "Prescricao de teste"), sessao);

            assertThat(laudo.getStatus()).isEqualTo(StatusLaudo.EMITIDO);
            assertThat(laudo.getParecer()).isEqualTo("Parecer de teste");
            assertThat(laudo.getCid().getCodigo()).isEqualTo("I20.9");
            assertThat(laudo.getAssinaturaDigital())
                    .matches("ICP-Brasil \\(simulada\\) \\| CRM 123456/SP \\| [a-f0-9]{64}");
            assertThat(laudo.getReceitas()).singleElement().satisfies(receita -> {
                assertThat(receita.getStatus()).isEqualTo(StatusReceita.ASSINADA);
                assertThat(receita.getPrescricao()).isEqualTo("Prescricao de teste");
                assertThat(receita.getLaudo()).isSameAs(laudo);
            });
            assertThat(dossie.getStatus()).isEqualTo(StatusDossie.LAUDO_EMITIDO);
            verify(laudos).save(laudo);
            verify(receitas).save(laudo.getReceitas().get(0));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = " ")
        void deveEmitirSemReceitaQuandoPrescricaoNaoFoiInformada(String prescricao) {
            Laudo laudo = service.emitir(1L,
                    new LaudoService.LaudoForm("Parecer de teste", "I20.9", prescricao), sessao);

            assertThat(laudo.getReceitas()).isEmpty();
            assertThat(dossie.getStatus()).isEqualTo(StatusDossie.LAUDO_EMITIDO);
            verifyNoInteractions(receitas);
        }
    }

    @Nested
    @DisplayName("Emissao recusada")
    class Falhas {
        @ParameterizedTest
        @EnumSource(value = StatusDossie.class, names = "ENCAMINHADO", mode = EnumSource.Mode.EXCLUDE)
        void deveRecusarCasoNaoEncaminhadoOuJaConcluido(StatusDossie status) {
            dossie.setStatus(status);

            assertThatThrownBy(() -> service.emitir(1L,
                    new LaudoService.LaudoForm("Parecer", "I20.9", null), sessao))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessage("Só é possível dar laudo a casos encaminhados");
            verify(laudos, never()).save(any());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = " ")
        void deveExigirParecer(String parecer) {
            assertThatThrownBy(() -> service.emitir(1L,
                    new LaudoService.LaudoForm(parecer, "I20.9", null), sessao))
                    .isInstanceOf(RegraNegocioException.class).hasMessage("Escreva o parecer");
            verify(laudos, never()).save(any());
        }

        @Test
        void deveExigirCidCadastrado() {
            when(cids.findById("INVALIDO")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.emitir(1L,
                    new LaudoService.LaudoForm("Parecer", "INVALIDO", null), sessao))
                    .isInstanceOf(RegraNegocioException.class).hasMessage("Escolha um código CID válido");
            verify(laudos, never()).save(any());
        }

        @Test
        void deveRevalidarCrmAntesDeEmitir() {
            medico.setCrmAtivo(false);

            assertThatThrownBy(() -> service.emitir(1L,
                    new LaudoService.LaudoForm("Parecer", "I20.9", null), sessao))
                    .isInstanceOf(RegraNegocioException.class).hasMessageContaining("RN04");
            verify(laudos, never()).save(any());
        }
    }
}
