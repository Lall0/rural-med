package br.mackenzie.ruralmed.service;

import br.mackenzie.ruralmed.model.*;
import br.mackenzie.ruralmed.regras.RegraNegocioException;
import br.mackenzie.ruralmed.repository.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Cadastro e acesso a pacientes")
class PacienteServiceTest {
    private final PacienteRepository pacientes = mock(PacienteRepository.class);
    private final MicroareaRepository areas = mock(MicroareaRepository.class);
    private final PacienteService service = new PacienteService(pacientes, areas);

    private AuthService.Sessao sessao(Perfil perfil) {
        Usuario usuario = new Usuario();
        usuario.setPerfil(perfil);
        return new AuthService.Sessao("teste", usuario, 1L, null);
    }

    private Paciente paciente(Long areaId) {
        Microarea area = new Microarea();
        area.setId(areaId);
        Paciente paciente = new Paciente();
        paciente.setNome("Paciente de teste");
        paciente.setCpf("10000000001");
        paciente.setMicroarea(area);
        return paciente;
    }

    @Nested
    @DisplayName("Operacoes permitidas")
    class Sucesso {
        @Test
        void deveListarSomentePacientesAtivosDaMicroareaParaAcs() {
            Paciente paciente = paciente(1L);
            when(pacientes.findByMicroareaIdAndAtivoTrueOrderByNome(1L)).thenReturn(List.of(paciente));

            assertThat(service.listar(sessao(Perfil.ACS))).containsExactly(paciente);
            verify(pacientes, never()).findAllByOrderByNome();
        }

        @Test
        void deveListarTodasAsMicroareasParaUbs() {
            Paciente paciente = paciente(2L);
            when(pacientes.findAllByOrderByNome()).thenReturn(List.of(paciente));

            assertThat(service.listar(sessao(Perfil.UBS))).containsExactly(paciente);
        }

        @Test
        void deveFixarMicroareaDoCadastroNaAreaDoAcs() {
            Paciente dados = paciente(2L);
            Microarea areaDoAcs = paciente(1L).getMicroarea();
            when(areas.findById(1L)).thenReturn(Optional.of(areaDoAcs));
            when(pacientes.save(any(Paciente.class))).thenAnswer(i -> i.getArgument(0));

            Paciente salvo = service.criar(dados, sessao(Perfil.ACS));

            assertThat(salvo.getMicroarea().getId()).isEqualTo(1L);
            assertThat(salvo.getNome()).isEqualTo("Paciente de teste");
        }

        @Test
        void deveAceitarCnsSemCpf() {
            Paciente dados = paciente(1L);
            dados.setCpf(" ");
            dados.setCns("123456789000001");
            when(areas.findById(1L)).thenReturn(Optional.of(dados.getMicroarea()));
            when(pacientes.save(any(Paciente.class))).thenAnswer(i -> i.getArgument(0));

            Paciente salvo = service.criar(dados, sessao(Perfil.UBS));

            assertThat(salvo.getCpf()).isNull();
            assertThat(salvo.getCns()).isEqualTo("123456789000001");
        }

        @Test
        void deveInativarSemApagarHistorico() {
            Paciente paciente = paciente(1L);
            when(pacientes.findById(10L)).thenReturn(Optional.of(paciente));

            service.inativar(10L, sessao(Perfil.ACS));

            assertThat(paciente.getAtivo()).isFalse();
            verify(pacientes).save(paciente);
            verify(pacientes, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("Rejeicoes de cadastro e acesso")
    class Falhas {
        @Test
        void deveRecusarPacienteDeOutraMicroareaParaAcs() {
            when(pacientes.findById(10L)).thenReturn(Optional.of(paciente(2L)));

            assertThatThrownBy(() -> service.buscar(10L, sessao(Perfil.ACS)))
                    .isInstanceOf(RegraNegocioException.class).hasMessageContaining("RN02");
        }

        @Test
        void deveRecusarPacienteInexistente() {
            when(pacientes.findById(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.buscar(10L, sessao(Perfil.UBS)))
                    .isInstanceOf(RegraNegocioException.class).hasMessage("Paciente não encontrado");
        }

        @Test
        void deveExigirCpfOuCns() {
            Paciente dados = paciente(1L);
            dados.setCpf(null);

            assertThatThrownBy(() -> service.criar(dados, sessao(Perfil.ACS)))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessage("Informe o CPF ou o Cartão Nacional de Saúde (CNS)");
            verify(pacientes, never()).save(any());
        }

        @Test
        void deveExigirMicroareaNoCadastroDaUbs() {
            Paciente dados = paciente(1L);
            dados.setMicroarea(null);

            assertThatThrownBy(() -> service.criar(dados, sessao(Perfil.UBS)))
                    .isInstanceOf(RegraNegocioException.class).hasMessage("Escolha a microárea do paciente");
        }
    }
}
