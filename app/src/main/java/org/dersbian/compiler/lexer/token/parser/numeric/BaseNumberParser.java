package org.dersbian.compiler.lexer.token.parser.numeric;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dersbian.compiler.lexer.token.number.INumber;

/**
 * Parsing dei literal numerici con base esplicita (binario, ottale, esadecimale).
 *
 * <p>Gestisce literal con prefisso:
 *
 * <ul>
 *   <li>Binario: {@code #b} (es. {@code #b1010})
 *   <li>Ottale: {@code #o} (es. {@code #o755})
 *   <li>Esadecimale: {@code #x} (es. {@code #xDEADBEEF})
 * </ul>
 *
 * <p>Supporta il suffisso opzionale senza segno ({@code u} o {@code U}).
 *
 * <p>Tutti i metodi restituiscono {@code null} in caso di parsing fallito (formato non valido,
 * cifre non ammesse per la base, overflow), in modo analogo a {@code Option::None}
 * nell'implementazione Rust originale.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("PMD.OnlyOneReturn")
public class BaseNumberParser {
    /** The length of a numeric base prefix (e.g., "0b", "0x", "0o"). */
    private static final int PREFIX_LENGTH = 2;

    /** The base/radix used for parsing binary numbers. */
    private static final int BINARY_RADIX = 2;

    /** The base/radix used for parsing octal numbers. */
    private static final int OCTAL_RADIX = 8;

    /** The base/radix used for parsing hexadecimal numbers. */
    private static final int HEX_RADIX = 16;

    /**
     * Parser generico per i literal numerici con base esplicita.
     *
     * @param radix base numerica (2, 8 o 16)
     * @param slice testo completo del literal, incluso il prefisso di 2 caratteri
     * @return {@code Number.Integer} per i literal con segno (nessun suffisso {@code u}), {@code
     *     Number.UnsignedInteger} per quelli senza segno, oppure {@code null} se il parsing
     *     fallisce o il literal contiene cifre non valide per la base indicata
     */
    public static INumber parseBaseNumber(final int radix, final String slice) {
        if (slice.length() < PREFIX_LENGTH) {
            return null;
        }
        final String numPart = slice.substring(PREFIX_LENGTH);

        final char last = numPart.isEmpty() ? '\0' : numPart.charAt(numPart.length() - 1);
        final boolean suffixU = last == 'u' || last == 'U';
        final String numStr = suffixU ? numPart.substring(0, numPart.length() - 1) : numPart;

        try {
            if (suffixU) {
                final long value = Long.parseUnsignedLong(numStr, radix);
                return new INumber.UnsignedInteger(value);
            }
            final long value = Long.parseLong(numStr, radix);
            return new INumber.Integer(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Effettua il parsing di literal binari con prefisso {@code #b}.
     *
     * <p>Formato: prefisso {@code #b} obbligatorio, cifre {@code 0}/{@code 1}, suffisso {@code
     * u}/{@code U} opzionale.
     *
     * @return il numero risultante, oppure {@code null} in caso di errore
     */
    public static INumber parseBinary(final String slice) {
        return parseBaseNumber(BINARY_RADIX, slice);
    }

    /**
     * Effettua il parsing di literal ottali con prefisso {@code #o}.
     *
     * <p>Formato: prefisso {@code #o} obbligatorio, cifre {@code 0-7}, suffisso {@code u}/{@code U}
     * opzionale.
     *
     * @return il numero risultante, oppure {@code null} in caso di errore
     */
    public static INumber parseOctal(final String slice) {
        return parseBaseNumber(OCTAL_RADIX, slice);
    }

    /**
     * Effettua il parsing di literal esadecimali con prefisso {@code #x}.
     *
     * <p>Formato: prefisso {@code #x} obbligatorio, cifre {@code 0-9}, {@code A-F}/{@code a-f},
     * suffisso {@code u}/{@code U} opzionale.
     *
     * @return il numero risultante, oppure {@code null} in caso di errore
     */
    public static INumber parseHex(final String slice) {
        return parseBaseNumber(HEX_RADIX, slice);
    }
}
