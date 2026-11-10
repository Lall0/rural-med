package br.mackenzie.ruralmed.service;

import br.mackenzie.ruralmed.model.*;
import br.mackenzie.ruralmed.regras.AcessoNegadoException;
import br.mackenzie.ruralmed.repository.DossieRepository;
import br.mackenzie.ruralmed.repository.MedicoRepository;
import br.mackenzie.ruralmed.repository.ReceitaRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PrescricaoServiceTest {
    private final DossieRepository dossies = mock(DossieRepository.class);
    private final MedicoRepository medicos = mock(MedicoRepository.class);
    private final ReceitaRepository receitas = mock(ReceitaRepository.class);
    private final PrescricaoService service = new PrescricaoService(dossies, medicos, receitas);
    private final Usuario usuario = new Usuario();
    private final AuthService.Sessao sessao = new AuthService.Sessao("token", usuario, null, 7L);
    private final Dossie dossie = new Dossie();
    private final Medico medico = new Medico();

    @BeforeEach
    void preparar() {
        usuario.setId(5L);
        usuario.setPerfil(Perfil.MEDICO);
        medico.setCrmAtivo(true);
        medico.setCrm("123456");
        medico.setUfCrm("SP");
        Especialidade especialidade = new Especialidade();
        especialidade.setId(7L);
        dossie.setStatus(StatusDossie.ENCAMINHADO);
        dossie.setEspecialidade(especialidade);
        when(dossies.findById(12L)).thenReturn(Optional.of(dossie));
        when(medicos.findById(5L)).thenReturn(Optional.of(medico));
        when(receitas.save(any(Receita.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void deveBuscarCatalogoSemIndicarMedicamentoPorQueixa() {
        List<PrescricaoService.MedicamentoReferencia> encontrados = service.buscarMedicamentos("PARACETAMOL");

        assertThat(encontrados).singleElement().satisfies(m -> {
            assertThat(m.nome()).isEqualTo("Paracetamol");
            assertThat(m.principioAtivo()).isEqualTo("paracetamol");
            assertThat(m.apresentacao()).isNotBlank();
        });
        assertThat(service.buscarMedicamentos("a")).isEmpty();
    }

    @Test
    void deveSalvarPrescricaoAssinadaSemCriarLaudo() {
        var form = new PrescricaoService.PrescricaoForm("Paracetamol", "comprimido ou solução oral",
                "Conforme avaliação clínica", "Oral", "Conforme avaliação clínica",
                "Conforme avaliação clínica", "Orientações fornecidas ao paciente", false);

        PrescricaoService.ReceitaResumo receita = service.prescrever(12L, form, sessao);

        assertThat(receita.medicamento()).isEqualTo("Paracetamol");
        assertThat(receita.assinaturaDigital()).contains("CRM 123456/SP");
        verify(receitas).save(argThat(r -> r.getLaudo() == null
                && r.getDossie() == dossie && r.getMedico() == medico
                && r.getStatus() == StatusReceita.ASSINADA));
    }

    @Test
    void deveImpedirPrescricaoEmCasoDeOutraEspecialidade() {
        AuthService.Sessao outraEspecialidade = new AuthService.Sessao("token", usuario, null, 8L);
        var form = new PrescricaoService.PrescricaoForm("Paracetamol", "comprimido ou solução oral",
                "Dose", "Oral", "Frequência", "Duração", null, false);

        assertThatThrownBy(() -> service.prescrever(12L, form, outraEspecialidade))
                .isInstanceOf(AcessoNegadoException.class);
        verify(receitas, never()).save(any());
    }

    @Test
    void deveExigirMedicamentoDoCatalogo() {
        var form = new PrescricaoService.PrescricaoForm("Medicamento desconhecido", "apresentação",
                "Dose", "Oral", "Frequência", "Duração", null, false);

        assertThatThrownBy(() -> service.prescrever(12L, form, sessao))
                .hasMessage("Selecione um medicamento do catálogo");
        verify(receitas, never()).save(any());
    }
}
