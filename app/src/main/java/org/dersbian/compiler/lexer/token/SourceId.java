package org.dersbian.compiler.lexer.token;

import java.nio.file.Path;
import java.util.Objects;

/** Identifica la sorgente. L'ID è valido per l'intera compilazione. */
public sealed interface SourceId
    permits SourceId.FilePath,
        SourceId.VirtualResource,
        SourceId.InMemoryModule,
        SourceId.Generated {

  /** Identificativo testuale stabile della sorgente. */
  String identifier();

  /** Descrizione leggibile, utile per log e messaggi diagnostici. */
  default String describe() {
    return switch (this) {
      case FilePath(var path) -> "file: " + path;
      case VirtualResource(var uri) -> "virtual: " + uri;
      case InMemoryModule(var moduleName) -> "in-memory module: " + moduleName;
      case Generated(var description) -> "generated: " + description;
    };
  }

  /** Sorgente corrispondente a un file presente su filesystem. */
  record FilePath(Path path) implements SourceId {
    public FilePath {
      Objects.requireNonNull(path, "path must not be null");
    }

    @Override
    public String identifier() {
      return path.toString();
    }
  }

  /** Sorgente di una risorsa virtuale (URI, JAR, URL). */
  record VirtualResource(String uri) implements SourceId {
    public VirtualResource {
      Objects.requireNonNull(uri, "uri must not be null");
      if (uri.isBlank()) {
        throw new IllegalArgumentException("uri must not be blank");
      }
    }

    @Override
    public String identifier() {
      return uri;
    }
  }

  /** Sorgente di un modulo in memoria (REPL, eval). */
  record InMemoryModule(String moduleName) implements SourceId {
    public InMemoryModule {
      Objects.requireNonNull(moduleName, "moduleName must not be null");
      if (moduleName.isBlank()) {
        throw new IllegalArgumentException("moduleName must not be blank");
      }
    }

    @Override
    public String identifier() {
      return moduleName;
    }
  }

  /** Sorgente generata dal compilatore (macro, ecc.). */
  record Generated(String description) implements SourceId {
    public Generated {
      Objects.requireNonNull(description, "description must not be null");
      if (description.isBlank()) {
        throw new IllegalArgumentException("description must not be blank");
      }
    }

    @Override
    public String identifier() {
      return "<generated:" + description + ">";
    }
  }
}
