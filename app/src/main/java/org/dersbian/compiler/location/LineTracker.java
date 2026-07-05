package org.dersbian.compiler.location;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable 1-based line lookup used to reproduce source-context error reports.
 *
 * <p>Rust's line-tracking helper has no direct standard-library equivalent in Java, so this class
 * stores a stable snapshot of source lines and exposes the closest semantic match: optional lookup
 * by 1-based line number.
 */
public final class LineTracker {
  private final List<String> lines;

  private LineTracker(final List<String> lines) {
    this.lines = List.copyOf(lines);
  }

  /** Creates a tracker from pre-split source lines. */
  public static LineTracker fromLines(final List<String> lines) {
    return new LineTracker(Objects.requireNonNull(lines, "lines must not be null"));
  }

  /** Creates a tracker from the raw source text, preserving line order. */
  public static LineTracker fromText(final String source) {
    Objects.requireNonNull(source, "source must not be null");
    return new LineTracker(source.lines().toList());
  }

  /** Returns the line at the given 1-based index, if available. */
  public Optional<String> getLine(final int lineNumber) {
    if (lineNumber < 1 || lineNumber > lines.size()) {
      return Optional.empty();
    }
    return Optional.ofNullable(lines.get(lineNumber - 1));
  }

  /** Returns the number of tracked lines. */
  public int lineCount() {
    return lines.size();
  }
}