package br.mackenzie.ruralmed.regras;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ValidadoresDocumentosTest {

    @Test
    void validaCpfPelosDigitosVerificadores() {
        assertTrue(ValidadoresDocumentos.cpfValido("52998224725"));
        assertFalse(ValidadoresDocumentos.cpfValido("12345678901"));
        assertFalse(ValidadoresDocumentos.cpfValido("11111111111"));
    }

    @Test
    void validaCnpjPelosDigitosVerificadores() {
        assertTrue(ValidadoresDocumentos.cnpjValido("11222333000181"));
        assertFalse(ValidadoresDocumentos.cnpjValido("12345678000190"));
        assertFalse(ValidadoresDocumentos.cnpjValido("11111111111111"));
    }
}
