/**
 * Lexical analysis for the Dersco language.
 *
 * <h2>Byte-order mark handling</h2>
 *
 * <p>A leading UTF-8 byte-order mark ({@code U+FEFF}) is silently stripped at lexer construction.
 * {@link org.dersbian.compiler.lexer.Lexer#Lexer(java.nio.file.Path, String) Lexer(Path, String)}
 * passes the source through {@link
 * org.dersbian.compiler.lexer.CodePoints#stripByteOrderMark(String)} before handing it to the
 * {@link org.dersbian.compiler.lexer.SourceCursor}, so the cursor's {@link
 * org.dersbian.compiler.lexer.SourceCursor#position() position} and the resulting {@link
 * org.dersbian.compiler.lexer.token.Span} offsets are measured against the BOM-stripped text, not
 * the original file.
 *
 * <p>Consequences for callers:
 *
 * <ul>
 *   <li>The {@link org.dersbian.compiler.lexer.token.SourceId} still references the original {@link
 *       java.nio.file.Path}; only the in-memory text is shortened.
 *   <li>Diagnostic spans and {@link org.dersbian.compiler.lexer.token.SourceLocation} offsets start
 *       at zero, not one, on a BOM-prefixed file.
 *   <li>If the caller has already pre-processed the source, the lexer will not strip a BOM a second
 *       time; the second call sees a non-{@code U+FEFF} first code point and leaves the text alone.
 * </ul>
 */
package org.dersbian.compiler.lexer;
