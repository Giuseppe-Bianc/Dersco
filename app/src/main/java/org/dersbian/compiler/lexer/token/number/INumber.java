package org.dersbian.compiler.lexer.token.number;

import java.util.Objects;

/**
 * Rappresenta i literal numerici nei vari formati supportati dal linguaggio.
 *
 * <p>Questa interfaccia sealed cattura le diverse rappresentazioni di numeri trovate nel codice
 * sorgente, preservandone il formato originale per una segnalazione precisa degli errori e
 * un'elaborazione accurata durante la compilazione.
 *
 * <h2>Sicurezza dei tipi</h2>
 *
 * <p>Ogni record impone i limiti specifici del tipo tramite il tipo primitivo scelto, permettendo
 * di rilevare errori di overflow/underflow già in fase di tokenizzazione, invece di rimandarli a
 * fasi successive della compilazione.
 *
 * <h2>Esempi</h2>
 *
 * <pre>{@code
 * Number small     = new Number.I8((byte) -42);
 * Number large     = new Number.Integer(9223372036854775807L);
 * Number byteVal   = new Number.U8((short) 255);
 * Number unsigned  = new Number.UnsignedInteger(42L);
 * Number pi        = new Number.Float64(3.14159);
 * Number planck    = new Number.Scientific64(6.62607015, -34);
 * }</pre>
 *
 * <p>Nota sulla rappresentazione degli interi senza segno: Java non dispone di tipi primitivi senza
 * segno più stretti di {@code long}. Per {@code u8}, {@code u16} e {@code u32} si usa quindi il
 * tipo con segno immediatamente più largo ({@code short}, {@code int}, {@code long}
 * rispettivamente), che può rappresentare l'intero intervallo non negativo come valore positivo.
 * Per {@code u64} si usa {@code long} conservando il pattern di bit grezzo: la stampa e il parsing
 * decimale sfruttano {@link Long#toUnsignedString} e {@link Long#parseUnsignedLong}.
 */
@SuppressWarnings({"AvoidCommonTypeNames", "checkstyle:AbbreviationAsWordInName"})
public sealed interface INumber {

    /** Literal intero con segno a 8 bit (es. {@code -42i8}). Intervallo: -128..127 */
    record I8(byte value) implements INumber {
        @Override
        public String toString() {
            return value + "i8";
        }
    }

    /** Literal intero con segno a 16 bit (es. {@code 1234i16}). Intervallo: -32768..32767 */
    record I16(short value) implements INumber {
        @Override
        public String toString() {
            return value + "i16";
        }
    }

    /** Literal intero con segno a 32 bit (es. {@code 123456i32}). */
    record I32(int value) implements INumber {
        @Override
        public String toString() {
            return value + "i32";
        }
    }

    /**
     * Literal intero con segno a 64 bit (es. {@code -42}, {@code 1234}). Tipo intero predefinito
     * quando non è specificato alcun suffisso.
     */
    record Integer(long value) implements INumber {
        @Override
        public String toString() {
            return Long.toString(value);
        }
    }

    /**
     * Literal intero senza segno a 8 bit (es. {@code 42u8}). Rappresentato come {@code short}
     * perché Java non ha un tipo a 8 bit capace di contenere l'intero intervallo 0..255 come valore
     * positivo.
     */
    record U8(short value) implements INumber {
        @Override
        public String toString() {
            return value + "u8";
        }
    }

    /**
     * Literal intero senza segno a 16 bit (es. {@code 1234u16}). Rappresentato come {@code int} per
     * contenere l'intervallo 0..65535.
     */
    record U16(int value) implements INumber {
        @Override
        public String toString() {
            return value + "u16";
        }
    }

    /**
     * Literal intero senza segno a 32 bit (es. {@code 123456u32}). Rappresentato come {@code long}
     * per contenere l'intervallo 0..4294967295.
     */
    record U32(long value) implements INumber {
        @Override
        public String toString() {
            return value + "u32";
        }
    }

    /**
     * Literal intero senza segno a 64 bit (es. {@code 42u}, {@code 1234u}). Rappresentato come
     * {@code long} contenente il pattern di bit grezzo; usare {@link Long#toUnsignedString(long)}
     * per la stampa quando il valore può superare {@link Long#MAX_VALUE}.
     */
    record UnsignedInteger(long value) implements INumber {
        @Override
        public String toString() {
            return Long.toUnsignedString(value);
        }
    }

    /** Literal a virgola mobile a 32 bit (es. {@code 3.14f}, {@code 6.022e23f}). */
    record Float32(float value) implements INumber {
        /**
         * Confronto bit-a-bit (via {@link Float#floatToRawIntBits}) per gestire NaN e zero con
         * segno in modo coerente, analogamente a {@code to_bits()} in Rust.
         */
        @Override
        public boolean equals(final Object obj) {
            return obj instanceof Float32 other
                    && Float.floatToRawIntBits(value) == Float.floatToRawIntBits(other.value());
        }

        @Override
        public int hashCode() {
            return Objects.hash(Float32.class, Float.floatToRawIntBits(value));
        }

        @Override
        public String toString() {
            return Float.toString(value);
        }
    }

    /** Literal a virgola mobile a 64 bit (es. {@code 3.14159}, {@code 6.02214076e23}). */
    record Float64(double value) implements INumber {
        @Override
        public boolean equals(final Object obj) {
            return obj instanceof Float64 other
                    && Double.doubleToRawLongBits(value)
                            == Double.doubleToRawLongBits(other.value());
        }

        @Override
        public int hashCode() {
            return Objects.hash(Float64.class, Double.doubleToRawLongBits(value));
        }

        @Override
        public String toString() {
            return Double.toString(value);
        }
    }

    /**
     * Notazione scientifica con base e esponente a 32 bit (es. {@code 6.022e23f}). Memorizza il
     * numero nella forma: base × 10^esponente.
     *
     * @param base valore di base (mantissa)
     * @param exponent esponente (potenza di 10)
     */
    record Scientific32(float base, int exponent) implements INumber {
        @Override
        public boolean equals(final Object obj) {
            return obj instanceof Scientific32 other
                    && Float.floatToRawIntBits(base) == Float.floatToRawIntBits(other.base())
                    && exponent == other.exponent();
        }

        @Override
        public int hashCode() {
            return Objects.hash(Scientific32.class, Float.floatToRawIntBits(base), exponent);
        }

        @Override
        public String toString() {
            return base + "e" + exponent;
        }
    }

    /**
     * Notazione scientifica con base e esponente a 64 bit (es. {@code 6.02214076e23}). Memorizza il
     * numero nella forma: base × 10^esponente.
     *
     * @param base valore di base (mantissa)
     * @param exponent esponente (potenza di 10)
     */
    record Scientific64(double base, int exponent) implements INumber {
        @Override
        public boolean equals(final Object obj) {
            return obj instanceof Scientific64 other
                    && Double.doubleToRawLongBits(base) == Double.doubleToRawLongBits(other.base())
                    && exponent == other.exponent();
        }

        @Override
        public int hashCode() {
            return Objects.hash(Scientific64.class, Double.doubleToRawLongBits(base), exponent);
        }

        @Override
        public String toString() {
            return base + "e" + exponent;
        }
    }
}
