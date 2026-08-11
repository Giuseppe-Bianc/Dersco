# Idea Intake: Parser ricorsivo-discesa e Pratt

- **Slug**: parser-pratt
- **Created**: 2026-08-11
- **Source**: pasted text; repository context: `C:\dev\intellij\Dersco`
- **Type**: new-capability

## Idea (as captured)

> Il tuo compito consiste nell'implementare un parser per il mio progetto utilizzando un'architettura basata sulla combinazione di **Recursive Descent Parsing** e **Pratt Parsing**.
>
> Il **Recursive Descent Parser** deve essere utilizzato per gestire la struttura sintattica generale del linguaggio, organizzando il parsing attraverso funzioni dedicate ai diversi costrutti previsti dalla grammatica. L'implementazione deve mantenere una separazione chiara tra le diverse categorie sintattiche, in modo da rendere il parser leggibile, modulare ed estendibile.
>
> Il **Pratt Parser** deve essere utilizzato per la gestione delle espressioni e, in particolare, per determinare correttamente il raggruppamento degli operatori in base alla loro **precedenza** e alla loro **associatività**. La precedenza deve stabilire l'ordine con cui gli operatori vengono applicati, mentre l'associatività deve determinare il modo in cui vengono raggruppati gli operatori che hanno lo stesso livello di precedenza.
>
> Il parser deve supportare in modo esplicito gli operatori con associatività **sinistra** e **destra**, rispettando le regole definite dalla grammatica del linguaggio. Il Pratt Parsing deve inoltre essere strutturato in modo da poter gestire le diverse categorie di operatori previste dal progetto, inclusi, quando presenti, operatori prefissi, infissi e postfix.
>
> L'integrazione tra Recursive Descent Parsing e Pratt Parsing deve essere progettata in modo coerente: il Recursive Descent Parsing deve occuparsi dei costrutti sintattici di livello superiore, mentre il Pratt Parser deve essere invocato nei punti della grammatica in cui è necessario analizzare un'espressione e determinarne correttamente la struttura in base alla precedenza e all'associatività degli operatori.
>
> La gestione degli errori sintattici deve essere strutturata e fornire informazioni sufficientemente precise per individuare il punto in cui si è verificato l'errore e, quando possibile, il token o il costrutto sintattico atteso. Gli errori devono essere gestiti in modo coerente con l'architettura complessiva del parser.
>
> Il parser deve produrre una rappresentazione strutturata del codice sorgente conforme all'architettura prevista dal progetto. Se il progetto utilizza un **Abstract Syntax Tree (AST)**, il parser deve costruire correttamente l'AST rispettando la struttura sintattica determinata durante il parsing.
>
> L'implementazione deve essere modulare, leggibile, facilmente estendibile e sufficientemente testabile. Le responsabilità relative alla tokenizzazione, al parsing dei costrutti sintattici generali, al parsing delle espressioni e alla gestione degli errori devono essere mantenute il più possibile separate.
>
> Infine, l'implementazione deve rispettare integralmente la grammatica e le regole sintattiche definite dal progetto, evitando comportamenti impliciti o ambigui. In particolare, la gestione della precedenza e dell'associatività deve essere deterministica e verificabile attraverso test dedicati per i diversi livelli di precedenza e per gli operatori con associatività sinistra e destra.

Il contesto del repository indica che Dersco è un'infrastruttura di compilazione Java per il linguaggio Dersco e che il servizio del compilatore attualmente collega il controllo sintattico alla tokenizzazione; il parser è indicato come lavoro da collegare.

## Restated

Si propone di aggiungere al progetto Dersco un parser che combini parsing ricorsivo-discesa per i costrutti di alto livello e Pratt parsing per le espressioni. Il parser dovrà produrre una rappresentazione sintattica strutturata, rispettare la grammatica del linguaggio e fornire errori sintattici precisi, con precedenza e associatività degli operatori verificabili tramite test.

## Origin & Context

- **Raised by**: utente che ha fornito la richiesta.
- **Trigger**: [NEEDS CLARIFICATION: quale esigenza o evento ha motivato l'introduzione del parser ora.]

## First-Glance Unknowns

- [NEEDS CLARIFICATION: dove è definita la grammatica completa e quali costrutti sintattici deve coprire inizialmente.]
- [NEEDS CLARIFICATION: quali tipi di espressione e quali operatori prefissi, infissi e postfix sono previsti, con precedenza e associatività.]
- [NEEDS CLARIFICATION: quale modello AST esiste già o quale rappresentazione strutturata è attesa dal progetto.]
- [NEEDS CLARIFICATION: quali errori sintattici, codici e strategie di recupero devono integrarsi con il modello di errori esistente.]
- [NEEDS CLARIFICATION: quali confini tra lexer, parser e servizio compilatore sono attesi per l'integrazione iniziale.]
