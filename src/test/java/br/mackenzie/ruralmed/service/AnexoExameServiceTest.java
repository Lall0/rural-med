package br.mackenzie.ruralmed.service;

import br.mackenzie.ruralmed.model.*;
import br.mackenzie.ruralmed.regras.RegraNegocioException;
import br.mackenzie.ruralmed.repository.AnexoExameRepository;
import br.mackenzie.ruralmed.repository.DossieRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AnexoExameServiceTest {
    private final AnexoExameRepository anexos = mock(AnexoExameRepository.class);
    private final DossieRepository dossies = mock(DossieRepository.class);
    private final AnexoExameService service = new AnexoExameService(anexos, dossies);
    private final AuthService.Sessao sessao = new AuthService.Sessao("token", usuarioUbs(), null, null);
    private final Dossie dossie = new Dossie();

    @BeforeEach
    void preparar() {
        when(dossies.findById(3L)).thenReturn(Optional.of(dossie));
        when(anexos.save(any(AnexoExame.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void devePersistirArquivoDeExameNoDossie() {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "resultado.pdf", "application/pdf", new byte[]{'%', 'P', 'D', 'F'});

        AnexoExame salvo = service.salvar(3L, arquivo, sessao);

        assertThat(salvo.getNomeArquivo()).isEqualTo("resultado.pdf");
        assertThat(salvo.getTipoConteudo()).isEqualTo("application/pdf");
        assertThat(salvo.getConteudo()).containsExactly((byte) '%', (byte) 'P', (byte) 'D', (byte) 'F');
        assertThat(salvo.getDossie()).isSameAs(dossie);
        verify(anexos).save(salvo);
    }

    @Test
    void deveRecusarTipoDeArquivoNaoPermitido() {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "script.html", "text/html", new byte[]{1});

        assertThatThrownBy(() -> service.salvar(3L, arquivo, sessao))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Formato não permitido");
        verify(anexos, never()).save(any());
    }

    private static Usuario usuarioUbs() {
        Usuario usuario = new Usuario();
        usuario.setId(8L);
        usuario.setPerfil(Perfil.UBS);
        return usuario;
    }
}
