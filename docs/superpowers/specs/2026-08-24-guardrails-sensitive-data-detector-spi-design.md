# Guardrails Sensitive-Data Detector SPI — Design (span-based redaction in `platform-ai-guardrails`)

- **Date:** 2026-08-24
- **Branch:** `claude/opennlp-guardrails-integration-32614d`
- **Status:** Accepted — the two forks put to the user during the brainstorm were answered (§11 records
  both, plus the three decisions this document made on its own and the evidence for each).
- **Ticket:** none filed yet.
- **Origin:** a spike into `github.com/ultramancode/spring-ai-privacy-guardrails`, asking whether its
  Apache OpenNLP integration could be reused here. §2 records the spike's finding, which is why this
  spec introduces **no OpenNLP dependency at all**.
- **Related:** `2026-07-31-ai-guardrails-standalone-design.md` (the extraction that created this
  module), `.agents/ai-guardrails.md` and `.agents/ai-gateway-guardrails.md` (the surfaces
  this touches).

## 1. Summary

`AiGuardrails` detects sensitive data with a **sequential chain of `String.replaceAll` calls**:
`redactAll(x)` is `redactSecrets(redactPii(x))`, and inside each half the patterns run one after
another over the *already-partly-redacted* output of the previous one. Detection and replacement are
the same step, and the text mutates underneath the scan.

This spec replaces that chain with a three-stage pipeline — **detect → resolve → apply** — behind a
published SPI, `SensitiveDataDetector`, that other modules can contribute to.

Three things make this more than an internal tidy-up:

1. **The current chain leaks partial secrets.** Because `CREDIT_CARD_PATTERN` runs before the secret
   patterns and rewrites the text they were going to match, a secret containing a 16-digit run is
   redacted only in its middle, and its identifying prefix survives into the model call (§3). This is
   a live correctness bug, empirically reproduced, not a hypothetical.
2. **The streaming path has no way to cover a non-regex detector**, and would silently appear to
   (§6). Closing that gap needs a declaration on the SPI, so it is a design decision rather than an
   implementation detail.
3. **It publishes an extension point.** `SensitiveSpan` and `SensitiveDataDetector` land in
   `platform-ai-guardrails-api`, which distributed EE apps carry without `-service` — the module
   boundary this repo has been bitten on before (`ResourceVisibilityPolicyRegistry`).

## 2. Why this exists: the OpenNLP spike, and what it concluded

The originating question was whether ByteChef could adopt the OpenNLP PII analyzer from
`spring-ai-privacy-guardrails`. The investigation found:

- The entire OpenNLP integration there is **two classes, ~150 lines** — `OpenNlpPiiAnalyzer` and
  `OpenNlpEntityModel`. It tokenizes with `SimpleTokenizer.tokenizePos`, runs one `NameFinderME` per
  configured model, and maps the returned *token-index* spans back to *character* offsets.
- **It ships no models.** `spring.ai.privacy.opennlp.entity-models` is empty by default and the
  project's own documentation describes the module as being for "applications that already own
  compatible NER models."
- **Apache OpenNLP no longer distributes NER models.** Maven Central carries
  `opennlp-models-sentdetect-*` and `opennlp-models-tokenizer-*` and **zero** NER artifacts; the
  official models page lists language detection plus 36 sentence/token/lemma/POS models and no named
  entity recognition. The only English NER models in existence are the legacy 1.5 SourceForge
  binaries — English-only, newswire-trained, roughly fifteen years old.

So "reuse it" resolves to adopting an adapter for models nobody distributes. The reusable idea was
never the OpenNLP adapter; it was the **span-producing analyzer SPI underneath it**. That is what
this spec builds.

OpenNLP itself remains a plausible *later* detector behind this SPI — operator-supplied models,
off by default, in its own module. It is explicitly out of scope here (§9).

## 3. The bug this fixes

`redactAll` = `redactSecrets(redactPii(content))`. PII patterns rewrite the text before any secret
pattern is tried. When a secret's body contains a 16-digit run, `CREDIT_CARD_PATTERN` claims it
first and destroys the text the secret pattern needed.

Reproduced against the live patterns copied out of `AiGuardrails`:

| input | today's `redactAll` | span-based |
|---|---|---|
| `sk-proj-1234567890123456` | `sk-proj-[REDACTED_CC]` | `[REDACTED_SECRET]` |
| `xoxb-1234567890123456-abcdef` | `xoxb-[REDACTED_CC]-abcdef` | `[REDACTED_SECRET]` |

In both rows today's output **discloses that a Slack/OpenAI credential was present and leaks its
prefix and suffix** to the model. Five further probes (plain credit card, email, JWT, a long
`sk-proj-` key, and a mixed email/IP/phone line) were byte-identical between the two engines.

The general statement: today's output depends on **which pattern ran first**; the new output depends
only on the spans themselves. That is the property the whole refactor is for, and it is what makes a
contributed detector safe to add later — its result cannot be silently corrupted by a detector that
happens to run before it.

## 4. Goals / non-goals

**Goals**

- Replace the sequential `replaceAll` chain with detect → resolve → apply over the original text.
- Publish `SensitiveDataDetector` / `SensitiveSpan` in `-api` as a bean-contributed extension point.
- Fix the partial-secret leak of §3.
- Make the streaming path's detector coverage explicit rather than silently partial.
- Preserve every existing placeholder string exactly.

**Non-goals**

- No OpenNLP, ONNX, or Presidio dependency. No new third-party dependency of any kind.
- No new user-visible settings, GraphQL fields, or UI. No persisted state, no migration.
- No change to blocked-terms, injection detection, moderation, or `BlockingMode` semantics.
- No change to `AiGuardrailsAdvisor`'s advisor ordering or to the workspace/global policy union.

## 5. The types (`platform-ai-guardrails-api`)

The module holds three types today and has **zero dependencies** — its `build.gradle.kts` declares
only `testImplementation` entries, so not even Spring is on its compile path. These additions keep it
that way: a two-value enum, a record, and an interface, none of which need a framework.

```java
public enum SensitiveKind { PII, SECRET }

public record SensitiveSpan(
    SensitiveKind kind, String category, int start, int end, double confidence) { ... }

public interface SensitiveDataDetector {

    String name();

    List<SensitiveSpan> detect(String text);

    default boolean streamSafe() {
        return true;
    }
}
```

### 5.1 Two axes, and why not one

`kind` is a **closed** two-value enum because it maps exactly onto the two independent policy toggles
that already exist — `redactPii` and `redactSecrets`, on both `AiGuardrailsWorkspaceSettings` and the
gateway's `AiGatewayProjectSettings`. Anything more open here would leave those toggles unable to
decide which spans they govern.

`category` is an **open**, validated string (`^[A-Z][A-Z0-9_]*$`) whose placeholder is derived as
`"[REDACTED_" + category + "]"`.

That derivation reproduces all six of today's placeholders exactly, with no lookup table:

| category | derived placeholder | matches today |
|---|---|---|
| `EMAIL` | `[REDACTED_EMAIL]` | yes |
| `SSN` | `[REDACTED_SSN]` | yes |
| `CC` | `[REDACTED_CC]` | yes |
| `PHONE` | `[REDACTED_PHONE]` | yes |
| `IP` | `[REDACTED_IP]` | yes |
| `SECRET` | `[REDACTED_SECRET]` | yes |

A closed category enum was rejected: it would force an edit to `-api` — a published module — for
every new entity type a contributed detector wants to emit. Under the open form, a future OpenNLP
detector emits `kind=PII, category="PERSON"` and gets `[REDACTED_PERSON]` with no change to `-api` at
all.

`confidence` is carried now, at `1.0` for the regex detectors. It is unused by resolution today
(§6.2) and exists so that adding a probabilistic detector later is not a record-signature change
rippling through every call site.

## 6. The pipeline

### 6.1 Detect

Each registered detector scans the **original** text and returns candidate spans. Detectors never see
each other's output — that is the entire point.

Two built-ins ship, holding today's patterns verbatim:

- `RegexPiiDetector` — `EMAIL`, `SSN`, `CC`, `PHONE`, `IP`; `kind = PII`.
- `RegexSecretDetector` — the nine developer-secret shapes; `kind = SECRET`.

Splitting the existing single pattern set into two detectors mirrors the `redactPii` / `redactSecrets`
split that already exists in the policy layer, so each is independently toggleable without filtering
inside one detector.

### 6.2 Resolve

Candidates are sorted by a **total** order and accepted greedily, rejecting any span that overlaps one
already accepted:

1. `kind` severity — `SECRET` before `PII` *(the decided rule, §11 D1)*
2. length, descending
3. `start`, ascending
4. `category`, ascending

The order is total, which is what makes bean-injection order irrelevant to the output. Spring's
`List<SensitiveDataDetector>` iteration order cannot affect a single character of the result, so no
`@Order` annotation, no `Ordered` interface, and no registration-sequence documentation is needed —
and a detector added later cannot perturb existing results except where it genuinely wins an overlap.

The length-descending tiebreak also reproduces, by construction, the one ordering property the old
chain got right: a nested match loses to its enclosing match. A JWT inside a PEM private-key block is
today swallowed by the PEM pattern because PEM is first in `SECRET_PATTERNS`; under the new rule it is
swallowed because PEM is longer. Same output, for a reason that does not depend on list position.

`confidence` is deliberately **not** a tiebreak. With only regex detectors every span scores `1.0`, so
including it would be dead weight that also invites a future detector to win overlaps by inflating its
own score. Revisit when a second detector actually exists.

### 6.3 Apply

Accepted spans are applied **right-to-left** (descending `start`) so that each replacement leaves the
offsets of the not-yet-applied spans valid. A left-to-right implementation would need offset
bookkeeping after every substitution; right-to-left needs none.

### 6.4 The three public entry points

All three run **one** detect pass and differ only in which kinds they keep:

- `redactAll(text)` — both kinds.
- `redactPii(text)` — `PII` only.
- `redactSecrets(text)` — `SECRET` only.

**The kind filter is applied to the candidates, before resolution — not to the accepted set after it.**
This is not a free choice. Filtering after resolution would let a span the caller did not ask for
*consume* an overlap and then be discarded, so `redactPii("xoxb-1234567890123456-abcdef")` would return
the text unredacted: the `SECRET` span wins the overlap, then the `PII` filter throws it away and
nothing is left to apply. That is a redaction regression on a path the caller explicitly enabled.

Filtering first gives each toggle the maximal redaction for the kinds it governs, which preserves
today's single-toggle output byte-for-byte and confines the §3 change to `redactAll` (and to
`redactPiiAndSecrets` with both toggles on) — exactly the claim §10 relies on.

A side effect worth naming: `redactAll` today scans the text fourteen times (five PII patterns then
nine secret patterns, each over a freshly-allocated intermediate string). The new path scans once per
detector and allocates one result.

## 7. Streaming

`AiGuardrails.sensitiveMatchRanges` becomes `detectCandidates`, returning **candidates, not winners** —
the pre-resolution set.

This matters and is easy to get wrong. `StreamingResponseRedactor`'s safe-cut pull-back
(`StreamingResponseRedactor.java:80`) must consider every span that *could* match, including overlap
losers. A losing span still occupies characters; if the cut landed inside one and the pull-back ignored
it, the emitted segment would be redacted in isolation differently from how the whole buffer would be.
The existing loop already iterates all ranges rather than a filtered set, so this is a rename, not a
logic change — but it is load-bearing and gets a comment saying so.

The class documents an invariant: `concat(every push) + flush == redactAll(whole stream)`. It survives,
because redaction is now a pure function of the span set over a segment and no accepted span crosses a
safe cut. §8 pins it with a test rather than trusting the argument.

### 7.1 `streamSafe()` — closing a gap rather than inheriting it

A regex detector is *local*: whether a span matches at position `i` depends only on nearby characters,
so scanning a buffered window gives the same answer as scanning the whole document. A sentence-level
detector — the NER case this SPI exists to enable — is not local. Feeding it a 512-character window
mid-sentence produces different, worse answers than feeding it the complete text.

Without a declaration, such a detector would be registered, would visibly cover the batch path, and
would silently contribute nothing usable on the streaming path. That is precisely the
"appears covered but isn't" failure this design is meant to avoid.

So: `streamSafe()` defaults to `true`, the streaming redactor uses only stream-safe detectors, and it
logs **once per instance** when it skips one. Both regex built-ins are stream-safe, so today the set is
unchanged.

## 8. Failure policy — fail open, per detector

A detector that throws is caught, WARN-logged, recorded as `detector_failed`, and the remaining
detectors continue.

`AiGuardrailMetrics.record(String event)` takes a free-form event string and the counter is already
tagged by surface, so this needs no metrics change.

### 8.1 The metric is best-effort, and that is not a detail

Recording goes through whichever `AiGuardrailMetrics` instance the call already carries —
`redactPiiAndSecrets` takes a `@Nullable AiGuardrailMetrics`, and the bean itself is
`@ConditionalOnProperty(bytechef.ai.gateway.enabled=true)` on top of `@ConditionalOnEEVersion`.

So there are real paths with **no metrics instance at all**: a gateway-disabled deployment, and the
filter-only entry points (`redactAll` / `redactPii` / `redactSecrets`) which take no metrics argument.
On those paths a detector failure is logged and **not counted**.

This is worth stating because §8's residual risk leans on the metric as the signal, and on those paths
the log line is the only signal there is. Widening the metrics bean's condition is out of scope here —
it would change when the counter exists for every other guardrail event too — but it is the obvious
follow-up if detector failures ever become operationally interesting.

### 8.1a As built — the gaps in §8.1 were closed in a follow-up

§8.1 described `detector_failed` as best-effort on the response-direction and streaming paths, and
argued that widening the metrics bean's condition was out of scope. The final whole-branch review
accepted that reasoning for the merge but flagged the mislabelling half as the worse one — a
`detector_failed` from an `ai_hub` response scan surfacing as `surface=gateway` points an operator at
the wrong surface, and wrong telemetry is worse than absent telemetry.

It was then closed without widening the bean's condition at all, which turned out to be the smaller
change: `scanResponseText`, `redactAll` and `newStreamingResponseRedactor` each gained an overload
taking an `@Nullable AiGuardrailMetrics`, and `AiGuardrailsAdvisor` passes the per-surface instance it
already held for `response_redacted`. `StreamingResponseRedactor` takes one through its constructor
instead of passing literal `null`.

The gateway's own paths still record through the engine's constructor-injected bean, deliberately:
those callers ARE the gateway, so its fixed `surface=gateway` tag is accurate for them and the
`bytechef.ai.gateway.enabled` gate costs nothing.

`.agents/ai-guardrails.md`'s Metrics section is the current description; treat §8.1 above as the
design-time record of why the gaps existed, not as a statement about the code today.

### 8.2 Rationale and residual risk

Fail-open matches the module's documented posture — injection detection and moderation both fail open —
and a model-loading detector's transient failure must not take down every AI surface in the product.
The built-in regex detectors cannot throw in practice, so today this path is unreachable.

**Residual risk, stated plainly:** a failed detector means content it would have redacted proceeds
unredacted, and per §8.1 that may be visible only in a log line. If a future detector's failure rate
makes this unacceptable, the fix is a per-detector `failClosed()` declaration on the SPI, not a global
flip — a global fail-closed would let one flaky detector block all traffic.

## 9. Call-site changes

| Site | Change |
|---|---|
| `AiGuardrails` | injects `List<SensitiveDataDetector>`; the three `redact*` methods become instance methods; `redactPiiAndSecrets` performs one detect pass instead of two chained passes |
| `StreamingResponseRedactor` | receives detectors via constructor; both construction sites (`AiGuardrails.java:308`, `AiGatewayGuardrails.java:352`) already have access |
| `AiGatewayGuardrails` | the three `static` delegates at lines 361–378 are **deleted**; lines 285, 405 and 415 become instance calls on the `aiGuardrails` field the class already holds and already uses at line 282 |

### 9.1 Why deleting the static delegates is safe

`AiGatewayGuardrails.redactPii` / `.redactSecrets` / `.redactAll` are called from exactly one place in
the repository: `AiGatewayGuardrailsTest`. There is no production caller. They are dead public API,
and removing them is why the move from static to instance costs almost nothing.

Verified by search across `server/` for `AiGatewayGuardrails.redact`.

## 10. Testing

**No existing test expectation changes.** The redaction tests in `AiGuardrailsTest` (lines 42–90) use
`contains` / `doesNotContain` on non-overlapping fixtures and exercise `redactPii` and `redactSecrets`
*individually*, so the §3 behavior change — which only manifests in `redactAll` over a secret
containing a digit run — is not covered by anything today. The tests are kept as-is and serve as the
regression guard that the common path is byte-identical.

New tests:

- **Resolution** — overlap precedence (secret beats PII), nesting (outer beats inner), adjacency
  (touching but non-overlapping spans both survive), and a determinism test that shuffles the detector
  list and asserts identical output.
- **The §3 cases** — `sk-proj-1234567890123456` and `xoxb-1234567890123456-abcdef` pinned to the new
  whole-secret output, with a comment recording what the old chain produced and why it was wrong.
- **Streaming equivalence** — over a corpus containing values straddling chunk boundaries, assert
  `concat(pushes) + flush == redactAll(whole)` for a range of window sizes and chunk splits.
- **Streaming skip** — a non-`streamSafe` fake detector contributes to `redactAll` but not to the
  streaming path, and the skip is logged once.
- **Fail open** — a throwing fake detector leaves the other detectors' redactions intact and increments
  `detector_failed`.
- **Contribution** — a fake detector bean emitting a novel category (`kind=PII, category="PERSON"`)
  produces `[REDACTED_PERSON]`, proving the SPI end to end without any new dependency.

One residual to cover: two *partially* overlapping same-kind spans (neither nested) resolve by length
under the new rule versus by list position under the old. No such pair exists among the current
patterns, but the test suite pins the rule so a future pattern addition cannot change it by accident.

## 11. Decisions

| # | Decision | Who | Rationale |
|---|---|---|---|
| D1 | On overlap, **secrets win** — spans computed against the original text, not bug-for-bug preservation of the chain | **User**, chosen from three options | Preserving the chain would bake order-dependence into the SPI permanently, which is the thing being refactored away. Fixing it also closes the §3 partial-secret leak. |
| D2 | **Open SPI in `-api`, bean-contributed** — not an internal seam, not a halfway "types published, wiring deferred" | **User**, chosen from three options | A future detector module depends only on `-api` + its own library. Matches the repo's documented "`-api` holds SPI wiring" rule and the `ResourceVisibilityPolicyRegistry` precedent. |
| D3 | Detection becomes **instance-based**; the `static` redaction API is removed rather than kept behind a static registry | This document | Evidence, not preference: `AiGuardrails` is already injected as a bean at every production site, `AiGatewayGuardrails` already holds an instance, and the static delegates have no production caller (§9.1). A static holder would have been the more invasive choice. |
| D4 | Two axes — closed `SensitiveKind`, open `String category` | This document | The closed kind is required by the two existing policy toggles; the open category avoids an `-api` edit per entity type and reproduces all six current placeholders by derivation (§5.1). |
| D5 | **Fail open** per detector, with a distinct `detector_failed` metric | This document | Consistent with the module's existing injection/moderation posture; a global fail-closed would let one flaky detector block all AI traffic (§8). |

## 12. Open questions

None blocking. Two things are deliberately deferred rather than unresolved:

- **A `failClosed()` per-detector declaration** — not added until a detector exists whose failure rate
  justifies it (§8).
- **`confidence` as a resolution input** — carried but unused until a probabilistic detector exists
  (§6.2).
