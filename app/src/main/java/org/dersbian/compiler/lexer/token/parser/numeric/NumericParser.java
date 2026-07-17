package org.dersbian.compiler.lexer.token.parser.numeric;

import java.util.function.LongFunction;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dersbian.compiler.lexer.token.number.INumber;

/**
 * Funzionalità principale di parsing dei literal numerici decimali.
 *
 * <p>Contiene la logica di parsing per interi, numeri a virgola mobile e notazione scientifica.
 *
 * <p>Tutti i metodi restituiscono {@code null} in caso di parsing fallito, in modo analogo a {@code
 * Option::None} nell'implementazione Rust originale.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings({"PMD.ShortVariable", "PMD.OnlyOneReturn"})
public class NumericParser {

    /**
     * Effettua il parsing di un literal numerico in una {@link INumber} strutturata.
     *
     * @param slice testo completo del literal così come riconosciuto dal lexer (parte numerica +
     *     suffisso opzionale)
     * @return la {@link INumber} risultante, oppure {@code null} in caso di formato non valido o
     *     overflow/underflow
     */
    public static INumber parseNumber(final String slice) {
        final SuffixParser.SplitResult split = SuffixParser.splitNumericAndSuffix(slice);
        return SuffixParser.handleSuffix(split.numericPart(), split.suffix());
    }

    /**
     * Effettua il parsing di un intero senza segno entro un intervallo specifico, restituendo il
     * risultato tramite {@code mapFn}.
     *
     * <p>Sostituisce, per necessità del sistema di tipi Java, la funzione generica Rust {@code
     * parse_integer::<T>}: poiché Java non offre tipi primitivi realmente generici né interi senza
     * segno stretti, il parsing avviene sempre su {@code long} seguito da un controllo esplicito
     * dei limiti del tipo di destinazione.
     *
     * @param numericPart la stringa numerica senza suffisso
     * @param max valore massimo consentito (il minimo è sempre 0, poiché {@link
     *     #isValidIntegerLiteral} esclude il segno)
     * @param mapFn funzione che avvolge il valore in una variante di {@link INumber}
     * @return il numero avvolto, oppure {@code null} se il formato non è valido o il valore eccede
     *     l'intervallo
     */
    public static INumber parseIntegerInRange(
            final String numericPart, final long max, final LongFunction<INumber> mapFn) {
        if (!isValidIntegerLiteral(numericPart)) {
            return null;
        }
        try {
            final long value = Long.parseLong(numericPart);
            if (value < 0 || value > max) {
                return null;
            }
            return mapFn.apply(value);
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    /**
     * Effettua il parsing di un intero senza segno a 64 bit (suffisso {@code u}), coprendo l'intero
     * intervallo 0..2^64-1 tramite {@link Long#parseUnsignedLong(String)}.
     *
     * @param numericPart la stringa numerica senza suffisso
     * @return {@code INumber.UnsignedInteger}, oppure {@code null} se il formato non è valido o il
     *     valore eccede u64::MAX
     */
    public static INumber parseUnsigned64(final String numericPart) {
        if (!isValidIntegerLiteral(numericPart)) {
            return null;
        }
        try {
            final long value = Long.parseUnsignedLong(numericPart);
            return new INumber.UnsignedInteger(value);
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    /**
     * Verifica che una stringa rappresenti un literal intero puro.
     *
     * <p>Un literal intero valido deve:
     *
     * <ul>
     *   <li>contenere solo cifre ASCII (0-9);
     *   <li>non contenere un punto decimale ({@code .});
     *   <li>non contenere un marcatore di esponente ({@code e} o {@code E});
     *   <li>non contenere un carattere di segno (gestito come token separato dal lexer).
     * </ul>
     *
     * @param numericPart la stringa numerica da validare
     * @return {@code true} se la stringa è un literal intero valido
     */
    public static boolean isValidIntegerLiteral(final String numericPart) {
        if (numericPart.isEmpty()) {
            return false;
        }
        for (int i = 0; i < numericPart.length(); i++) {
            final char c = numericPart.charAt(i);
            if (c == '.' || c == 'e' || c == 'E') {
                return false;
            }
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    /**
     * Effettua il parsing di stringhe numeriche con suffisso float a 32 bit ({@code f}).
     *
     * <p>Gestisce sia la notazione decimale semplice sia quella scientifica, producendo
     * rispettivamente {@link INumber.Float32} o {@link INumber.Scientific32}.
     *
     * @param numericPart la stringa numerica privata del suffisso {@code f}
     * @return il numero risultante, oppure {@code null} se il parsing fallisce
     */
    public static INumber handleFloatSuffix(final String numericPart) {
        final INumber scientific = parseScientific(numericPart, true);
        if (scientific != null) {
            return scientific;
        }
        try {
            return new INumber.Float32(Float.parseFloat(numericPart));
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    /**
     * Effettua il parsing di stringhe numeriche senza suffisso o con suffisso {@code d}.
     *
     * <p>Implementa le regole di inferenza del tipo predefinito: literal interi (senza
     * decimale/esponente) → i64; literal a virgola mobile → f64; notazione scientifica →
     * Scientific64.
     *
     * @param numericPart la stringa numerica senza suffisso (o con il suffisso {@code d} già
     *     rimosso)
     * @return il numero risultante, oppure {@code null} se il parsing fallisce
     */
    public static INumber handleDefaultSuffix(final String numericPart) {
        final INumber scientific = parseScientific(numericPart, false);
        return scientific != null ? scientific : handleNonScientific(numericPart);
    }

    /**
     * Effettua il parsing di numeri in notazione non scientifica (interi e float semplici).
     *
     * <p>Determina il tipo appropriato in base alla presenza di un punto decimale: assente → intero
     * {@code i64}; presente → {@code f64}.
     *
     * @param numericPart la stringa numerica da analizzare
     * @return {@code INumber.Integer} per i literal senza punto decimale, {@code INumber.Float64}
     *     per quelli con punto decimale, oppure {@code null} se il parsing fallisce (overflow,
     *     underflow o formato non valido)
     */
    public static INumber handleNonScientific(final String numericPart) {
        try {
            if (numericPart.indexOf('.') >= 0) {
                return new INumber.Float64(Double.parseDouble(numericPart));
            }
            return new INumber.Integer(Long.parseLong(numericPart));
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    /**
     * Effettua il parsing di numeri in notazione scientifica (es. {@code "6.022e23"}).
     *
     * <p>Formato: {@code base[e|E][+|-]esponente}, dove la base può essere un numero intero o a
     * virgola mobile.
     *
     * @param s stringa numerica completa, potenzialmente in notazione scientifica
     * @param isF32 se {@code true} la base viene interpretata come float a 32 bit, altrimenti come
     *     64 bit
     * @return {@code INumber.Scientific32}/{@code INumber.Scientific64}, oppure {@code null} se non
     *     si tratta di notazione scientifica o il parsing fallisce
     */
    public static INumber parseScientific(final String s, final boolean isF32) {
        final int pos = indexOfExponentMarker(s);
        if (pos < 0) {
            return null;
        }
        final String baseStr = s.substring(0, pos);
        final String expStr = s.substring(pos + 1);

        final int exp;
        try {
            exp = Integer.parseInt(expStr);
        } catch (final NumberFormatException e) {
            return null;
        }

        try {
            if (isF32) {
                final float base = Float.parseFloat(baseStr);
                return new INumber.Scientific32(base, exp);
            }
            final double base = Double.parseDouble(baseStr);
            return new INumber.Scientific64(base, exp);
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    private static int indexOfExponentMarker(final String s) {
        final int idxLower = s.indexOf('e');
        final int idxUpper = s.indexOf('E');
        if (idxLower < 0) {
            return idxUpper;
        }
        if (idxUpper < 0) {
            return idxLower;
        }
        return Math.min(idxLower, idxUpper);
    }
}
