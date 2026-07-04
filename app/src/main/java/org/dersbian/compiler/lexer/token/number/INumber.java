package org.dersbian.compiler.lexer.token.number;

/** Rappresenta un valore numerico letterale. */
@SuppressWarnings({"AvoidCommonTypeNames", "checkstyle:AbbreviationAsWordInName"})
public sealed interface INumber {

  /** Valore intero. */
  record IntegerValue(long value, String suffix) implements INumber {
    @Override
    public String toString() {
      return suffix == null ? Long.toString(value) : value + suffix;
    }
  }

  /** Valore in virgola mobile. */
  record FloatingValue(double value, String suffix) implements INumber {
    @Override
    public String toString() {
      return suffix == null ? Double.toString(value) : value + suffix;
    }
  }
}
