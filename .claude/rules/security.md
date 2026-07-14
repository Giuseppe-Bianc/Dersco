---
paths:
  - "app/src/main/java/org/dersbian/compiler/**"
  - "app/src/main/java/org/dersbian/cli/**"
  - "app/src/main/java/org/dersbian/App.java"
---

# Security

- Validate all input at the system boundary. Never trust source files, CLI args, or env values.
- Use parameterized process builders. Never concatenate user input into shell commands.
- Sanitize diagnostic output to prevent terminal injection. Use the ANSI helpers in `compiler/error/`.
- Never log secrets, tokens, passwords, or PII surfaced during compilation.
- Use constant-time comparison when comparing tokens or hashes.
- Build artifacts and bytecode paths must stay inside the configured output directory.
- File reads must use an explicit, validated `Charset` (UTF-8) and bounded buffer sizes.
