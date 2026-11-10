package br.mackenzie.ruralmed.regras;

public final class ValidadoresDocumentos {

    private ValidadoresDocumentos() {
    }

    public static boolean cpfValido(String cpf) {
        if (cpf == null || !cpf.matches("\\d{11}") || cpf.matches("(\\d)\\1{10}")) return false;
        int primeiro = calcularDigito(cpf, 9);
        int segundo = calcularDigito(cpf, 10);
        return primeiro == cpf.charAt(9) - '0' && segundo == cpf.charAt(10) - '0';
    }

    public static boolean cnpjValido(String cnpj) {
        if (cnpj == null || !cnpj.matches("\\d{14}") || cnpj.matches("(\\d)\\1{13}")) return false;
        int primeiro = calcularDigitoCnpj(cnpj, 12);
        int segundo = calcularDigitoCnpj(cnpj, 13);
        return primeiro == cnpj.charAt(12) - '0' && segundo == cnpj.charAt(13) - '0';
    }

    private static int calcularDigito(String numero, int tamanho) {
        int soma = 0;
        for (int i = 0; i < tamanho; i++) soma += (numero.charAt(i) - '0') * (tamanho + 1 - i);
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    private static int calcularDigitoCnpj(String cnpj, int tamanho) {
        int[] pesos = tamanho == 12
                ? new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2}
                : new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int soma = 0;
        for (int i = 0; i < tamanho; i++) soma += (cnpj.charAt(i) - '0') * pesos[i];
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
