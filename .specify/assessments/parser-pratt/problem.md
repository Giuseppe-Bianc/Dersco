# Problem Definition: Validazione sintattica del linguaggio Dersco

- **Slug**: parser-pratt
- **Created**: 2026-08-11
- **Inputs used**: intake.md, research.md

## Problem Statement

Gli sviluppatori che lavorano con Dersco non dispongono oggi di una validazione sintattica completa del sorgente: il controllo esposto dal compilatore arriva alla tokenizzazione, mentre struttura dei costrutti, raggruppamento degli operatori e corrispondenza con la grammatica non risultano verificati. Il problema è rilevante ora perché il repository contiene già modelli AST e diagnostici sintattici, ma la ricerca non ha trovato una grammatica normativa né un livello di analisi sintattica collegato.

## Affected Users & Stakeholders

- **Users**: [NEEDS CLARIFICATION: sviluppatori che scrivono o verificano programmi Dersco] — ricevono un controllo incompleto della correttezza strutturale del codice e non hanno una rappresentazione sintattica verificata.
- **Stakeholders**: [NEEDS CLARIFICATION: manutentori e proprietari del compilatore Dersco] — devono poter stabilire la correttezza della sintassi e l'affidabilità delle diagnosi prima delle fasi successive.
- **Stakeholders**: [NEEDS CLARIFICATION: eventuali strumenti o pipeline che consumano l'AST] — dipendono da una struttura sintattica coerente, ma non sono identificati nell'intake o nella ricerca.

## Goals

- Rendere verificabile la conformità dei programmi Dersco alla grammatica autorevole, incluse le espressioni e il raggruppamento deterministico degli operatori.
- Ottenere una rappresentazione sintattica strutturata coerente con le forme del linguaggio già modellate nel repository.
- Rendere gli errori sintattici localizzabili e distinguibili dagli errori lessicali, con indicazione dell'atteso quando la grammatica lo consente.
- Rendere misurabile la copertura dei costrutti e delle regole di precedenza/associatività tramite casi di validazione ripetibili.

## Non-Goals

- Scegliere in questa fase un algoritmo, un'API o una decomposizione di classi: sono decisioni di soluzione da valutare dopo la definizione del problema.
- Implementare analisi semantica, type checking, generazione IR, generazione assembly o altri comportamenti successivi al parsing; la ricerca li colloca oltre il controllo sintattico corrente.
- Ampliare o modificare la grammatica senza una fonte normativa concordata.
- Valutare domanda di mercato, adozione o priorità economica oltre le evidenze disponibili nell'intake e nella ricerca.

## Success Metrics

- Percentuale delle produzioni della grammatica concordata coperte da casi di accettazione e rifiuto — (baseline: grammatica e corpus non disponibili).
- Percentuale dei casi di espressione con precedenza e associatività definite che producono il raggruppamento atteso — (baseline: 0% verificato nel servizio attuale; corpus atteso: unknown).
- Percentuale degli input sintatticamente non validi del corpus concordato per cui viene riportato uno span e, quando definito, il costrutto atteso — (baseline: unknown; corpus atteso: unknown).
- Percentuale dei programmi validi del corpus concordato per cui è disponibile una rappresentazione AST coerente — (baseline: unknown, perché il servizio corrente non collega un parser).
- Valutazione qualitativa dei manutentori sulla chiarezza e utilità delle diagnosi — (baseline: [NEEDS CLARIFICATION: metodo e soglia di valutazione]).

## Cost of Inaction

Se il problema resta irrisolto, il controllo corrente continuerà a fermarsi alla tokenizzazione: errori nella struttura del programma o nel raggruppamento degli operatori potranno non essere individuati in questa fase, e le strutture AST già presenti non avranno una fonte sintattica verificata. Il costo concreto in termini di difetti, tempo di diagnosi e utenti coinvolti non è quantificabile con le evidenze disponibili.

## Open Questions

- [NEEDS CLARIFICATION: qual è la fonte autorevole e versionata della grammatica Dersco?]
- [NEEDS CLARIFICATION: quali utenti e stakeholder devono essere considerati prioritari?]
- [NEEDS CLARIFICATION: quali costrutti rientrano nella prima versione del perimetro sintattico?]
- [NEEDS CLARIFICATION: quali livelli di precedenza e regole di associatività sono normativi, inclusi gli assegnamenti?]
- [NEEDS CLARIFICATION: quali nodi AST esistenti sono richiesti come output per il perimetro iniziale?]
- [NEEDS CLARIFICATION: quali strategie di recupero e quali informazioni sull'atteso sono richieste per gli errori sintattici?]
- [NEEDS CLARIFICATION: quale corpus di programmi validi e invalidi rappresenta il comportamento atteso?]
- [NEEDS CLARIFICATION: quale soglia rende accettabile la qualità delle diagnosi per i manutentori?]
