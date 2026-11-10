package br.mackenzie.ruralmed.regras;

import static org.junit.jupiter.api.Assertions.*;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RegrasDeNegocioTest {

    @Nested
    @DisplayName("Limites exatos do escore")
    class Limites {
        @ParameterizedTest
        @CsvSource({"37.7,0", "37.8,8", "38.9,8", "39.0,15"})
        void deveAplicarFaixaExataDeFebre(double temperatura, int pontos) {
            var dados = new DadosTriagem("Coceira", temperatura, 120, 80, 80, 98, 1, List.of());
            assertEquals(pontos, RegrasDeNegocio.calcularEscore(dados, 30, null).pontos());
        }

        @ParameterizedTest
        @CsvSource({"89,25,true", "90,12,false", "94,12,false", "95,0,false"})
        void deveDiferenciarSaturacaoBaixaECritica(int saturacao, int pontos, boolean prioritario) {
            var dados = new DadosTriagem("Coceira", 36.5, 120, 80, 80, saturacao, 1, List.of());
            var escore = RegrasDeNegocio.calcularEscore(dados, 30, null);
            assertEquals(pontos, escore.pontos());
            assertEquals(prioritario, escore.prioritario());
        }

        @ParameterizedTest
        @CsvSource({"49,15", "50,0", "120,0", "121,15"})
        void deveAplicarLimitesDaFrequenciaCardiaca(int frequencia, int pontos) {
            var dados = new DadosTriagem("Coceira", 36.5, 120, 80, frequencia, 98, 1, List.of());
            assertEquals(pontos, RegrasDeNegocio.calcularEscore(dados, 30, null).pontos());
        }

        @ParameterizedTest
        @CsvSource({"4,0", "5,6", "7,6", "8,12"})
        void deveAplicarFaixasDeDor(int dor, int pontos) {
            var dados = new DadosTriagem("Coceira", 36.5, 120, 80, 80, 98, dor, List.of());
            assertEquals(pontos, RegrasDeNegocio.calcularEscore(dados, 30, null).pontos());
        }

        @ParameterizedTest
        @CsvSource({"64,0", "65,10"})
        void deveAplicarLimiteDeIdade(int idade, int pontos) {
            var dados = new DadosTriagem("Coceira", 36.5, 120, 80, 80, 98, 1, List.of());
            assertEquals(pontos, RegrasDeNegocio.calcularEscore(dados, idade, null).pontos());
        }

        @ParameterizedTest
        @CsvSource({"1199,NO_PRAZO,241", "1200,NO_PRAZO,240", "1201,ALERTA,239",
                "1440,ALERTA,0", "1441,ATRASADO,-1"})
        void deveCalcularPrazoNasFronteiras(int minutosPassados, String situacao, long restantes) {
            OffsetDateTime entrada = OffsetDateTime.parse("2026-11-03T08:00:00-03:00");
            var prazo = RegrasDeNegocio.calcularPrazo(true, entrada, entrada.plusMinutes(minutosPassados));
            assertEquals(situacao, prazo.situacao());
            assertEquals(restantes, prazo.minutosRestantes());
        }
    }

    @Test
    void casoLeveTemEscoreBaixoENaoEPrioritario() {
        DadosTriagem leve = new DadosTriagem("coceira no braço", 36.6, 120, 80, 78, 98, 2, List.of());
        RegrasDeNegocio.Escore escore = RegrasDeNegocio.calcularEscore(leve, 40, "mancha vermelha");
        assertEquals(0, escore.pontos());
        assertFalse(escore.prioritario());
    }

    @Test
    void casoGraveEPrioritarioEMostraOsMotivos() {
        DadosTriagem grave = new DadosTriagem("Dor no peito", 37.0, 185, 112, 125, 91, 9, List.of("diabetes"));
        RegrasDeNegocio.Escore escore = RegrasDeNegocio.calcularEscore(grave, 70, "falta de ar ao caminhar");
        assertTrue(escore.prioritario());
        assertTrue(escore.pontos() >= RegrasDeNegocio.LIMITE_PRIORITARIO);
        assertTrue(escore.fatores().contains("relato: falta de ar (+15)"));
    }

    @Test
    void saturacaoCriticaTornaPrioritarioMesmoComPoucosPontos() {
        DadosTriagem d = new DadosTriagem("cansaço", 36.5, 120, 80, 80, 88, 1, List.of());
        assertTrue(RegrasDeNegocio.calcularEscore(d, 30, null).prioritario());
    }

    @Test
    void rn03RecusaTriagemIncompleta() {
        DadosTriagem incompleta = new DadosTriagem("dor de cabeça", null, 120, 80, null, 98, 3, List.of());
        RegraNegocioException erro = assertThrows(RegraNegocioException.class,
                () -> RegrasDeNegocio.validarTriagem(incompleta));
        assertTrue(erro.getMessage().contains("temperatura"));
    }

    @Test
    void rn06PrazoDeVinteEQuatroHoras() {
        OffsetDateTime entrada = OffsetDateTime.parse("2026-11-03T08:00:00-03:00");
        assertEquals("SEM_PRAZO", RegrasDeNegocio.calcularPrazo(false, entrada, entrada).situacao());
        assertEquals("NO_PRAZO", RegrasDeNegocio.calcularPrazo(true, entrada, entrada.plusHours(10)).situacao());
        assertEquals("ALERTA", RegrasDeNegocio.calcularPrazo(true, entrada, entrada.plusHours(21)).situacao());
        assertEquals("ATRASADO", RegrasDeNegocio.calcularPrazo(true, entrada, entrada.plusHours(25)).situacao());
    }
}
