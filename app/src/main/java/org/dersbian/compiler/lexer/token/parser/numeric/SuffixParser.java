package org.dersbian.compiler.lexer.token.parser.numeric;

import java.util.Locale;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dersbian.compiler.lexer.token.number.INumber;

/**
 * Analisi e gestione dei suffissi di tipo.
 *
 * <p>Fornisce funzionalità per separare i literal numerici nella loro componente numerica e
 * nell'eventuale suffisso di tipo, instradando poi verso il parser specifico appropriato.
 *
 * <p>{@link #handleSuffix} restituisce {@code null} in caso di parsing fallito o suffisso
 * sconosciuto, in modo analogo a {@code Option::None} nell'implementazione Rust originale. I metodi
 * di rilevamento del pattern di suffisso ({@link #checkSingleCharSuffix}, {@link
 * #checkTwoCharSuffix}, {@link #checkThreeCharSuffix}) continuano invece a restituire {@code
 * Optional<SuffixPattern>} poiché non producono un {@link INumber}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings({"PMD.ShortVariable", "PMD.OnlyOneReturn", "PMD.LongVariable"})
public class SuffixParser {

    /** Minimum string length required for a two-character suffix. */
    private static final int TWO_CHAR_MIN_LENGTH = 2;

    /** Minimum string length required for a three-character suffix. */
    private static final int THREE_CHAR_MIN_LENGTH = 3;

    /** Rappresenta i possibili pattern di lunghezza del suffisso per i literal numerici. */
    public enum SuffixPattern {
        /** Suffissi a un carattere: {@code u}, {@code f}, {@code d} (il caso più frequente). */
        SINGLE_CHAR(1),
        /** Suffissi a due caratteri: {@code i8}, {@code u8}. */
        TWO_CHAR(2),
        /** Suffissi a tre caratteri: {@code i16}, {@code i32}, {@code u16}, {@code u32}. */
        THREE_CHAR(3);

        /** Lunghezza del suffisso espressa in numero di caratteri. */
        private final int charCount;

        SuffixPattern(final int length) {
            this.charCount = length;
        }

        /** Restituisce la lunghezza in caratteri di questo pattern di suffisso. */
        public int length() {
            return charCount;
        }
    }

    /**
     * Risultato della separazione di un literal numerico in parte numerica e suffisso opzionale.
     *
     * @param numericPart la porzione numerica, senza suffisso
     * @param suffix il suffisso (con il case originale preservato), o {@code null} se assente
     */
    public record SplitResult(String numericPart, String suffix) {}

    /**
     * Verifica se l'ultimo carattere è un suffisso a un solo carattere ({@code u}, {@code f} o
     * {@code d}, senza distinzione tra maiuscole e minuscole).
     */
    public static Optional<SuffixPattern> checkSingleCharSuffix(final String s) {
        if (s.isEmpty()) {
            return Optional.empty();
        }
        final char last = Character.toLowerCase(s.charAt(s.length() - 1));
        return (last == 'u' || last == 'f' || last == 'd')
                ? Optional.of(SuffixPattern.SINGLE_CHAR)
                : Optional.empty();
    }

    /**
     * Verifica se gli ultimi tre caratteri formano un suffisso valido a tre caratteri ({@code i16},
     * {@code i32}, {@code u16}, {@code u32}, senza distinzione tra maiuscole e minuscole).
     */
    public static Optional<SuffixPattern> checkThreeCharSuffix(final String s) {
        if (s.length() < THREE_CHAR_MIN_LENGTH) {
            return Optional.empty();
        }
        final String lastThree = s.substring(s.length() - THREE_CHAR_MIN_LENGTH);
        final char c0 = Character.toLowerCase(lastThree.charAt(0));
        final char c1 = lastThree.charAt(1);
        final char c2 = lastThree.charAt(2);

        final boolean valid =
                (c0 == 'i' || c0 == 'u') && ((c1 == '1' && c2 == '6') || (c1 == '3' && c2 == '2'));
        return valid ? Optional.of(SuffixPattern.THREE_CHAR) : Optional.empty();
    }

    /**
     * Verifica se gli ultimi due caratteri formano un suffisso valido a due caratteri ({@code i8},
     * {@code u8}, senza distinzione tra maiuscole e minuscole).
     */
    public static Optional<SuffixPattern> checkTwoCharSuffix(final String s) {
        if (s.length() < TWO_CHAR_MIN_LENGTH) {
            return Optional.empty();
        }
        final String lastTwo = s.substring(s.length() - TWO_CHAR_MIN_LENGTH);
        final char c0 = Character.toLowerCase(lastTwo.charAt(0));
        final char c1 = lastTwo.charAt(1);

        final boolean valid = (c0 == 'i' || c0 == 'u') && c1 == '8';
        return valid ? Optional.of(SuffixPattern.TWO_CHAR) : Optional.empty();
    }

    /**
     * Rileva il pattern di suffisso presente alla fine di {@code s}, controllando prima i pattern a
     * un carattere (i più frequenti), poi quelli a tre caratteri, infine quelli a due caratteri.
     */
    private static Optional<SuffixPattern> detectSuffixPattern(
            final String sufx) { // fix #1: added final
        final Optional<SuffixPattern> single = checkSingleCharSuffix(sufx); // fix #2: added final
        if (single.isPresent()) {
            return single;
        }
        final Optional<SuffixPattern> three = checkThreeCharSuffix(sufx); // fix #3: added final
        if (three.isPresent()) {
            return three;
        }
        return checkTwoCharSuffix(sufx);
    }

    /**
     * Separa un literal numerico nella sua parte numerica e nell'eventuale suffisso di tipo.
     *
     * <p>Suffissi supportati: {@code u}, {@code f}, {@code d} (un carattere); {@code i8}, {@code
     * u8} (due caratteri); {@code i16}, {@code i32}, {@code u16}, {@code u32} (tre caratteri).
     *
     * @param slice stringa completa del literal numerico, incluso l'eventuale suffisso
     * @return la porzione numerica e il suffisso opzionale (case originale preservato)
     */
    public static SplitResult splitNumericAndSuffix(final String slice) {
        final SplitResult result;
        if (slice.isEmpty()) {
            result = new SplitResult(slice, null);
        } else {
            result =
                    detectSuffixPattern(slice)
                            .map(
                                    pattern -> {
                                        final int splitPos =
                                                slice.length()
                                                        - pattern.length(); // fix #4: added final
                                        return new SplitResult(
                                                slice.substring(0, splitPos),
                                                slice.substring(splitPos));
                                    })
                            .orElse(new SplitResult(slice, null));
        }
        return result;
    }

    /**
     * Instrada il parsing del literal numerico in base al suffisso di tipo.
     *
     * <table>
     *   <caption>Tabella di risoluzione del tipo</caption>
     *   <tr><th>Suffisso</th><th>Tipo</th><th>Esempio</th></tr>
     *   <tr><td>(nessuno)</td><td>i64/f64</td>
     *   <td>{@code 42}→Integer(42), {@code 3.14}→Float64(3.14)</td></tr>
     *   <tr><td>{@code u}</td><td>u64</td><td>{@code 42u}→UnsignedInteger(42)</td></tr>
     *   <tr><td>{@code i8}</td><td>i8</td><td>{@code 42i8}→I8(42)</td></tr>
     *   <tr><td>{@code u16}</td><td>u16</td><td>{@code 1000u16}→U16(1000)</td></tr>
     *   <tr><td>{@code f}</td><td>f32</td><td>{@code 3.14f}→Float32(3.14)</td></tr>
     *   <tr><td>{@code d}</td><td>f64</td><td>{@code 3.14d}→Float64(3.14)</td></tr>
     * </table>
     *
     * @param numericPart parte numerica senza suffisso
     * @param suffix suffisso di tipo opzionale (case-insensitive), può essere {@code null}
     * @return la {@link INumber} risultante, oppure {@code null} in caso di formato non valido o
     *     suffisso non supportato
     */
    @SuppressWarnings("PMD.CyclomaticComplexity")
    public static INumber handleSuffix(final String numericPart, final String suffix) {
        final INumber result;
        if (suffix == null) {
            result = NumericParser.handleDefaultSuffix(numericPart);
        } else {
            result =
                    switch (suffix.toLowerCase(Locale.ROOT)) {
                        case "u" -> NumericParser.parseUnsigned64(numericPart);
                        case "u8" ->
                                NumericParser.parseIntegerInRange(
                                        numericPart, 255, v -> new INumber.U8((short) v));
                        case "u16" ->
                                NumericParser.parseIntegerInRange(
                                        numericPart, 65_535, v -> new INumber.U16((int) v));
                        case "u32" ->
                                NumericParser.parseIntegerInRange(
                                        numericPart, 4_294_967_295L, INumber.U32::new);
                        case "i8" ->
                                NumericParser.parseIntegerInRange(
                                        numericPart, 127, v -> new INumber.I8((byte) v));
                        case "i16" ->
                                NumericParser.parseIntegerInRange(
                                        numericPart, 32_767, v -> new INumber.I16((short) v));
                        case "i32" ->
                                NumericParser.parseIntegerInRange(
                                        numericPart, 2_147_483_647L, v -> new INumber.I32((int) v));

                        case "f" -> NumericParser.handleFloatSuffix(numericPart);
                        case "d" -> NumericParser.handleDefaultSuffix(numericPart);
                        default -> null;
                    };
        }
        return result;
    }
}
