package br.mackenzie.ruralmed.regras;

import br.mackenzie.ruralmed.model.Medico;
import br.mackenzie.ruralmed.model.Paciente;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public final class RegrasDeNegocio {

    private RegrasDeNegocio() {
    }

    public static void validarTriagem(DadosTriagem d) {
        List<String> faltando = new ArrayList<>();
        if (d == null) {
            throw new RegraNegocioException("RN03: a triagem está vazia");
        }
        if (d.queixaPrincipal() == null || d.queixaPrincipal().isBlank()) faltando.add("queixa principal");
        if (d.temperatura() == null) faltando.add("temperatura");
        if (d.pressaoSistolica() == null) faltando.add("pressão sistólica");
        if (d.pressaoDiastolica() == null) faltando.add("pressão diastólica");
        if (d.frequenciaCardiaca() == null) faltando.add("frequência cardíaca");
        if (d.saturacaoO2() == null) faltando.add("saturação de O2");
        if (d.dor() == null) faltando.add("dor");

        if (!faltando.isEmpty()) {
            throw new RegraNegocioException("RN03: preencha os campos obrigatórios: " + String.join(", ", faltando));
        }
    }

    public record Escore(int pontos, boolean prioritario, List<String> fatores) {
    }

    public static final int LIMITE_PRIORITARIO = 50;

    /** Sinais de alerta tornam o caso prioritario mesmo abaixo de 50 pontos; nao e diagnostico. */
    public static Escore calcularEscore(DadosTriagem d, int idade, String relato) {
        List<String> fatores = new ArrayList<>();
        int pontos = 0;
        boolean sinalDeAlerta = false;

        if (idade >= 65) {
            pontos += 10; fatores.add("idade 65 anos ou mais (+10)");
        }

        if (d.temperatura() >= 39) {
            pontos += 15; fatores.add("febre alta (+15)");
        } else if (d.temperatura() >= 37.8) {
            pontos += 8; fatores.add("febre (+8)");
        }

        if (d.saturacaoO2() < 90) {
            pontos += 25; fatores.add("saturação de O2 crítica (+25)"); sinalDeAlerta = true;
        } else if (d.saturacaoO2() <= 94) {
            pontos += 12; fatores.add("saturação de O2 baixa (+12)");
        }

        if (d.frequenciaCardiaca() > 120 || d.frequenciaCardiaca() < 50) {
            pontos += 15; fatores.add("frequência cardíaca alterada (+15)");
        }

        if (d.pressaoSistolica() >= 180 || d.pressaoDiastolica() >= 110) {
            pontos += 20; fatores.add("pressão muito alta (+20)"); sinalDeAlerta = true;
        } else if (d.pressaoSistolica() < 90) {
            pontos += 20; fatores.add("pressão muito baixa (+20)"); sinalDeAlerta = true;
        }

        if (d.dor() >= 8) {
            pontos += 12; fatores.add("dor intensa (+12)");
        } else if (d.dor() >= 5) {
            pontos += 6; fatores.add("dor moderada (+6)");
        }

        if (d.comorbidades() != null) {
            for (String doenca : d.comorbidades()) {
                pontos += 5; fatores.add("comorbidade: " + doenca + " (+5)");
            }
        }

        String texto = ((relato == null ? "" : relato) + " " + d.queixaPrincipal()).toLowerCase();
        if (texto.contains("dor no peito")) {
            pontos += 15; fatores.add("relato: dor no peito (+15)"); sinalDeAlerta = true;
        }
        if (texto.contains("falta de ar")) {
            pontos += 15; fatores.add("relato: falta de ar (+15)"); sinalDeAlerta = true;
        }
        if (texto.contains("desmai")) {
            pontos += 12; fatores.add("relato: desmaio (+12)"); sinalDeAlerta = true;
        }
        if (texto.contains("sangra")) {
            pontos += 10; fatores.add("relato: sangramento (+10)");
        }

        boolean prioritario = pontos >= LIMITE_PRIORITARIO || sinalDeAlerta;
        return new Escore(pontos, prioritario, fatores);
    }

    public static void verificarMicroarea(Long microareaDoAcs, Paciente paciente) {
        if (!paciente.getMicroarea().getId().equals(microareaDoAcs)) {
            throw new RegraNegocioException("RN02: o ACS só acessa pacientes da sua microárea");
        }
    }

    public static void verificarCrmAtivo(Medico medico) {
        if (!Boolean.TRUE.equals(medico.getCrmAtivo())) {
            throw new RegraNegocioException("RN04: o médico precisa ter registro ativo no CRM");
        }
    }

    /** situacao: SEM_PRAZO, NO_PRAZO, ALERTA (faltam menos de 4 h) ou ATRASADO. */
    public record Prazo(String situacao, Long minutosRestantes) {
    }

    public static Prazo calcularPrazo(boolean prioritario, OffsetDateTime encaminhadoEm, OffsetDateTime agora) {
        if (!prioritario || encaminhadoEm == null) {
            return new Prazo("SEM_PRAZO", null);
        }
        OffsetDateTime limite = encaminhadoEm.plusHours(24);
        long minutos = Duration.between(agora, limite).toMinutes();

        if (minutos < 0) return new Prazo("ATRASADO", minutos);
        if (minutos < 4 * 60) return new Prazo("ALERTA", minutos);
        return new Prazo("NO_PRAZO", minutos);
    }

    public static void verificarConsentimento(Paciente paciente) {
        if (paciente.getConsentimentoEm() == null) {
            throw new RegraNegocioException("O paciente ainda não consentiu com o atendimento por telessaúde");
        }
    }
}
