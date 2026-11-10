package br.mackenzie.ruralmed.service;

import br.mackenzie.ruralmed.model.*;
import br.mackenzie.ruralmed.regras.AcessoNegadoException;
import br.mackenzie.ruralmed.regras.RegraNegocioException;
import br.mackenzie.ruralmed.regras.RegrasDeNegocio;
import br.mackenzie.ruralmed.repository.DossieRepository;
import br.mackenzie.ruralmed.repository.MedicoRepository;
import br.mackenzie.ruralmed.repository.ReceitaRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrescricaoService {

    public record MedicamentoReferencia(String nome, String principioAtivo, String apresentacao) {
    }

    public record PrescricaoForm(String medicamento, String apresentacao, String dose, String viaAdministracao,
            String frequencia, String duracao, String orientacoes, Boolean farmaciaPopular) {
    }

    public record ReceitaResumo(Long id, String medicamento, String apresentacao, String descricao, String dose,
            String viaAdministracao, String frequencia, String duracao, String orientacoes,
            Boolean farmaciaPopular, OffsetDateTime assinadaEm, String assinaturaDigital) {
    }

    private static final List<MedicamentoReferencia> CATALOGO = List.of(
            new MedicamentoReferencia("Paracetamol", "paracetamol", "comprimido ou solução oral"),
            new MedicamentoReferencia("Dipirona", "dipirona monoidratada", "comprimido ou solução oral"),
            new MedicamentoReferencia("Ibuprofeno", "ibuprofeno", "comprimido ou suspensão oral"),
            new MedicamentoReferencia("Loratadina", "loratadina", "comprimido ou solução oral"),
            new MedicamentoReferencia("Amoxicilina", "amoxicilina", "cápsula ou suspensão oral"),
            new MedicamentoReferencia("Azitromicina", "azitromicina", "comprimido ou suspensão oral"),
            new MedicamentoReferencia("Omeprazol", "omeprazol", "cápsula"),
            new MedicamentoReferencia("Salbutamol", "salbutamol", "solução para inalação ou aerossol")
    );

    private final DossieRepository dossieRepository;
    private final MedicoRepository medicoRepository;
    private final ReceitaRepository receitaRepository;

    public PrescricaoService(DossieRepository dossieRepository, MedicoRepository medicoRepository,
                             ReceitaRepository receitaRepository) {
        this.dossieRepository = dossieRepository;
        this.medicoRepository = medicoRepository;
        this.receitaRepository = receitaRepository;
    }

    public List<MedicamentoReferencia> buscarMedicamentos(String busca) {
        String termo = normalizarBusca(busca);
        if (termo.length() < 2) return List.of();
        return CATALOGO.stream()
                .filter(m -> normalizarBusca(m.nome()).contains(termo)
                        || normalizarBusca(m.principioAtivo()).contains(termo))
                .toList();
    }

    @Transactional
    public ReceitaResumo prescrever(Long dossieId, PrescricaoForm form, AuthService.Sessao sessao) {
        if (form == null) throw new RegraNegocioException("Informe os dados da prescrição");
        Dossie dossie = dossieRepository.findById(dossieId)
                .orElseThrow(() -> new RegraNegocioException("Atendimento não encontrado"));
        if (dossie.getStatus() != StatusDossie.ENCAMINHADO) {
            throw new RegraNegocioException("Só é possível prescrever durante um atendimento encaminhado");
        }
        if (dossie.getEspecialidade() == null
                || !dossie.getEspecialidade().getId().equals(sessao.especialidadeId())) {
            throw new AcessoNegadoException("O médico só pode prescrever em casos da própria especialidade");
        }

        Medico medico = medicoRepository.findById(sessao.usuario().getId()).orElseThrow();
        RegrasDeNegocio.verificarCrmAtivo(medico);
        MedicamentoReferencia medicamento = CATALOGO.stream()
                .filter(m -> m.nome().equals(form.medicamento())
                        && m.apresentacao().equals(form.apresentacao()))
                .findFirst()
                .orElseThrow(() -> new RegraNegocioException("Selecione um medicamento do catálogo"));
        validarCampo(form.dose(), "Informe a dose indicada pelo médico");
        validarCampo(form.viaAdministracao(), "Informe a via de administração");
        validarCampo(form.frequencia(), "Informe a frequência de uso");
        validarCampo(form.duracao(), "Informe a duração do tratamento");
        if (!List.of("Oral", "Tópica", "Inalatória", "Oftálmica", "Otológica", "Outra")
                .contains(form.viaAdministracao().trim())) {
            throw new RegraNegocioException("Selecione uma via de administração válida");
        }
        validarTamanho(form.dose(), 180, "dose");
        validarTamanho(form.viaAdministracao(), 100, "via de administração");
        validarTamanho(form.frequencia(), 180, "frequência");
        validarTamanho(form.duracao(), 180, "duração");
        validarTamanho(form.orientacoes(), 2000, "orientações");

        String resumo = "Medicamento: " + medicamento.nome()
                + " (" + medicamento.principioAtivo() + "), " + medicamento.apresentacao()
                + ". Dose: " + form.dose().trim()
                + ". Via: " + form.viaAdministracao().trim()
                + ". Frequência: " + form.frequencia().trim()
                + ". Duração: " + form.duracao().trim()
                + (form.orientacoes() == null || form.orientacoes().isBlank()
                ? "" : ". Orientações: " + form.orientacoes().trim())
                + (Boolean.TRUE.equals(form.farmaciaPopular()) ? ". Orientação Farmácia Popular solicitada." : "");

        Receita receita = new Receita();
        receita.setDossie(dossie);
        receita.setMedico(medico);
        receita.setPrescricao(resumo);
        receita.setNomeMedicamento(medicamento.nome());
        receita.setApresentacao(medicamento.apresentacao());
        receita.setDose(form.dose().trim());
        receita.setViaAdministracao(form.viaAdministracao().trim());
        receita.setFrequencia(form.frequencia().trim());
        receita.setDuracao(form.duracao().trim());
        receita.setOrientacoes(form.orientacoes() == null || form.orientacoes().isBlank()
                ? null : form.orientacoes().trim());
        receita.setFarmaciaPopular(Boolean.TRUE.equals(form.farmaciaPopular()));
        receita.setAssinaturaDigital(assinar(resumo, medico));
        receita.setAssinadaEm(OffsetDateTime.now());
        receita.setStatus(StatusReceita.ASSINADA);
        receitaRepository.save(receita);
        return resumir(receita);
    }

    @Transactional(readOnly = true)
    public List<ReceitaResumo> listar(Long dossieId, AuthService.Sessao sessao) {
        Dossie dossie = dossieRepository.findById(dossieId)
                .orElseThrow(() -> new RegraNegocioException("Atendimento não encontrado"));
        if (sessao.usuario().getPerfil() == Perfil.ACS
                && !dossie.getPaciente().getMicroarea().getId().equals(sessao.microareaId())) {
            throw new AcessoNegadoException("O ACS só pode acessar receitas da própria microárea");
        }
        return receitaRepository.findByDossieIdOrderByAssinadaEmDesc(dossieId).stream()
                .map(this::resumir)
                .toList();
    }

    private ReceitaResumo resumir(Receita receita) {
        return new ReceitaResumo(receita.getId(), receita.getNomeMedicamento(), receita.getApresentacao(),
                receita.getPrescricao(),
                receita.getDose(), receita.getViaAdministracao(), receita.getFrequencia(), receita.getDuracao(),
                receita.getOrientacoes(), receita.getFarmaciaPopular(), receita.getAssinadaEm(),
                receita.getAssinaturaDigital());
    }

    private void validarCampo(String valor, String mensagem) {
        if (valor == null || valor.isBlank()) throw new RegraNegocioException(mensagem);
    }

    private void validarTamanho(String valor, int maximo, String nomeCampo) {
        if (valor != null && valor.length() > maximo) {
            throw new RegraNegocioException("O campo " + nomeCampo + " deve ter até " + maximo + " caracteres");
        }
    }

    private String assinar(String texto, Medico medico) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            return "ICP-Brasil (simulada) | CRM " + medico.getCrm() + "/" + medico.getUfCrm()
                    + " | " + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String normalizarBusca(String valor) {
        if (valor == null) return "";
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}
