package br.mackenzie.ruralmed.service;

import br.mackenzie.ruralmed.model.*;
import br.mackenzie.ruralmed.regras.AcessoNegadoException;
import br.mackenzie.ruralmed.regras.RegraNegocioException;
import br.mackenzie.ruralmed.repository.*;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Autenticacao e permissoes")
class AuthServiceTest {
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final AcsRepository agentes = mock(AcsRepository.class);
    private final MedicoRepository medicos = mock(MedicoRepository.class);
    private final AuthService auth = new AuthService(usuarios, agentes, medicos);
    private Usuario usuario;

    @BeforeEach
    void prepararUsuario() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setPerfil(Perfil.UBS);
        usuario.setSenhaHash("$2a$10$RqVvcwxpr5Jn1yMMamWojOi/5bHfMpIBixX8H/sKl9zcbOcO1Twua");
    }

    @Nested
    @DisplayName("Acessos validos")
    class Sucesso {
        @Test
        void deveNormalizarEmailECriarSessaoAutorizada() {
            when(usuarios.findByEmail("ubs@ruralmed.dev")).thenReturn(Optional.of(usuario));

            var sessao = auth.login(" UBS@RURALMED.DEV ", "ruralmed123");

            assertThat(sessao.token()).isNotBlank();
            assertThat(auth.exigir(sessao.token(), Perfil.UBS)).isSameAs(sessao);
            assertThat(sessao.usuario()).isSameAs(usuario);
        }

        @Test
        void deveRemoverFormatacaoDoCpf() {
            when(usuarios.findByCpf("10000000001")).thenReturn(Optional.of(usuario));

            var sessao = auth.login("100.000.000-01", "ruralmed123");

            assertThat(auth.exigir(sessao.token()).usuario().getPerfil()).isEqualTo(Perfil.UBS);
        }

        @Test
        void deveCarregarMicroareaDoAcs() {
            usuario.setPerfil(Perfil.ACS);
            Microarea area = new Microarea();
            area.setId(12L);
            Acs acs = new Acs();
            acs.setMicroarea(area);
            when(usuarios.findByEmail("acs@ruralmed.dev")).thenReturn(Optional.of(usuario));
            when(agentes.findById(1L)).thenReturn(Optional.of(acs));

            var sessao = auth.login("acs@ruralmed.dev", "ruralmed123");

            assertThat(sessao.microareaId()).isEqualTo(12L);
            assertThat(sessao.especialidadeId()).isNull();
        }

        @Test
        void deveCarregarEspecialidadeDoMedico() {
            usuario.setPerfil(Perfil.MEDICO);
            Especialidade especialidade = new Especialidade();
            especialidade.setId(7L);
            Medico medico = new Medico();
            medico.setEspecialidade(especialidade);
            medico.setCrmAtivo(true);
            when(usuarios.findByEmail("medico@ruralmed.dev")).thenReturn(Optional.of(usuario));
            when(medicos.findById(1L)).thenReturn(Optional.of(medico));

            var sessao = auth.login("medico@ruralmed.dev", "ruralmed123");

            assertThat(sessao.especialidadeId()).isEqualTo(7L);
            assertThat(sessao.microareaId()).isNull();
        }

        @Test
        void deveGerarHashCompativelSemGuardarSenhaEmTexto() {
            String hash = auth.gerarHash("senha-de-teste");

            assertThat(hash).isNotEqualTo("senha-de-teste");
            assertThat(new BCryptPasswordEncoder().matches("senha-de-teste", hash)).isTrue();
        }
    }

    @Nested
    @DisplayName("Acessos recusados")
    class Falhas {
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = " ")
        void deveRecusarIdentificadorAusente(String login) {
            assertThatThrownBy(() -> auth.login(login, "ruralmed123"))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessage("E-mail/CPF ou senha inválidos");
            verifyNoInteractions(usuarios);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = " ")
        void deveRecusarSenhaAusente(String senha) {
            assertThatThrownBy(() -> auth.login("ubs@ruralmed.dev", senha))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessage("E-mail/CPF ou senha inválidos");
            verifyNoInteractions(usuarios);
        }

        @Test
        void deveRecusarContaInexistente() {
            when(usuarios.findByEmail("ausente@ruralmed.dev")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> auth.login("ausente@ruralmed.dev", "ruralmed123"))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessage("E-mail/CPF ou senha inválidos");
        }

        @Test
        void deveRecusarSenhaIncorreta() {
            when(usuarios.findByEmail("ubs@ruralmed.dev")).thenReturn(Optional.of(usuario));

            assertThatThrownBy(() -> auth.login("ubs@ruralmed.dev", "incorreta"))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessage("E-mail/CPF ou senha inválidos");
        }

        @Test
        void deveRecusarUsuarioInativo() {
            usuario.setAtivo(false);
            when(usuarios.findByEmail("ubs@ruralmed.dev")).thenReturn(Optional.of(usuario));

            assertThatThrownBy(() -> auth.login("ubs@ruralmed.dev", "ruralmed123"))
                    .isInstanceOf(RegraNegocioException.class).hasMessage("Usuário inativo");
        }

        @Test
        void deveRecusarMedicoSemCrmAtivo() {
            usuario.setPerfil(Perfil.MEDICO);
            Medico medico = new Medico();
            medico.setCrmAtivo(false);
            when(usuarios.findByEmail("medico@ruralmed.dev")).thenReturn(Optional.of(usuario));
            when(medicos.findById(1L)).thenReturn(Optional.of(medico));

            assertThatThrownBy(() -> auth.login("medico@ruralmed.dev", "ruralmed123"))
                    .isInstanceOf(RegraNegocioException.class).hasMessageContaining("RN04");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "token-inexistente")
        void deveExigirSessaoValida(String token) {
            assertThatThrownBy(() -> auth.exigir(token))
                    .isInstanceOf(AcessoNegadoException.class).hasMessage("Faça login para continuar");
        }

        @Test
        void deveRecusarOutroPerfilEInvalidarTokenNoLogout() {
            when(usuarios.findByEmail("ubs@ruralmed.dev")).thenReturn(Optional.of(usuario));
            var sessao = auth.login("ubs@ruralmed.dev", "ruralmed123");

            assertThatThrownBy(() -> auth.exigir(sessao.token(), Perfil.MEDICO))
                    .isInstanceOf(AcessoNegadoException.class)
                    .hasMessage("Seu perfil não tem acesso a esta funcionalidade");
            auth.sair(sessao.token());

            assertThatThrownBy(() -> auth.exigir(sessao.token()))
                    .isInstanceOf(AcessoNegadoException.class).hasMessage("Faça login para continuar");
        }
    }
}
