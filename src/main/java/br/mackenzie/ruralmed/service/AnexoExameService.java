package br.mackenzie.ruralmed.service;

import br.mackenzie.ruralmed.model.*;
import br.mackenzie.ruralmed.regras.AcessoNegadoException;
import br.mackenzie.ruralmed.regras.RegraNegocioException;
import br.mackenzie.ruralmed.repository.AnexoExameRepository;
import br.mackenzie.ruralmed.repository.DossieRepository;
import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AnexoExameService {

    private static final long TAMANHO_MAXIMO = 10L * 1024 * 1024;
    private static final List<String> TIPOS_PERMITIDOS =
            List.of("application/pdf", "image/jpeg", "image/png");

    public record AnexoResumo(Long id, String nomeArquivo, String tipoConteudo, Long tamanho, String criadoEm) {
    }

    private final AnexoExameRepository anexoRepository;
    private final DossieRepository dossieRepository;

    public AnexoExameService(AnexoExameRepository anexoRepository, DossieRepository dossieRepository) {
        this.anexoRepository = anexoRepository;
        this.dossieRepository = dossieRepository;
    }

    @Transactional(readOnly = true)
    public List<AnexoResumo> listar(Long dossieId, AuthService.Sessao sessao) {
        Dossie dossie = buscarDossie(dossieId, sessao);
        return anexoRepository.findByDossieIdOrderByCriadoEmDesc(dossie.getId()).stream()
                .map(a -> new AnexoResumo(a.getId(), a.getNomeArquivo(), a.getTipoConteudo(),
                        a.getTamanho(), a.getCriadoEm().toString()))
                .toList();
    }

    @Transactional
    public AnexoExame salvar(Long dossieId, MultipartFile arquivo, AuthService.Sessao sessao) {
        Dossie dossie = buscarDossie(dossieId, sessao);
        if (arquivo == null || arquivo.isEmpty()) {
            throw new RegraNegocioException("Selecione um arquivo de exame");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO) {
            throw new RegraNegocioException("O arquivo deve ter no máximo 10 MB");
        }
        String tipo = arquivo.getContentType();
        if (tipo == null || !TIPOS_PERMITIDOS.contains(tipo.toLowerCase())) {
            throw new RegraNegocioException("Formato não permitido. Envie PDF, JPG ou PNG");
        }
        try {
            AnexoExame anexo = new AnexoExame();
            anexo.setDossie(dossie);
            anexo.setNomeArquivo(nomeSeguro(arquivo.getOriginalFilename()));
            anexo.setTipoConteudo(tipo.toLowerCase());
            anexo.setTamanho(arquivo.getSize());
            anexo.setConteudo(arquivo.getBytes());
            return anexoRepository.save(anexo);
        } catch (IOException e) {
            throw new RegraNegocioException("Não foi possível ler o arquivo enviado");
        }
    }

    @Transactional(readOnly = true)
    public AnexoExame baixar(Long anexoId, AuthService.Sessao sessao) {
        AnexoExame anexo = anexoRepository.findById(anexoId)
                .orElseThrow(() -> new RegraNegocioException("Anexo não encontrado"));
        buscarDossie(anexo.getDossie().getId(), sessao);
        return anexo;
    }

    private Dossie buscarDossie(Long dossieId, AuthService.Sessao sessao) {
        Dossie dossie = dossieRepository.findById(dossieId)
                .orElseThrow(() -> new RegraNegocioException("Dossiê não encontrado"));
        Perfil perfil = sessao.usuario().getPerfil();
        if (perfil == Perfil.ACS
                && !dossie.getPaciente().getMicroarea().getId().equals(sessao.microareaId())) {
            throw new AcessoNegadoException("O ACS só pode acessar exames da própria microárea");
        }
        return dossie;
    }

    private String nomeSeguro(String nome) {
        if (nome == null || nome.isBlank()) return "exame";
        String seguro = nome.replaceAll("[\\\\/\\r\\n\\t]", "_").trim();
        return seguro.length() > 180 ? seguro.substring(seguro.length() - 180) : seguro;
    }
}
