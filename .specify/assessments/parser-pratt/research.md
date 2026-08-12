# Idea Research: Parser ricorsivo-discesa e Pratt

- **Slug**: parser-pratt
- **Created**: 2026-08-11
- **Evidence confidence (overall)**: medium

## Users & Demand

- La richiesta dell'utente descrive esplicitamente un parser con recursive descent per i costrutti di alto livello e Pratt parsing per le espressioni, includendo test per precedenza e associatività. Si tratta di una richiesta dichiarata, non di un comportamento utente osservato. — [source: `.specify/assessments/parser-pratt/intake.md` | cited] (confidence: high)
- L'intake non contiene ticket, interviste, telemetria o dati d'uso che mostrino quante persone incontrano oggi il problema. — [source: `.specify/assessments/parser-pratt/intake.md` | cited] (confidence: high)

## Prior Art

- Il repository contiene già un AST sigillato: `Expr` copre espressioni binarie, unarie, grouping, letterali, array, variabili, assegnamenti, chiamate e accessi ad array; `Stmt` modella dichiarazioni, funzioni, controlli di flusso, blocchi e istruzioni di controllo. Questo è precedente interno rilevante, ma non costituisce prova di un parser collegato. — [source: `app/src/main/java/org/dersbian/compiler/syntax/ast/Expr.java`; `app/src/main/java/org/dersbian/compiler/syntax/ast/Stmt.java` | cited] (confidence: high)
- `BinaryOp` mappa operatori aritmetici, relazionali, logici, bitwise, shift e di assegnamento composto; `UnaryOp` e `UnaryOpSide` distinguono operatori unari e posizione prefissa/postfissa. — [source: `app/src/main/java/org/dersbian/compiler/syntax/ast/BinaryOp.java`; `app/src/main/java/org/dersbian/compiler/syntax/ast/UnaryOp.java`; `app/src/main/java/org/dersbian/compiler/syntax/ast/UnaryOpSide.java` | cited] (confidence: high)
- La cronologia Git registra l'introduzione e l'evoluzione dell'AST (tra cui `40ffc7c feat(ast): implement abstract syntax tree for expressions and statements` e `dcb3835 feat(ast): add UnaryOpSide enum and update Unary expression structure`), mentre `DefaultCompilerService` conserva il commento `TODO: wire up the real parser.` — [source: Git history for `app/src/main/java/org/dersbian/compiler`; `app/src/main/java/org/dersbian/compiler/DefaultCompilerService.java` | cited] (confidence: high)
- Una ricerca statica nel codice di produzione, nei campioni `dr_files` e negli artefatti Spec Kit non ha trovato una grammatica né classi/interfacce Parser o punti d'ingresso `parseExpression`, `parseProgram` o `parseStatement`. È evidenza limitata di assenza nell'ambito cercato, non una prova assoluta di assenza dal progetto. — [source: repository search executed 2026-08-11 over `app/src/main`, `dr_files`, `.specify` | cited] (confidence: medium)

## Market & Context

- Il flusso attuale di `checkSyntax` legge il sorgente, tokenizza, produce il report degli errori lessicali e termina con un TODO per il parser; pertanto il controllo sintattico esposto dal servizio non mostra un'analisi sintattica basata sull'AST in questo punto del codice. — [source: `app/src/main/java/org/dersbian/compiler/DefaultCompilerService.java` | cited] (confidence: high)
- Non sono state raccolte fonti esterne su linguaggi o compilatori comparabili: la policy della skill richiede il pinning o la verifica dell'IP del peer per ogni fetch, capacità non esposta dallo strumento disponibile. — [source: `speckit-assess-research` URL Trust Policy | cited] (confidence: high)

## Data & Constraints

- Il lexer riconosce gli operatori e i test del lexer includono forme con prefissi sovrapposti come `+`, `+=`, `++`, `=`, `==`, `!`, `!=`, `<`, `<=`, `<<` e `<<=`, fornendo un insieme di token già distinto per il livello di parsing. — [source: `app/src/test/java/org/dersbian/compiler/lexer/LexerTest.java`; `app/src/main/java/org/dersbian/compiler/lexer/token/TokenKind.java` | cited] (confidence: high)
- Il modello `CompileError.SyntaxError` già trasporta codice, messaggio, span e aiuto opzionale; `BinaryOp` usa inoltre un errore sintattico specifico per un token non valido come operatore binario. — [source: `app/src/main/java/org/dersbian/compiler/error/CompileError.java`; `app/src/main/java/org/dersbian/compiler/syntax/ast/BinaryOp.java` | cited] (confidence: high)
- Nei file esaminati non è stata trovata una tabella normativa di grammatica, precedenza o associatività. L'insieme di token e AST da solo non determina queste regole. — [source: repository search executed 2026-08-11 over `app/src/main`, `dr_files`, `.specify`; `app/src/main/java/org/dersbian/compiler/lexer/token/TokenKind.java`; `app/src/main/java/org/dersbian/compiler/syntax/ast/BinaryOp.java` | cited] (confidence: medium)

## Evidence Against the Idea

- Non esiste evidenza disponibile che definisca la grammatica completa né la precedenza e l'associatività normative; senza tali fonti non è possibile verificare oggettivamente la conformità richiesta dall'idea. — [source: `.specify/assessments/parser-pratt/intake.md`; repository search executed 2026-08-11 over `app/src/main`, `dr_files`, `.specify` | cited] (confidence: medium)
- Non esistono dati osservati su domanda, frequenza del problema, impatto sugli utenti o costo dell'assenza di parser; la domanda attuale è quindi limitata a un singolo requisito dichiarato. — [source: `.specify/assessments/parser-pratt/intake.md` | cited] (confidence: high)
- L'AST e i token danno indicazioni sulle forme sintattiche potenziali, ma non provano quali di esse debbano essere supportate nella prima integrazione; ampliare il perimetro oltre la grammatica documentata resterebbe un'assunzione. — [source: `app/src/main/java/org/dersbian/compiler/syntax/ast/Expr.java`; `app/src/main/java/org/dersbian/compiler/syntax/ast/Stmt.java`; `.specify/assessments/parser-pratt/intake.md` | cited] (confidence: high)

## Gaps & Open Questions

- [NEEDS CLARIFICATION: qual è la fonte autorevole della grammatica del linguaggio Dersco e quale sua versione delimita l'iniziativa.]
- [NEEDS CLARIFICATION: quali livelli di precedenza e regole di associatività, comprese quelle per assegnamenti, devono essere normativi.]
- [NEEDS CLARIFICATION: quali nodi dell'AST esistente sono nel perimetro iniziale e quali sono solo preparatori.]
- [NEEDS CLARIFICATION: chi sono gli utenti o stakeholder interessati e quale problema concreto subiscono con l'attuale controllo soltanto lessicale.]
- [NEEDS CLARIFICATION: quali requisiti di recupero dagli errori e di diagnostica sintattica devono essere misurati.]
- [NEEDS CLARIFICATION: quali fonti esterne o progetti comparabili il committente autorizza a usare come evidenza aggiuntiva.]

## Sources

- `.specify/assessments/parser-pratt/intake.md` (system: local assessment artifact)
- `app/src/main/java/org/dersbian/compiler/DefaultCompilerService.java` (system: local repository)
- `app/src/main/java/org/dersbian/compiler/syntax/ast/Expr.java` (system: local repository)
- `app/src/main/java/org/dersbian/compiler/syntax/ast/Stmt.java` (system: local repository)
- `app/src/main/java/org/dersbian/compiler/syntax/ast/BinaryOp.java` (system: local repository)
- `app/src/main/java/org/dersbian/compiler/error/CompileError.java` (system: local repository)
- Git history for `app/src/main/java/org/dersbian/compiler` (system: local repository)
- `speckit-assess-research` URL Trust Policy (system: local skill instructions)
