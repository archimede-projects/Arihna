

### 2026-09-09 — Corano: Tajwid algoritmico runtime — APPROVED / BETA RULES MILESTONE

#### Stato separato QCF V4
- Il percorso `QCF V4 Tajweed` resta **BLOCKED** in attesa di un permesso esplicito di redistribuzione offline dei relativi font/assets da parte dei titolari/provider competenti.
- Questa milestone NON modifica, sblocca, sostituisce o presume alcun diritto relativo a QCF V4, Dar Al Maarifah o asset proprietari equivalenti.
- Nessun font o immagine Tajwid proprietario deve essere incluso nell'APK per questa milestone.

#### Obiettivo di questa milestone
- Introdurre una modalità `Ḥafṣ Tajwid` basata su **testo Uthmani già redistribuito legalmente nell'app** e su riconoscimento algoritmico locale delle regole, senza dipendenza runtime da font/immagini Tajwid proprietari.
- La colorazione è una sovrapposizione visiva: il testo coranico sorgente NON deve essere alterato, normalizzato, riscritto o corretto dal motore Tajwid.
- La prima iterazione è Hafs. Warsh potrà essere esteso solo dopo verifica dedicata delle regole e dell'ortografia della riwaya; nessun matching Hafs deve essere riutilizzato implicitamente su Warsh.

#### Valutazione primaria: fcat97/tajweedApi
- Repository verificato: `fcat97/tajweedApi`.
- Pin di riferimento valutato: release `2.1.1`, commit `8fe7057bede84595bec8907407be71ec4a6024b1`; `main` osservato a `460127a2b6cd248f2a0a24a388b1ec2c489dcd93`.
- Licenza: MIT; l'avviso di copyright/licenza deve essere mantenuto negli attribution/NOTICE se codice o logica sostanziale viene incorporata/distribuita.
- Limite tecnico esplicito upstream: l'implementazione `IndoPakTajweedApi` dichiara di funzionare sul formato IndoPak/IndoQuran, differente dal Saudi/Uthmani usato da Arihna.
- Copertura upstream osservata: qalqalah, qalqalah in stop, ikhfa, iqlab, idgham con ghunnah, idgham senza ghunnah, ghunnah obbligatoria; non è una copertura completa di tutte le regole tradizionali.
- Conseguenza: NON è consentito applicare ciecamente il parser IndoPak al testo Tanzil Uthmani e presentarlo come corretto. Arihna può riusare/derivare la logica MIT solo tramite un adapter Uthmani verificato con fixture e test specifici.

#### Quran Foundation `text_uthmani_tajweed`
- L'endpoint è riconosciuto come QF Content e restituisce testo Uthmani con tag Tajwid inline.
- I Developer Terms QF correnti alla verifica del 2026-09-09 impongono per il normale QF Content un limite di caching/storage di 1 settimana, salvo permesso esplicito o disponibilità tramite Content Sync.
- Content Sync elenca come gruppi offline `mushafs`, traduzioni, word-by-word, tafsir, recitazioni e articoli; non concede in modo esplicito un dataset offline permanente separato per `text_uthmani_tajweed` ottenuto dal normale endpoint.
- Pertanto `text_uthmani_tajweed` NON deve essere incorporato come dataset persistente/offline nell'APK in questa milestone senza ulteriore permesso o un percorso Content Sync che lo restituisca esplicitamente con i termini richiesti.
- Può restare una fonte di confronto/verifica di sviluppo entro i termini applicabili, non la sorgente redistribuita della feature offline.

#### Architettura richiesta
- Introdurre un motore locale separato, testabile e puro, ad esempio `UthmaniTajwidEngine`, che riceve il testo Uthmani immutato e restituisce solo intervalli `start/end + rule`.
- Il rendering Compose applica `AnnotatedString`/span di colore agli intervalli; il testo sottostante deve essere byte/Unicode-identico alla sorgente Quran già presente.
- Le regole/colori devono avere identificatori stabili e una legenda accessibile.
- Gli overlap devono essere risolti deterministically; nessun crash o indice fuori range su combining marks, waqf signs o caratteri Uthmani.
- Il motore non deve richiedere rete ed è eseguito on-device.
- Nessuna dipendenza da JitPack è obbligatoria: se l'artefatto upstream non è Uthmani-safe, è preferibile incorporare un adapter/derivazione minimale MIT con attribution e test, invece di importare una libreria non compatibile con il corpus.

#### Identità ed esperienza UI
- Mantenere `HAFS`, `HAFS_TAJWID`, `WARSH` come identità di edizione/rendering separate secondo la SPEC precedente.
- `HAFS_TAJWID` in questa milestone deve essere descritto visibilmente come `Colorazione tajwid · Beta` oppure `Regole principali`, mai come Mushaf Tajwid tradizionale completo.
- La UI deve includere una spiegazione breve che la colorazione è generata algoritmicamente e può non coprire tutte le regole.
- Il colore non deve compromettere contrasto, leggibilità RTL, zoom, gesture, fullscreen, bookmark, ultima posizione, selezione Sura/Juz/Hizb e safe-area/cutout già approvati.
- Il pulsante `Lettura` e i fix Galaxy S25 esistenti restano requisiti di non-regressione.

#### Regole minime beta e correttezza
- Per il primo candidato, implementare solo regole che possano essere descritte e testate deterministicamente sul corpus Uthmani: `qalqalah`, `ikhfa`, `iqlab`, `idgham con ghunnah`, `idgham senza ghunnah`, `ghunnah su nun/mim mushaddad`; eventuale stop-qalqalah solo se verificato sulle fixture Uthmani.
- Ogni regola deve avere fixture positive e negative con testo Uthmani reale.
- Il gate deve verificare almeno: nessun cambiamento del testo; range validi e non vuoti; nessun overlap non risolto; determinismo; matching su fixture note; assenza di matching su fixture negative.
- Se una regola non è affidabile sul corpus Uthmani, deve essere esclusa dalla release beta invece di essere colorata approssimativamente.

#### Dati/licenze e attribution
- Testo Uthmani: continuare a rispettare la licenza/provenienza Tanzil già usata nell'app (CC BY 3.0) e le relative attribution.
- Logica fcat97 eventualmente derivata: MIT, con copyright/licenza conservati in NOTICE/credits.
- Quran Foundation: nessun contenuto `text_uthmani_tajweed` persistente oltre i termini consentiti e nessuna inclusione offline nell'APK in questa milestone.
- QCF V4: resta BLOCKED e totalmente separato.

#### Test e gate del candidato
- Il runtime candidate deve essere **un singolo commit figlio diretto di questa SPEC**. Se fallisce, ogni correzione successiva deve essere un nuovo sibling dalla stessa SPEC, non figlio del candidato fallito.
- Aggiungere unit test JVM per il motore Uthmani e instrumentation test per selezione `Ḥafṣ Tajwid`, disclaimer beta, RTL/rendering, zoom/fullscreen/bookmark e regressioni Galaxy S25.
- Eseguire il gate exact-SHA già definito dal progetto su API 28 e API 36, static checks e build release.
- Solo un candidato all-green può fare fast-forward non forzato di `main`.
- Dopo il fast-forward: build APK con signer persistente, prerelease, riscaricamento e verifica SHA-256 + signer prima di fornire il link.

#### Criterio di comunicazione
- Non dichiarare mai `Mushaf Tajwid completo` per questa milestone.
- Etichetta obbligatoria finché la copertura non è formalmente completa e verificata: **`Colorazione tajwid · Beta / regole principali`**.
- QCF V4 deve continuare a risultare `BLOCKED` fino a prova documentale di redistribuzione offline.
