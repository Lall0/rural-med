package br.mackenzie.ruralmed.controller;

import br.mackenzie.ruralmed.model.AnexoExame;
import br.mackenzie.ruralmed.model.Perfil;
import br.mackenzie.ruralmed.service.AnexoExameService;
import br.mackenzie.ruralmed.service.AuthService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class AnexoExameController {

    private final AuthService authService;
    private final AnexoExameService anexoService;

    public AnexoExameController(AuthService authService, AnexoExameService anexoService) {
        this.authService = authService;
        this.anexoService = anexoService;
    }

    @GetMapping("/dossies/{dossieId}/exames")
    public List<AnexoExameService.AnexoResumo> listar(@PathVariable Long dossieId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        AuthService.Sessao sessao = authService.exigir(token, Perfil.ACS, Perfil.UBS, Perfil.MEDICO);
        return anexoService.listar(dossieId, sessao);
    }

    @PostMapping(value = "/dossies/{dossieId}/exames", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AnexoExameService.AnexoResumo salvar(@PathVariable Long dossieId,
            @RequestParam("arquivo") MultipartFile arquivo,
            @RequestHeader(value = "Authorization", required = false) String token) {
        AuthService.Sessao sessao = authService.exigir(token, Perfil.ACS, Perfil.UBS, Perfil.MEDICO);
        AnexoExame anexo = anexoService.salvar(dossieId, arquivo, sessao);
        return new AnexoExameService.AnexoResumo(anexo.getId(), anexo.getNomeArquivo(),
                anexo.getTipoConteudo(), anexo.getTamanho(), anexo.getCriadoEm().toString());
    }

    @GetMapping("/exames/{id}")
    public ResponseEntity<byte[]> baixar(@PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String token) {
        AuthService.Sessao sessao = authService.exigir(token, Perfil.ACS, Perfil.UBS, Perfil.MEDICO);
        AnexoExame anexo = anexoService.baixar(id, sessao);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(anexo.getTipoConteudo()))
                .contentLength(anexo.getTamanho())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(anexo.getNomeArquivo(), StandardCharsets.UTF_8).build().toString())
                .body(anexo.getConteudo());
    }
}
