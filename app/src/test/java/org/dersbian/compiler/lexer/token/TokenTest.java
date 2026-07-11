package org.dersbian.compiler.lexer.token;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings({
    "PMD.AtLeastOneConstructor",
    "PMD.CommentRequired",
    "PMD.UnitTestAssertionsShouldIncludeMessage"
})
class TokenTest {

    @Test
    void createBuildsTokenFromExplicitLocations() {
        final SourceId sourceId = new SourceId.InMemoryModule("module");
        final SourceLocation start = SourceLocation.create(1, 1, 0L);
        final SourceLocation end = SourceLocation.create(1, 4, 3L);
        final Token token =
                Token.create(sourceId, new TokenKind.IdentifierAscii("foo"), start, end);

        Assertions.assertAll(
                () -> Assertions.assertEquals(sourceId, token.sourceId()),
                () -> Assertions.assertEquals(new TokenKind.IdentifierAscii("foo"), token.type()),
                () -> Assertions.assertEquals(Span.create(start, end), token.span()),
                () -> Assertions.assertTrue(token.isType(new TokenKind.IdentifierAscii("foo"))),
                () -> Assertions.assertFalse(token.isSynthetic()),
                () ->
                        Assertions.assertEquals(
                                "identifier 'foo' module:line 1:column 1-line 1:column 4",
                                token.toString()));
    }

    @Test
    void eofCreatesZeroLengthTokenAtLocation() {
        final SourceId sourceId = new SourceId.InMemoryModule("module");
        final SourceLocation location = SourceLocation.create(2, 1, 8L);
        final Token token = Token.eof(sourceId, location);

        Assertions.assertAll(
                () -> Assertions.assertEquals(TokenKind.Simple.Special.EOF, token.type()),
                () -> Assertions.assertEquals(Span.point(location), token.span()),
                () -> Assertions.assertTrue(token.span().isEmpty()));
    }

    @Test
    void generatedSourcesMarkTokensAsSynthetic() {
        final SourceId sourceId = new SourceId.Generated("default constructor");
        final Span span = Span.point(SourceLocation.create(1, 1, 0L));
        final Token token = Token.create(sourceId, TokenKind.Simple.Keyword.FUN, span);

        Assertions.assertTrue(token.isSynthetic());
    }
}
