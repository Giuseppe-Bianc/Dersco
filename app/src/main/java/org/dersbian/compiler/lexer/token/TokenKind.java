package org.dersbian.compiler.lexer.token;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Rappresenta tutti i possibili tipi di token del linguaggio. */
@SuppressWarnings("PMD")
public sealed interface TokenKind {

  /**
   * Annotazione puramente documentale, analoga a {@code #[must_use]} di Rust. Java non dispone di
   * un controllo nativo verificato dal compilatore per il valore di ritorno; strumenti esterni (es.
   * Error Prone con {@code @CheckReturnValue}) offrono un controllo simile, ma introdurrebbero una
   * dipendenza non presente nel sorgente originale.
   */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target(ElementType.METHOD)
  @interface MustUse {}

  /**
   * Verifica se il token rappresenta una parola chiave di tipo.
   *
   * <p>Traduzione di {@code TokenKind::is_type}. In Rust era una {@code const fn}: Java non ha una
   * valutazione "const" generica per metodi con logica arbitraria, quindi qui è un normale metodo
   * d'istanza (di default sull'interfaccia), pur restando puro / privo di effetti collaterali come
   * l'originale.
   *
   * @return {@code true} per tutte le varianti di tipo (i8, u8, f32, ...), {@code false} altrimenti
   */
  @MustUse
  default boolean isType() {
    if (this instanceof Simple s) {
      return switch (s) {
        case TYPE_I8, TYPE_I16, TYPE_I32, TYPE_I64 -> true;
        case TYPE_U8, TYPE_U16, TYPE_U32, TYPE_U64 -> true;
        case TYPE_F32, TYPE_F64, TYPE_CHAR, TYPE_STRING, TYPE_BOOL -> true;
        default -> false;
      };
    }
    return false;
  }

  // ------------------------------------------------------------------
  // Varianti senza payload ("unit variant" -> costanti di un enum)
  // ------------------------------------------------------------------

  /**
   * Tutte le varianti di {@code TokenKind} prive di dati associati: operatori, parole chiave
   * semplici, parentesi/parentesi quadre/graffe, parole chiave di tipo e token speciali (spazi,
   * commenti, EOF).
   */
  enum Simple implements TokenKind {
    // Operatori multi-carattere
    PLUS_EQUAL,
    MINUS_EQUAL,
    EQUAL_EQUAL,
    NOT_EQUAL,
    LESS_EQUAL,
    GREATER_EQUAL,
    PLUS_PLUS,
    MINUS_MINUS,
    OR_OR,
    AND_AND,
    SHIFT_LEFT,
    SHIFT_RIGHT,
    PERCENT_EQUAL,
    XOR_EQUAL,

    // Operatori a singolo carattere
    PLUS,
    MINUS,
    STAR,
    SLASH,
    LESS,
    GREATER,
    NOT,
    XOR,
    PERCENT,
    OR,
    AND,
    EQUAL,
    COLON,
    COMMA,
    DOT,

    // Parole chiave
    KEYWORD_FUN,
    KEYWORD_IF,
    KEYWORD_ELSE,
    KEYWORD_RETURN,
    KEYWORD_WHILE,
    KEYWORD_FOR,
    KEYWORD_MAIN,
    KEYWORD_VAR,
    KEYWORD_CONST,
    KEYWORD_NULLPTR,
    KEYWORD_BREAK,
    KEYWORD_CONTINUE,

    // Parentesi
    OPEN_PAREN,
    CLOSE_PAREN,
    OPEN_BRACKET,
    CLOSE_BRACKET,
    OPEN_BRACE,
    CLOSE_BRACE,

    // Parole chiave di tipo
    TYPE_I8,
    TYPE_I16,
    TYPE_I32,
    TYPE_I64,
    TYPE_U8,
    TYPE_U16,
    TYPE_U32,
    TYPE_U64,
    TYPE_F32,
    TYPE_F64,
    TYPE_CHAR,
    TYPE_STRING,
    TYPE_BOOL,

    // Token speciali
    SEMICOLON,
    WHITESPACE,
    COMMENT,
    MULTILINE_COMMENT,

    /** Marcatore di fine file (mai prodotto direttamente dal lexer). */
    EOF;

    /** Traduzione di {@code impl fmt::Display for TokenKind}. */
    @Override
    public String toString() {
      return switch (this) {
        case PLUS -> "'+'";
        case MINUS -> "'-'";
        case STAR -> "'*'";
        case SLASH -> "'/'";
        case PLUS_EQUAL -> "'+='";
        case MINUS_EQUAL -> "'-='";
        case EQUAL_EQUAL -> "'=='";
        case NOT_EQUAL -> "'!='";
        case LESS -> "'<'";
        case GREATER -> "'>'";
        case LESS_EQUAL -> "'<='";
        case GREATER_EQUAL -> "'>='";
        case PLUS_PLUS -> "'++'";
        case MINUS_MINUS -> "'--'";
        case OR_OR -> "'||'";
        case AND_AND -> "'&&'";
        case SHIFT_LEFT -> "'<<'";
        case SHIFT_RIGHT -> "'>>'";
        case PERCENT_EQUAL -> "'%='";
        case XOR_EQUAL -> "'^='";
        case NOT -> "'!'";
        case XOR -> "'^'";
        case PERCENT -> "'%'";
        case OR -> "'|'";
        case AND -> "'&'";
        case EQUAL -> "'='";
        case COLON -> "':'";
        case COMMA -> "','";
        case DOT -> "'.'";
        case SEMICOLON -> "';'";

        case KEYWORD_FUN -> "'fun'";
        case KEYWORD_IF -> "'if'";
        case KEYWORD_ELSE -> "'else'";
        case KEYWORD_RETURN -> "'return'";
        case KEYWORD_WHILE -> "'while'";
        case KEYWORD_FOR -> "'for'";
        case KEYWORD_MAIN -> "'main'";
        case KEYWORD_VAR -> "'var'";
        case KEYWORD_CONST -> "'const'";
        case KEYWORD_NULLPTR -> "'nullptr'";
        case KEYWORD_BREAK -> "'break'";
        case KEYWORD_CONTINUE -> "'continue'";

        case OPEN_PAREN -> "'('";
        case CLOSE_PAREN -> "')'";
        case OPEN_BRACKET -> "'['";
        case CLOSE_BRACKET -> "']'";
        case OPEN_BRACE -> "'{'";
        case CLOSE_BRACE -> "'}'";

        case TYPE_I8 -> "'i8'";
        case TYPE_I16 -> "'i16'";
        case TYPE_I32 -> "'i32'";
        case TYPE_I64 -> "'i64'";
        case TYPE_U8 -> "'u8'";
        case TYPE_U16 -> "'u16'";
        case TYPE_U32 -> "'u32'";
        case TYPE_U64 -> "'u64'";
        case TYPE_F32 -> "'f32'";
        case TYPE_F64 -> "'f64'";
        case TYPE_CHAR -> "'char'";
        case TYPE_STRING -> "'string'";
        case TYPE_BOOL -> "'bool'";

        case WHITESPACE -> "whitespace";
        case COMMENT -> "comment";
        case MULTILINE_COMMENT -> "multiline comment";
        case EOF -> "end of file";
      };
    }
  }

  // ------------------------------------------------------------------
  // Varianti con payload ("tuple variant" -> record)
  // ------------------------------------------------------------------

  /** Letterale booleano (`true`/`false`). */
  record KeywordBool(boolean value) implements TokenKind {
    @Override
    public String toString() {
      return "boolean '" + value + "'";
    }
  }

  /** Identificatore ASCII (lettere, cifre, underscore). */
  record IdentifierAscii(String value) implements TokenKind {
    @Override
    public String toString() {
      return "identifier '" + value + "'";
    }
  }

  /** Identificatore Unicode (supporta caratteri internazionali). */
  record IdentifierUnicode(String value) implements TokenKind {
    @Override
    public String toString() {
      return "identifier '" + value + "'";
    }
  }

  /** Letterale numerico (interi, float, notazione scientifica, suffissi). */
  record Numeric(Number value) implements TokenKind {
    @Override
    public String toString() {
      return "number '" + value + "'";
    }
  }

  /** Letterale binario (es. {@code #b1010u}). */
  record Binary(Number value) implements TokenKind {
    @Override
    public String toString() {
      return "binary '" + value + "'";
    }
  }

  /** Letterale ottale (es. {@code #o755}). */
  record Octal(Number value) implements TokenKind {
    @Override
    public String toString() {
      return "octal '" + value + "'";
    }
  }

  /** Letterale esadecimale (es. {@code #xdeadbeefu}). */
  record Hexadecimal(Number value) implements TokenKind {
    @Override
    public String toString() {
      return "hexadecimal '" + value + "'";
    }
  }

  /** Letterale stringa (contenuto già privato delle virgolette). */
  record StringLiteral(String value) implements TokenKind {
    @Override
    public String toString() {
      return "string literal \"" + value + "\"";
    }
  }

  /** Letterale carattere (contenuto già privato degli apici). */
  record CharLiteral(String value) implements TokenKind {
    @Override
    public String toString() {
      return "character literal '" + value + "'";
    }
  }
}
