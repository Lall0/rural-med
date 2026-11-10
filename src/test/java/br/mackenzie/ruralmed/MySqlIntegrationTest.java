package br.mackenzie.ruralmed;

import br.mackenzie.ruralmed.model.Dossie;
import br.mackenzie.ruralmed.model.StatusDossie;
import br.mackenzie.ruralmed.model.StatusLaudo;
import br.mackenzie.ruralmed.model.StatusReceita;
import br.mackenzie.ruralmed.regras.DadosTriagem;
import br.mackenzie.ruralmed.repository.DossieRepository;
import br.mackenzie.ruralmed.repository.LaudoRepository;
import br.mackenzie.ruralmed.repository.PacienteRepository;
import br.mackenzie.ruralmed.service.AuthService;
import br.mackenzie.ruralmed.service.LaudoService;
import br.mackenzie.ruralmed.service.TriagemService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnabledIfEnvironmentVariable(named = "MYSQL_TEST_URL", matches = ".+")
@SpringBootTest(properties = {
        "spring.datasource.url=${MYSQL_TEST_URL}",
        "spring.datasource.username=${MYSQL_TEST_USERNAME:root}",
        "spring.datasource.password=${MYSQL_TEST_PASSWORD:}"
})
@AutoConfigureMockMvc
@Transactional
@DisplayName("API, Flyway e persistencia em MySQL isolado")
class MySqlIntegrationTest {

    @DynamicPropertySource
    static void exigirBancoExclusivoDeTeste(DynamicPropertyRegistry registry) {
        String url = System.getenv("MYSQL_TEST_URL");
        if (url == null || !url.matches("jdbc:mysql://[^/]+/ruralmed_test_[a-zA-Z0-9_]+(?:\\?.*)?")) {
            throw new IllegalArgumentException("MYSQL_TEST_URL deve apontar para um banco ruralmed_test_* exclusivo");
        }
        registry.add("spring.datasource.url", () -> url);
    }

    @Autowired private JdbcTemplate jdbc;
    @Autowired private DossieRepository dossies;
    @Autowired private PacienteRepository pacientes;
    @Autowired private LaudoRepository laudos;
    @Autowired private AuthService auth;
    @Autowired private TriagemService triagem;
    @Autowired private LaudoService laudoService;
    @Autowired private EntityManager entities;
    @Autowired private ObjectMapper json;
    @Autowired private MockMvc http;

    private String login(String email) throws Exception {
        var resposta = http.perform(post("/api/login")
                        .contentType("application/json")
                        .content(json.writeValueAsString(java.util.Map.of(
                                "login", email, "senha", "ruralmed123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.senhaHash").doesNotExist())
                .andReturn().getResponse();
        return json.readTree(resposta.getContentAsString()).get("token").asText();
    }

    @ParameterizedTest
    @CsvSource({
            "acs@ruralmed.dev, ACS",
            "acs2@ruralmed.dev, ACS",
            "ubs@ruralmed.dev, UBS",
            "cardio@ruralmed.dev, MEDICO",
            "dermato@ruralmed.dev, MEDICO",
            "gestor@ruralmed.dev, GESTOR",
            "100.000.000-03, ACS"
    })
    void deveAutenticarContasDeDemonstracaoPorEmailOuCpf(String identificador, String perfil) throws Exception {
        String token = login(identificador);
        http.perform(get("/api/especialidades").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
        assertEquals(perfil, auth.exigir(token).usuario().getPerfil().name());
    }

    @ParameterizedTest
    @CsvSource({
            "acs@ruralmed.dev, /api/usuarios",
            "acs@ruralmed.dev, /api/fila",
            "ubs@ruralmed.dev, /api/fila",
            "cardio@ruralmed.dev, /api/usuarios",
            "cardio@ruralmed.dev, /api/pacientes",
            "gestor@ruralmed.dev, /api/dossies",
            "gestor@ruralmed.dev, /api/pacientes",
            "gestor@ruralmed.dev, /api/usuarios"
    })
    void deveNegarOperacoesDeOutroPerfil(String email, String endpoint) throws Exception {
        http.perform(get(endpoint).header("Authorization", login(email)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value("Seu perfil não tem acesso a esta funcionalidade"));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "ubs@ruralmed.dev | /api/triagens/sincronizar | []",
            "gestor@ruralmed.dev | /api/triagens/sincronizar | []",
            "acs@ruralmed.dev | /api/dossies/1/encaminhar | {}",
            "cardio@ruralmed.dev | /api/dossies/1/encaminhar | {}",
            "acs@ruralmed.dev | /api/dossies/1/laudo | {}",
            "ubs@ruralmed.dev | /api/dossies/1/laudo | {}",
            "gestor@ruralmed.dev | /api/usuarios | {}",
            "acs@ruralmed.dev | /api/especialidades | {\"nome\":\"Teste\"}",
            "cardio@ruralmed.dev | /api/clinicas | {\"nome\":\"Teste\",\"cnpj\":\"00000000000000\"}",
            "gestor@ruralmed.dev | /api/pacientes | {\"nome\":\"Teste\",\"dataNascimento\":\"1990-01-01\"}"
    })
    void deveNegarGravacaoDeOutroPerfil(String email, String endpoint, String corpo) throws Exception {
        http.perform(post(endpoint).header("Authorization", login(email))
                        .contentType("application/json").content(corpo))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value("Seu perfil não tem acesso a esta funcionalidade"));
    }

    @Test
    void deveExigirLoginEInvalidarTokenAposSair() throws Exception {
        http.perform(get("/api/pacientes"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value("Faça login para continuar"));
        String token = login("acs@ruralmed.dev");
        http.perform(post("/api/sair").header("Authorization", token)).andExpect(status().isOk());
        http.perform(get("/api/pacientes").header("Authorization", token))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveRecusarSenhaIncorretaSemCriarSessao() throws Exception {
        http.perform(post("/api/login").contentType("application/json")
                        .content("{\"login\":\"acs@ruralmed.dev\",\"senha\":\"incorreta\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("E-mail/CPF ou senha inválidos"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"login\":\"acs@ruralmed.dev\"}", "{\"senha\":\"ruralmed123\"}",
            "{\"login\":\" \",\"senha\":\"ruralmed123\"}"})
    void deveRecusarLoginIncompletoComErroTratado(String corpo) throws Exception {
        http.perform(post("/api/login").contentType("application/json").content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("E-mail/CPF ou senha inválidos"));
    }

    @Test
    void deveCalcularRelatoriosERepassesPelaApi() throws Exception {
        String gestor = login("gestor@ruralmed.dev");
        var coletas = dossies.findAll().stream().map(Dossie::getColetadoEm).toList();
        String inicio = coletas.stream().min(OffsetDateTime::compareTo).orElseThrow()
                .minusDays(1).toLocalDate().toString();
        String fim = coletas.stream().max(OffsetDateTime::compareTo).orElseThrow()
                .plusDays(1).toLocalDate().toString();
        http.perform(get("/api/relatorios/atendimentos").header("Authorization", gestor)
                        .param("inicio", inicio).param("fim", fim))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.triagens").value(3))
                .andExpect(jsonPath("$.prioritarias").value(1))
                .andExpect(jsonPath("$.laudosEmitidos").value(1))
                .andExpect(jsonPath("$.viagensTfdEvitadas").value(1));
        var emissao = laudos.findAll().get(0).getEmitidoEm()
                .atZoneSameInstant(java.time.ZoneId.of("America/Sao_Paulo"));
        http.perform(get("/api/relatorios/repasses").header("Authorization", gestor)
                        .param("ano", String.valueOf(emissao.getYear()))
                        .param("mes", String.valueOf(emissao.getMonthValue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].especialidade").value("Dermatologia"))
                .andExpect(jsonPath("$[0].laudos").value(1))
                .andExpect(jsonPath("$[0].total").value(120.0));
    }

    @Test
    void deveIsolarPacientesEntreMicroareas() throws Exception {
        String token = login("acs2@ruralmed.dev");
        Long pacienteDeOutraArea = pacientes.findAll().stream()
                .filter(p -> "52998224725".equals(p.getCpf())).findFirst().orElseThrow().getId();
        http.perform(get("/api/pacientes").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Benedito Pereira"));
        http.perform(get("/api/pacientes/" + pacienteDeOutraArea).header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("RN02: o ACS só acessa pacientes da sua microárea"));
    }

    @Test
    void deveSepararFilasPorEspecialidade() throws Exception {
        http.perform(get("/api/fila").header("Authorization", login("cardio@ruralmed.dev")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].especialidade.nome").value("Cardiologia"));
        http.perform(get("/api/fila").header("Authorization", login("dermato@ruralmed.dev")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void deveExecutarFluxoCompletoPelaApi() throws Exception {
        Long pacienteId = pacientes.findAll().stream()
                .filter(p -> "52998224725".equals(p.getCpf())).findFirst().orElseThrow().getId();
        UUID idLocal = UUID.randomUUID();
        var recebida = new TriagemService.TriagemRecebida(idLocal, pacienteId, OffsetDateTime.now(),
                new DadosTriagem("Dor no peito", 37.0, 180, 110, 125, 91, 9, List.of()), "Falta de ar");
        String tokenAcs = login("acs@ruralmed.dev");
        String lote = json.writeValueAsString(List.of(recebida));
        http.perform(post("/api/triagens/sincronizar").header("Authorization", tokenAcs)
                        .contentType("application/json").content(lote))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].situacao").value("CRIADO"));
        http.perform(post("/api/triagens/sincronizar").header("Authorization", tokenAcs)
                        .contentType("application/json").content(lote))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].situacao").value("JA_EXISTIA"));
        Dossie dossie = dossies.findByIdLocal(idLocal).orElseThrow();
        String tokenMedico = login("cardio@ruralmed.dev");
        Long especialidade = auth.exigir(tokenMedico).especialidadeId();
        http.perform(post("/api/dossies/" + dossie.getId() + "/encaminhar")
                        .header("Authorization", login("ubs@ruralmed.dev"))
                        .contentType("application/json")
                        .content("{\"especialidadeId\":" + especialidade + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENCAMINHADO"))
                .andExpect(jsonPath("$.prioritario").value(true));
        String parecer = "{\"parecer\":\"Parecer de teste\",\"cidCodigo\":\"I20.9\",\"prescricao\":\"Receita de teste\"}";
        http.perform(post("/api/dossies/" + dossie.getId() + "/laudo")
                        .header("Authorization", tokenMedico).contentType("application/json").content(parecer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EMITIDO"))
                .andExpect(jsonPath("$.receitas[0].status").value("ASSINADA"));
        entities.flush();
        entities.clear();
        assertEquals(StatusDossie.LAUDO_EMITIDO, dossies.findById(dossie.getId()).orElseThrow().getStatus());
        http.perform(post("/api/dossies/" + dossie.getId() + "/laudo")
                        .header("Authorization", tokenMedico).contentType("application/json").content(parecer))
                .andExpect(status().isBadRequest());
    }

    @Test
    void apiMantemJsonDeTriagemComoObjetoEFatoresComoLista() throws Exception {
        http.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
        var resposta = http.perform(post("/api/login")
                        .contentType("application/json")
                        .content("{\"login\":\"acs@ruralmed.dev\",\"senha\":\"ruralmed123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.senhaHash").doesNotExist())
                .andReturn().getResponse();
        String token = json.readTree(resposta.getContentAsString()).get("token").asText();
        http.perform(get("/api/dossies").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idLocal").isString())
                .andExpect(jsonPath("$[0].dadosTriagem").isMap())
                .andExpect(jsonPath("$[0].fatoresEscore").isArray());
    }

    @Test
    void carregaMigracoesEDadosDeDemonstracao() throws Exception {
        assertEquals(3, jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1", Integer.class));
        assertEquals(5, pacientes.count());
        assertEquals(3, dossies.count());
        for (Dossie dossie : dossies.findAll()) {
            assertNotNull(dossie.getIdLocal());
            assertTrue(json.readTree(dossie.getDadosTriagem()).isObject());
            assertTrue(json.readTree(dossie.getFatoresEscore()).isArray());
        }
        assertNotNull(auth.login("cardio@ruralmed.dev", "ruralmed123"));
    }

    @Test
    void persisteTriagemSemDuplicarEEmiteLaudoComReceita() throws Exception {
        var sessao = auth.login("acs@ruralmed.dev", "ruralmed123");
        var paciente = pacientes.findAll().stream()
                .filter(p -> "52998224725".equals(p.getCpf())).findFirst().orElseThrow();
        UUID idLocal = UUID.randomUUID();
        OffsetDateTime coleta = OffsetDateTime.parse("2026-11-03T08:30:00.123456-03:00");
        var dados = new DadosTriagem("Dor no peito", 37.1, 182, 110, 118, 93, 8,
                List.of("hipertensao"));
        var recebida = new TriagemService.TriagemRecebida(idLocal, paciente.getId(), coleta,
                dados, "Falta de ar");

        assertEquals("CRIADO", triagem.sincronizar(List.of(recebida), sessao).get(0).situacao());
        entities.flush();
        entities.clear();

        Dossie salvo = dossies.findByIdLocal(idLocal).orElseThrow();
        assertEquals(coleta.toInstant(), salvo.getColetadoEm().toInstant());
        assertEquals(json.valueToTree(dados), json.readTree(salvo.getDadosTriagem()));
        assertTrue(json.readTree(salvo.getFatoresEscore()).isArray());
        assertEquals(StatusDossie.SINCRONIZADO, salvo.getStatus());
        assertEquals("JA_EXISTIA", triagem.sincronizar(List.of(recebida), sessao).get(0).situacao());
        assertEquals(4, dossies.count());

        var medico = auth.login("cardio@ruralmed.dev", "ruralmed123");
        triagem.encaminhar(salvo.getId(), medico.especialidadeId());
        entities.flush();
        entities.clear();
        assertEquals("NO_PRAZO", dossies.findById(salvo.getId()).orElseThrow().getPrazo().situacao());
        assertTrue(triagem.fila(medico).stream().anyMatch(d -> d.getId().equals(salvo.getId())));

        var emitido = laudoService.emitir(salvo.getId(),
                new LaudoService.LaudoForm("Parecer de teste", "I20.9", "Prescricao de teste"), medico);
        entities.flush();
        entities.clear();

        var recarregado = laudos.findById(emitido.getId()).orElseThrow();
        assertEquals(StatusLaudo.EMITIDO, recarregado.getStatus());
        assertEquals(StatusReceita.ASSINADA, recarregado.getReceitas().get(0).getStatus());
        assertEquals(StatusDossie.LAUDO_EMITIDO, dossies.findById(salvo.getId()).orElseThrow().getStatus());
    }
}
