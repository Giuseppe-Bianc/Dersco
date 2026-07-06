package org.dersbian.compiler.lexer.token;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings({
    "PMD.AtLeastOneConstructor",
    "PMD.CommentRequired",
    "PMD.LooseCoupling",
    "PMD.UnitTestAssertionsShouldIncludeMessage",
    "PMD.UnitTestContainsTooManyAsserts"
})
class TokenKindTest {

    @Test
    void simpleTypeKeywordsReportAsTypes() {
        final EnumSet<TokenKind.Simple> expected =
                EnumSet.of(
                        TokenKind.Simple.TYPE_I8,
                        TokenKind.Simple.TYPE_I16,
                        TokenKind.Simple.TYPE_I32,
                        TokenKind.Simple.TYPE_I64,
                        TokenKind.Simple.TYPE_U8,
                        TokenKind.Simple.TYPE_U16,
                        TokenKind.Simple.TYPE_U32,
                        TokenKind.Simple.TYPE_U64,
                        TokenKind.Simple.TYPE_F32,
                        TokenKind.Simple.TYPE_F64,
                        TokenKind.Simple.TYPE_CHAR,
                        TokenKind.Simple.TYPE_STRING,
                        TokenKind.Simple.TYPE_BOOL);

        final EnumSet<TokenKind.Simple> actual =
                Arrays.stream(TokenKind.Simple.values())
                        .filter(TokenKind.Simple::isType)
                        .collect(
                                Collectors.toCollection(
                                        () -> EnumSet.noneOf(TokenKind.Simple.class)));

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void payloadTokenKindsAreNotTypeKeywords() {
        Assertions.assertFalse(new TokenKind.IdentifierAscii("i32").isType());
    }

    @Test
    void stringRepresentationsExposeDisplayText() {
        Assertions.assertEquals("'fun'", TokenKind.Simple.KEYWORD_FUN.toString());
        Assertions.assertEquals(
                "identifier 'value'", new TokenKind.IdentifierAscii("value").toString());
        Assertions.assertEquals(
                "string literal \"hello\"", new TokenKind.StringLiteral("hello").toString());
    }
}
