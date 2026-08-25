# Guardrails OpenNLP Detector — Design (operator-supplied NER behind the detector SPI)

- **Date:** 2026-08-24
- **Branch:** `claude/opennlp-guardrails-integration-32614d`
- **Status:** Accepted — the two forks put to the user were answered (§9 records both, plus the five
  decisions this document made on its own).
- **Ticket:** none filed yet.
- **Depends on:** `2026-08-24-guardrails-sensitive-data-detector-spi-design.md`, which published the
  `SensitiveDataDetector` SPI this module implements. That work is complete and merged into this
  branch; this spec adds nothing to it.
- **Related:** `.agents/ai-guardrails.md` (engine and SPI reference).

## 1. Summary

The guardrails engine detects **structured** sensitive data — email, SSN, credit card, phone, IPv4,
and nine developer-secret shapes — by regular expression. It cannot detect **unstructured** PII:
person names, organizations, locations. No regex ever will.

This spec adds an optional module, `platform-ai-guardrails-opennlp`, that closes that gap with
Apache OpenNLP named-entity recognition behind the existing SPI. It ships **no models**: the adapter
and its configuration surface only, off by default, with models supplied by the operator.

The honest framing, stated once here and not softened later: **this module does nothing until an
operator points it at model files, and Apache distributes none.** §2 explains why that is still the
right shape, and §8 is explicit about who this is and is not for.

## 2. Why ship an adapter with no models

The originating spike (recorded in the SPI spec's §2) established:

- Apache OpenNLP's Maven Central presence contains `opennlp-models-sentdetect-*` and
  `opennlp-models-tokenizer-*` and **zero** NER artifacts.
- The official models page lists language detection plus 36 sentence/token/lemma/POS models, and no
  named-entity recognition.
- The only English NER models in existence are the legacy 1.5 SourceForge binaries — English-only,
  newswire-trained, roughly fifteen years old.

Three alternatives were considered and rejected (§9, D1):

- **Bundling the legacy 1.5 models** would give a working default at the cost of ByteChef vouching
  for provenance and licensing nobody audited, and of shipping a newswire-trained model into a
  destructive, pre-model redaction path where a spurious `PERSON` hit silently corrupts a
  developer's prompt.
- **`opennlp-dl` with an ONNX transformer** is the only option that would work *well* on prompt text,
  but costs a ~400MB model, `onnxruntime` native libraries — the first native dependency in this
  repository — and per-inference latency on the request path. It also undercuts its own premise: at
  that point OpenNLP is a thin ONNX wrapper and binding `onnxruntime` directly to the SPI would be
  simpler.
- **Building nothing** was a live option and remains defensible. It was rejected because the adapter
  is small, self-contained, and leaves the guardrails engine untouched (§3 lists the three files
  outside it that do change), so its carrying cost is close to zero while it is switched off — and
  because operators who already own compatible models (an
  organization that trained NER on its own corpus is the realistic case) currently have no way to
  plug them in at all.

## 3. Goals / non-goals

**Goals**

- Detect unstructured PII via operator-supplied OpenNLP name-finder models.
- Ship the adapter and configuration only — no model files, no network fetch at runtime.
- Fail loudly at startup on a misconfigured model, rather than degrading to silent non-coverage.
- Keep false positives controllable, because redaction here is destructive and happens before the
  model call.
- Add the module without touching the guardrails engine. Nothing in `-api`, `-service`, the engine,
  or any existing detector changes — that property is the SPI's central claim and this module is its
  first real test of it.

  Three files outside the module do change, and none is an SPI concern: `settings.gradle.kts` and
  `gradle/libs.versions.toml` (unavoidable for any new Gradle module), and
  `ApplicationProperties` (§5 — Spring's central strict binder, unavoidable for any operator-settable
  `bytechef.*` key). An earlier draft of this spec claimed "zero existing files"; that was wrong on
  the third, and the correction is worth keeping visible because the SPI's promise is about the
  *engine*, not about Spring configuration.

**Non-goals**

- No models shipped, and no model download.
- No ONNX, `opennlp-dl`, or native dependency.
- No UI, GraphQL, or persisted settings — this is operator YAML, like the other
  `bytechef.ai.guardrails.*` switches.
- No change to the SPI, the engine, the existing detectors, or `AiGuardrails`.
- No non-English support beyond whatever model the operator supplies (the adapter is
  language-agnostic; it never inspects language).

## 4. Module and dependency

```
server/ee/libs/platform/platform-ai/platform-ai-guardrails/
  platform-ai-guardrails-api/          (existing — the SPI)
  platform-ai-guardrails-service/      (existing — the engine)
  platform-ai-guardrails-graphql/      (existing)
  platform-ai-guardrails-opennlp/      (NEW)
```

The new module depends on **`platform-ai-guardrails-api` only** — not on `-service`. A detector never
sees the engine. It also needs `spring-context` and `spring-boot-autoconfigure` (for
`@ConfigurationProperties` and `Resource`), and `org.apache.opennlp:opennlp-tools`.

**Version: `2.5.11`**, declared in `gradle/libs.versions.toml` as `opennlp = "2.5.11"` and referenced
as `libs.opennlp` — the repo's convention for dependencies the Spring Boot BOM does not manage.

Two version notes worth recording, because both invite a wrong "correction":

- **Maven Central's search index lags.** It reports `2.5.9` as the newest 2.x, but
  `repo1.maven.org` serves `2.5.10` and `2.5.11`. Verified by direct HTTP: `2.5.11` returns 200.
  `2.5.11` is also the version the reference project pins as its verified baseline.
- **`3.0.0-M3` is a milestone, not a release.** Maven Central's `latestVersion` field reports it
  because it sorts newest-first without filtering pre-releases. A milestone dependency in a
  guardrail path is not acceptable; stay on the 2.x line until 3.0.0 is final.

`opennlp-tools` is pure Java with no native components (~2MB), and pulls only slf4j-api, which the
platform already carries.

## 5. Configuration

```yaml
bytechef:
  ai:
    guardrails:
      opennlp:
        enabled: false
        tokenizer-model:                 # optional Resource; SimpleTokenizer when unset
        min-confidence: 0.85
        entity-models:                   # empty by default
          PERSON: file:/opt/bytechef/models/en-ner-person.bin
          ORGANIZATION: classpath:models/en-ner-organization.bin
```

**The `guardrails` subtree must be declared in `ApplicationProperties.Ai`** —
`com.bytechef.config.ApplicationProperties` is
`@ConfigurationProperties(prefix = "bytechef", ignoreUnknownFields = false)`, and today neither
`Ai` nor `Ai.Gateway` declares a `guardrails` field.

This needs care, because the naive reading in both directions is wrong:

- It is **not** true that every standalone `@ConfigurationProperties` under `bytechef.*` must be
  mirrored. `bytechef.plan`, `bytechef.asset-file`, `bytechef.oauth2.resource-server`,
  `bytechef.approval.signed-token` and `bytechef.ai.model-catalog` are all standalone classes with no
  `ApplicationProperties` field, and they work.
- It is **also not** true that a standalone class is therefore always safe. Strict binding fires on
  keys that are **present in a property source but unbound** — so those examples survive only because
  nothing sets them in a bundled yml; they run on their defaults. The same is true of the existing
  `bytechef.ai.gateway.guardrails.*` family, which is read entirely through `@Value` defaults and set
  nowhere.

This module is different on exactly that axis: it is inert until an operator **sets**
`enabled: true`, so its keys are present-in-a-property-source by construction. Worse, the module is
**optional** — apps that do not carry it (execution-app, worker-app, …) would see those keys in a
shared configuration with no bean to bind them, and `ignoreUnknownFields = false` fails the whole
context. That failure mode has been hit before in this repo, by `bytechef.licence.*`, and was caught
only by `ServerApplicationIntTest`.

A boot failure across every app is not a risk worth running to avoid one field, so: add a
`Guardrails` nested class with an `openNlp` subtree to `ApplicationProperties.Ai`, alphabetically
placed. The module's own `@ConfigurationProperties` class may still exist for ergonomics, but the
central binder must know the subtree.

An implementation plan must include an integration-level assertion that a context boots with these
keys **set**, in an app that does not carry the module — a unit test cannot catch this class of
failure, which is why it escaped once already.

**Keys are `SensitiveSpan` categories, not a separate vocabulary.** `PERSON` produces
`[REDACTED_PERSON]` directly through `SensitiveSpan.placeholder()`, with no mapping table anywhere.
This is the SPI's open-category decision paying off exactly as intended; a closed enum would have
required an edit to the published `-api` module for every entity type an operator wants.

Category keys are validated against the SPI's `^[A-Z][A-Z0-9_]*$` grammar at startup, so a
lower-case or punctuated key is a configuration error rather than a span-construction failure at
request time.

Values are Spring `Resource` strings, so `file:`, `classpath:`, and any other registered protocol
work without the module implementing path handling of its own.

**The bean is registered only when `enabled=true` AND `entity-models` is non-empty.** An enabled but
empty configuration registers nothing rather than an inert detector, so the engine's detector list
does not gain a member that can never contribute.

## 6. The detector

One bean, `OpenNlpSensitiveDataDetector`, implementing `SensitiveDataDetector`:

- `name()` → `"opennlp-ner"`
- `detect(String)` → the spans found
- `streamSafe()` → **`false`** (§6.3)

### 6.1 Thread safety — the trap

**`TokenNameFinderModel` is thread-safe; `NameFinderME` is not.**

The SPI contract states that one detector instance serves every workspace and every concurrent
request. So the **models** are loaded once and held for the process lifetime, and a **fresh
`NameFinderME` is constructed per `detect()` call**. Construction is cheap — it wraps the already-parsed
model — and this is what the reference implementation does.

Caching the `NameFinderME` instead would be the natural-looking optimization and is wrong: its
internal state is per-document, so concurrent calls interleave and produce garbage spans at
positions that never contained an entity. No single-threaded test would catch it. A comment on the
field says so, and a concurrency test (§7) pins it.

`NameFinderME.clearAdaptiveData()` is not needed under this per-call construction, which is a second
reason to prefer it over caching: the cached form requires remembering to call it between documents,
and forgetting leaks one request's entity context into the next — a cross-request data leak in a
component whose entire job is preventing those.

### 6.2 Span mapping

```
tokenizePos(text) → Span[] with CHARACTER offsets
    ↓ covered text
String[] tokens → NameFinderME.find(tokens) → Span[] with TOKEN indices
    ↓
start = tokenPositions[entity.getStart()].getStart()
end   = tokenPositions[entity.getEnd() - 1].getEnd()
```

The `- 1` is because OpenNLP's `getEnd()` is exclusive. Getting this wrong redacts the wrong
characters — silently, and plausibly, since the output still looks redacted. Every returned entity
span is bounds-checked against the token array before use; a malformed one is treated as that
model's failure (§6.4) rather than propagating an `ArrayIndexOutOfBoundsException`.

Tokenization runs **once** per `detect()` call and the token array is shared across every model, so N
models cost one tokenization, not N.

### 6.3 `streamSafe()` returns false

NER over a 512-character sliding window that begins mid-sentence gives different — and worse —
answers than over the complete text. The streaming redactor therefore excludes this detector and logs
the exclusion once per engine instance.

Consequence, stated plainly so it is not discovered later: **streamed completions get regex redaction
only.** Batch response scanning and all request-direction scanning cover NER fully. This is the
scenario `streamSafe()` was added for in the SPI design; this module is its first real user.

### 6.4 Per-model isolation

Each model's `find()` call is wrapped in its own try/catch inside `detect()`. A failing model is
WARN-logged with its category and skipped; the others still contribute.

This matters because of how the engine handles detector faults: `SensitiveDataRedactor.collectSpans`
discards a detector's **entire batch** when `detect()` throws. That rule is correct for a malformed
span set, but here it would mean one misbehaving model silently costs every other category's spans
too. Catching per model keeps the blast radius to the model that failed.

### 6.5 Confidence threshold

`NameFinderME.probs(spans)` yields a probability per span. Spans scoring below `min-confidence` are
dropped; survivors carry their probability in `SensitiveSpan.confidence`.

This is the module's primary false-positive control, and it carries more weight than a tuning knob
normally would: redaction is **destructive and pre-model**, so a spurious `PERSON` hit on "Claude",
"Stripe", or "Redis" in a developer's prompt silently corrupts the text the model receives, with no
signal to the user. A newswire-trained model on chat and code text is exactly the input distribution
that produces those hits.

The default is `0.85`, deliberately conservative — under-detection here degrades to the status quo
(no NER coverage), while over-detection corrupts prompts.

`confidence` remains **unused by the engine's overlap resolution** (SPI spec §6.2). Thresholding is a
detector-local concern; letting confidence into the tiebreak would invite a detector to win overlaps
by inflating its own scores.

## 7. Testing without a model to test with

The module ships no model, so tests **train one in memory**. `NameFinderME.train(...)` builds a
working model from a handful of annotated `NameSample`s. The resulting model is poor at general NER
and entirely adequate for exercising the adapter.

That makes the following testable for real rather than mocked:

- **Character-offset mapping**, including a multi-token entity — the `- 1` arithmetic of §6.2, which
  is the easiest thing here to get subtly and silently wrong.
- **Category → placeholder**: a model registered under `PERSON` produces `[REDACTED_PERSON]` end to
  end through a real `SensitiveDataRedactor`.
- **Threshold behaviour**: the same input with `min-confidence` above and below the span's
  probability yields a span and no span respectively.
- **Per-model isolation**: one deliberately broken model does not suppress a working model's spans.
- **`streamSafe()` is false**, and `SensitiveDataRedactor.streamSafeView()` therefore excludes this
  detector — asserted through the real view, not by reading the flag back.
- **Startup failure**: a missing/corrupt model resource fails bean construction with a message naming
  the category and path.
- **Concurrency**: the same detector instance driven from multiple threads over texts with known
  entities returns correct spans every time — the guard for §6.1. This is a genuine test of the
  per-call `NameFinderME` decision; with a cached instance it fails.

No model file enters the repository and no test touches the network.

## 8. Who this is for, and who it is not

**It is for** an operator who already owns OpenNLP-compatible name-finder models — most realistically
one trained on their own corpus, which is also the case where accuracy is good enough for destructive
redaction.

**It is not for** an operator who wants working NER out of the box. There is no supported way to get
that today, and this module does not create one. Anyone reaching for the legacy SourceForge 1.5
models should read §2 first and understand that they are pointing a fifteen-year-old newswire model
at chat and code text, in a path that rewrites prompts irreversibly.

The documentation must say this plainly rather than presenting the module as a feature that merely
needs switching on.

## 9. Decisions

| # | Decision | Who | Rationale |
|---|---|---|---|
| D1 | **Ship no models**; operator-supplied only | **User**, chosen from four options | Avoids vouching for unaudited model provenance and licensing, avoids a ~400MB artifact and the repo's first native dependency, and matches the reference project's own conclusion. Accepts that the feature is inert until configured. |
| D2 | **Eager loading, fail fast** at startup | **User**, chosen from three options | The SPI's failure policy is deliberately fail-open, so a lazily-loaded broken model would be caught, counted, and skipped — handing the operator a guardrail that silently protects nothing. Misconfiguration is a deployment error, not a runtime condition, and the detector is off by default so only operators who opted in can trigger it. |
| D3 | One bean holding N models, with **per-model try/catch**, rather than one bean per model | This document | A bean per model would isolate failures via the engine's own per-detector fail-open, but needs dynamic registration (`BeanRegistrar`) for no gain a local try/catch does not already provide. Also avoids re-tokenizing per model. |
| D4 | **Fresh `NameFinderME` per call**, models held | This document | `NameFinderME` is not thread-safe and the SPI mandates thread-safe detectors. Also removes the `clearAdaptiveData()` obligation, whose omission would leak one request's entity context into the next. |
| D5 | **Confidence threshold in the detector**, default 0.85; `confidence` still unused by resolution | This document | The only false-positive control available on a destructive pre-model path. Keeping it out of overlap resolution preserves the SPI decision that a detector cannot win overlaps by inflating scores. |
| D6 | Register the bean only when enabled **and** at least one model is configured | This document | An enabled-but-empty configuration should contribute nothing rather than an inert detector the engine must still call. |
| D7 | Declare the `guardrails` subtree in `ApplicationProperties.Ai` rather than relying on a standalone `@ConfigurationProperties` alone | This document | Strict binding (`ignoreUnknownFields = false`) fails on keys present in a property source but unbound. This module's keys are present by construction (it is inert until `enabled: true` is set) and the module is optional, so apps without it would see the keys with no bean to bind them and fail to boot. Precedent: `bytechef.licence.*` did exactly this. |

## 10. Open questions

None blocking. Two things are deliberately deferred:

- **A models-download or bundling story.** Out of scope by D1; revisit only if a licensed,
  appropriately-trained model set becomes available.
- **Non-English tokenization.** The adapter is language-agnostic and accepts an operator-supplied
  tokenizer model, but nothing validates that the tokenizer and the name-finder models agree on a
  language. Mismatched models degrade accuracy silently. Worth a startup warning if this module ever
  sees real use.
