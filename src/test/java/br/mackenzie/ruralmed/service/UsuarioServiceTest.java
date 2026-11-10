package br.mackenzie.ruralmed.service;

import br.mackenzie.ruralmed.model.*;
import br.mackenzie.ruralmed.regras.RegraNegocioException;
import br.mackenzie.ruralmed.repository.*;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Cadastro e inativacao de usuarios")
class UsuarioServiceTest {
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final AcsRepository agentes = mock(AcsRepository.class);
    private final MedicoRepository medicos = mock(MedicoRepository.class);
    private final MicroareaRepository areas = mock(MicroareaRepository.class);
    private final EspecialidadeRepository especialidades = mock(EspecialidadeRepository.class);
    private final ClinicaParceiraRepository clinicas = mock(ClinicaParceiraRepository.class);
    private final AuthService auth = new AuthService(usuarios, agentes, medicos);
    private final UsuarioService service = new UsuarioService(usuarios, agentes, medicos,
            areas, especialidades, clinicas, auth);

    private UsuarioService.UsuarioForm form(Perfil perfil, String senha, Boolean crmAtivo) {
        return new UsuarioService.UsuarioForm("Usuario de teste", "TESTE@RURALMED.DEV",
                "10000000001", senha, perfil, 1L, 2L, null, "123456", "SP", crmAtivo,
                new BigDecimal("150.00"));
    }

    @Nested
    @DisplayName("Cadastros validos")
    class Sucesso {
        @Test
        void deveCadastrarUbsComEmailNormalizadoESenhaProtegida() {
            Usuario salvo = service.criar(form(Perfil.UBS, "senha-teste", null));

            assertThat(salvo.getEmail()).isEqualTo("teste@ruralmed.dev");
            assertThat(salvo.getPerfil()).isEqualTo(Perfil.UBS);
            assertThat(salvo.getAtivo()).isTrue();
            assertThat(new BCryptPasswordEncoder().matches("senha-teste", salvo.getSenhaHash())).isTrue();
            verify(usuarios).save(salvo);
        }

        @Test
        void deveVincularAcsAMicroarea() {
            Microarea area = new Microarea();
            area.setId(1L);
            when(areas.findById(1L)).thenReturn(Optional.of(area));
            ArgumentCaptor<Acs> captor = ArgumentCaptor.forClass(Acs.class);

            Usuario salvo = service.criar(form(Perfil.ACS, "senha-teste", null));

            verify(agentes).save(captor.capture());
            assertThat(captor.getValue().getUsuario()).isSameAs(salvo);
            assertThat(captor.getValue().getMicroarea()).isSameAs(area);
        }

        @Test
        void deveVincularMedicoAEspecialidadeEValorDoLaudo() {
            Especialidade especialidade = new Especialidade();
            especialidade.setId(2L);
            when(especialidades.findById(2L)).thenReturn(Optional.of(especialidade));
            ArgumentCaptor<Medico> captor = ArgumentCaptor.forClass(Medico.class);

            Usuario salvo = service.criar(form(Perfil.MEDICO, "senha-teste", true));

            verify(medicos).save(captor.capture());
            assertThat(captor.getValue().getUsuario()).isSameAs(salvo);
            assertThat(captor.getValue().getEspecialidade()).isSameAs(especialidade);
            assertThat(captor.getValue().getValorPorLaudo()).isEqualByComparingTo("150.00");
        }

        @Test
        void deveInativarUsuarioSemExcluirRegistro() {
            Usuario usuario = new Usuario();
            when(usuarios.findById(1L)).thenReturn(Optional.of(usuario));

            service.inativar(1L);

            assertThat(usuario.getAtivo()).isFalse();
            verify(usuarios).save(usuario);
            verify(usuarios, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("Cadastros recusados")
    class Falhas {
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "12345")
        void deveExigirSenhaComPeloMenosSeisCaracteres(String senha) {
            assertThatThrownBy(() -> service.criar(form(Perfil.UBS, senha, null)))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessage("A senha precisa ter pelo menos 6 caracteres");
            verify(usuarios, never()).save(any());
        }

        @Test
        void deveRecusarEmailDuplicado() {
            when(usuarios.findByEmail("teste@ruralmed.dev")).thenReturn(Optional.of(new Usuario()));

            assertThatThrownBy(() -> service.criar(form(Perfil.UBS, "senha-teste", null)))
                    .isInstanceOf(RegraNegocioException.class).hasMessage("Este e-mail já está cadastrado");
            verify(usuarios, never()).save(any());
        }

        @Test
        void deveRecusarMedicoSemCrmAtivo() {
            when(especialidades.findById(2L)).thenReturn(Optional.of(new Especialidade()));

            assertThatThrownBy(() -> service.criar(form(Perfil.MEDICO, "senha-teste", false)))
                    .isInstanceOf(RegraNegocioException.class).hasMessageContaining("RN04");
            verify(medicos, never()).save(any());
        }

        @Test
        void deveRecusarInativacaoDeUsuarioInexistente() {
            assertThatThrownBy(() -> service.inativar(99L))
                    .isInstanceOf(RegraNegocioException.class).hasMessage("Usuário não encontrado");
        }
    }
}
