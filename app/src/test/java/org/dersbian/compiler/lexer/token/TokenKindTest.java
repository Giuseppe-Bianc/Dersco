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
        final EnumSet<TokenKind.Simple.TypeKeyword> expected =
                EnumSet.of(
                        TokenKind.Simple.TypeKeyword.I8,
                        TokenKind.Simple.TypeKeyword.I16,
                        TokenKind.Simple.TypeKeyword.I32,
                        TokenKind.Simple.TypeKeyword.I64,
                        TokenKind.Simple.TypeKeyword.U8,
                        TokenKind.Simple.TypeKeyword.U16,
                        TokenKind.Simple.TypeKeyword.U32,
                        TokenKind.Simple.TypeKeyword.U64,
                        TokenKind.Simple.TypeKeyword.F32,
                        TokenKind.Simple.TypeKeyword.F64,
                        TokenKind.Simple.TypeKeyword.CHAR,
                        TokenKind.Simple.TypeKeyword.STRING,
                        TokenKind.Simple.TypeKeyword.BOOL);

        final EnumSet<TokenKind.Simple.TypeKeyword> actual =
                Arrays.stream(TokenKind.Simple.TypeKeyword.values())
                        .filter(TokenKind.Simple::isType)
                        .collect(
                                Collectors.toCollection(
                                        () -> EnumSet.noneOf(TokenKind.Simple.TypeKeyword.class)));

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void payloadTokenKindsAreNotTypeKeywords() {
        Assertions.assertFalse(new TokenKind.IdentifierAscii("i32").isType());
    }

    @Test
    void stringRepresentationsExposeDisplayText() {
        Assertions.assertEquals("Keyword 'fun'", TokenKind.Simple.Keyword.FUN.toString());
        Assertions.assertEquals(
                "Identifier 'value'", new TokenKind.IdentifierAscii("value").toString());
        Assertions.assertEquals(
                "String literal \"hello\"", new TokenKind.StringLiteral("hello").toString());
    }
}
