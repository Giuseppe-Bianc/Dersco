package org.dersbian.compiler.location;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Tracks source lines to support line-based lookups (e.g., for error reporting). */
public final class LineTracker {

  /** The source lines, 0-indexed internally but exposed as 1-based line numbers. */
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
    final Optional<String> result;
    if (lineNumber < 1 || lineNumber > lines.size()) {
      result = Optional.empty();
    } else {
      result = Optional.ofNullable(lines.get(lineNumber - 1));
    }
    return result;
  }

  /** Returns the number of tracked lines. */
  public int lineCount() {
    return lines.size();
  }
}
