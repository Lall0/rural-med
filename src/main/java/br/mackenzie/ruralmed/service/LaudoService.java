package br.mackenzie.ruralmed.service;

import br.mackenzie.ruralmed.model.*;
import br.mackenzie.ruralmed.regras.RegraNegocioException;
import br.mackenzie.ruralmed.regras.RegrasDeNegocio;
import br.mackenzie.ruralmed.repository.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LaudoService {

    public record LaudoForm(String parecer, String cidCodigo, String prescricao, Boolean farmaciaPopular) {
    }

    private final LaudoRepository laudoRepository;
    private final ReceitaRepository receitaRepository;
    private final DossieRepository dossieRepository;
    private final MedicoRepository medicoRepository;
    private final CidRepository cidRepository;

    public LaudoService(LaudoRepository laudoRepository, ReceitaRepository receitaRepository,
                        DossieRepository dossieRepository, MedicoRepository medicoRepository,
                        CidRepository cidRepository) {
        this.laudoRepository = laudoRepository;
        this.receitaRepository = receitaRepository;
        this.dossieRepository = dossieRepository;
        this.medicoRepository = medicoRepository;
        this.cidRepository = cidRepository;
    }

    @Transactional
    public Laudo emitir(Long dossieId, LaudoForm form, AuthService.Sessao sessao) {
        if (form == null) throw new RegraNegocioException("Informe os dados do laudo");
        Dossie dossie = dossieRepository.findById(dossieId)
                .orElseThrow(() -> new RegraNegocioException("Dossiê não encontrado"));
        if (dossie.getStatus() != StatusDossie.ENCAMINHADO) {
            throw new RegraNegocioException("Só é possível dar laudo a casos encaminhados");
        }
        if (form.parecer() == null || form.parecer().isBlank()) {
            throw new RegraNegocioException("Escreva o parecer");
        }
        String parecer = form.parecer().trim();
        if (parecer.length() > 10000) {
            throw new RegraNegocioException("O parecer deve ter até 10000 caracteres");
        }
        Medico medico = medicoRepository.findById(sessao.usuario().getId()).orElseThrow();
        RegrasDeNegocio.verificarCrmAtivo(medico); // RN04
        Cid cid = null;
        if (form.cidCodigo() != null && !form.cidCodigo().isBlank()) {
            String codigoCid = form.cidCodigo().trim().toUpperCase(java.util.Locale.ROOT);
            if (!codigoCid.matches("[A-Z][0-9]{2}(\\.[0-9A-Z]{1,2})?")) {
                throw new RegraNegocioException("Informe um código CID-10 válido");
            }
            cid = cidRepository.findById(codigoCid)
                    .orElseThrow(() -> new RegraNegocioException("Escolha um código CID válido"));
        }
        String prescricao = form.prescricao() == null ? null : form.prescricao().trim();
        if (prescricao != null && prescricao.length() > 2000) {
            throw new RegraNegocioException("A prescrição deve ter até 2000 caracteres");
        }

        Laudo laudo = new Laudo();
        laudo.setDossie(dossie);
        laudo.setMedico(medico);
        laudo.setCid(cid);
        laudo.setParecer(parecer);

        laudo.setAssinaturaDigital(assinar(parecer + "\nCID: " + (cid == null ? "" : cid.getCodigo()), medico));
        laudo.setStatus(StatusLaudo.EMITIDO);
        laudo.setEmitidoEm(OffsetDateTime.now());
        laudoRepository.save(laudo);

        if (prescricao != null && !prescricao.isBlank()) {
            Receita receita = new Receita();
            receita.setLaudo(laudo);
            receita.setDossie(dossie);
            receita.setMedico(medico);
            String textoPrescricao = prescricao
                    + (Boolean.TRUE.equals(form.farmaciaPopular()) ? "\nOrientação Farmácia Popular solicitada." : "");
            receita.setPrescricao(textoPrescricao);
            receita.setFarmaciaPopular(Boolean.TRUE.equals(form.farmaciaPopular()));
            receita.setAssinaturaDigital(assinar(textoPrescricao, medico));
            receita.setAssinadaEm(OffsetDateTime.now());
            receita.setStatus(StatusReceita.ASSINADA);
            receitaRepository.save(receita);
            laudo.getReceitas().add(receita);
        }

        dossie.setStatus(StatusDossie.LAUDO_EMITIDO);
        dossieRepository.save(dossie);
        return laudo;
    }

    public List<Laudo> meusLaudos(AuthService.Sessao sessao) {
        return laudoRepository.findByMedicoUsuarioIdOrderByIdDesc(sessao.usuario().getId());
    }

    /** Simulacao com SHA-256; nao substitui uma assinatura ICP-Brasil real. */
    private String assinar(String texto, Medico medico) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            return "ICP-Brasil (simulada) | CRM " + medico.getCrm() + "/" + medico.getUfCrm()
                    + " | " + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
