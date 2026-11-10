package br.mackenzie.ruralmed.regras;

import java.util.List;

public record DadosTriagem(
        String queixaPrincipal,
        Double temperatura,
        Integer pressaoSistolica,
        Integer pressaoDiastolica,
        Integer frequenciaCardiaca,
        Integer saturacaoO2,
        Integer dor,
        List<String> comorbidades) {
}
