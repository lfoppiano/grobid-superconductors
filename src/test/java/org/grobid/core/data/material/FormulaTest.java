package org.grobid.core.data.material;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

class FormulaTest {

    @Test
    void testEquals_onlyRawValue_shouldReturnTrue() {
        Formula formula1 = new Formula("La 3 A 1 R 2");
        Formula formula2 = new Formula("La 3 A 1 R 2");

        Assertions.assertEquals(formula1, formula2);

    }

    @Test
    void testEquals_onlyRawValue_shouldReturnFalse() {
        Formula formula1 = new Formula("La 3 A 1 R 2");
        Formula formula2 = new Formula("La 3 A 2 R 2");

        Assertions.assertEquals(formula1, formula2);

    }

    @Test
    void testEquals_onlyComposition_shouldReturnTrue() {
        Formula formula1 = new Formula();
        formula1.setFormulaComposition(Map.of("La", "2", "Fe", "3"));
        Formula formula2 = new Formula();
        formula2.setFormulaComposition(Map.of("Fe", "3", "La", "2"));

        Assertions.assertEquals(formula1, formula2);

    }

    @Test
    void testEquals_onlyComposition_shouldReturnFalse() {
        Formula formula1 = new Formula();
        formula1.setFormulaComposition(Map.of("La", "2", "Fe", "3"));
        Formula formula2 = new Formula();
        formula2.setFormulaComposition(Map.of("La", "3", "Fe", "3"));

        Assertions.assertNotEquals(formula1, formula2);

    }
}