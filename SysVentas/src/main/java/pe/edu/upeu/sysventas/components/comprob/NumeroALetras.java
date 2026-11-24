package pe.edu.upeu.sysventas.components.comprob;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Convierte un monto a letras para la boleta.
 * Ejemplo: 1250.50 -> "MIL DOSCIENTOS CINCUENTA Y 50/100 SOLES"
 */
public final class NumeroALetras {

    private static final String[] UNIDADES = {"", "UNO", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO", "NUEVE",
            "DIEZ", "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE", "DIECISEIS", "DIECISIETE", "DIECIOCHO", "DIECINUEVE",
            "VEINTE", "VEINTIUNO", "VEINTIDOS", "VEINTITRES", "VEINTICUATRO", "VEINTICINCO", "VEINTISEIS", "VEINTISIETE",
            "VEINTIOCHO", "VEINTINUEVE"};
    private static final String[] DECENAS = {"", "", "", "TREINTA", "CUARENTA", "CINCUENTA", "SESENTA", "SETENTA", "OCHENTA", "NOVENTA"};
    private static final String[] CENTENAS = {"", "CIENTO", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS", "QUINIENTOS",
            "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS"};

    private NumeroALetras() { }

    /** Monto en soles: "CIENTO VEINTE Y 50/100 SOLES" */
    public static String soles(BigDecimal monto) {
        if (monto == null) monto = BigDecimal.ZERO;
        BigDecimal m = monto.setScale(2, RoundingMode.HALF_UP);
        long entero = m.longValue();
        int centimos = m.remainder(BigDecimal.ONE).movePointRight(2).abs().intValue();
        return entero(entero) + " Y " + String.format("%02d", centimos) + "/100 SOLES";
    }

    /** Número entero en letras (hasta 999 999 999). */
    public static String entero(long n) {
        if (n == 0) return "CERO";
        int millones = (int) (n / 1_000_000);
        int miles = (int) ((n / 1000) % 1000);
        int resto = (int) (n % 1000);
        StringBuilder s = new StringBuilder();
        if (millones > 0) s.append(millones == 1 ? "UN MILLON" : apocope(menorDeMil(millones)) + " MILLONES");
        if (miles > 0) {
            if (s.length() > 0) s.append(' ');
            s.append(miles == 1 ? "MIL" : apocope(menorDeMil(miles)) + " MIL");
        }
        if (resto > 0) {
            if (s.length() > 0) s.append(' ');
            s.append(menorDeMil(resto));
        }
        return s.toString();
    }

    private static String menorDeMil(int n) {
        if (n == 100) return "CIEN";
        StringBuilder s = new StringBuilder();
        int c = n / 100, r = n % 100;
        if (c > 0) s.append(CENTENAS[c]);
        if (r > 0) {
            if (s.length() > 0) s.append(' ');
            if (r < 30) s.append(UNIDADES[r]);
            else {
                s.append(DECENAS[r / 10]);
                if (r % 10 > 0) s.append(" Y ").append(UNIDADES[r % 10]);
            }
        }
        return s.toString();
    }

    // "VEINTIUNO MIL" -> "VEINTIUN MIL"
    private static String apocope(String s) {
        return s.endsWith("UNO") ? s.substring(0, s.length() - 1) : s;
    }
}
