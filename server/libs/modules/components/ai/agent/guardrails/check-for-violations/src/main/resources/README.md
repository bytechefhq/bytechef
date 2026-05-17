# Check For Violations

`CheckForViolations` is the parent cluster element that runs rule-based and LLM-based guardrails over
an input text and aggregates their verdicts. Attach one or more guardrail children (Keywords, PII,
Secret Keys, URLs, Custom Regex, Jailbreak, NSFW, Topical Alignment, Custom, LLM PII) plus — if any
LLM-based child is present — a single shared `MODEL` child. The resolved `ChatClient` is injected
into every child via `GuardrailContext.chatClient()` (returns `Optional<ChatClient>`), so
individual LLM guardrails no longer declare their own model.

## Preflight stage

Rule-based guardrails (PII, Secret Keys, URLs, Custom Regex, Keywords) run in a preflight stage.
The masking variants (PII, Secret Keys, URLs, Custom Regex) record a violation and add their
detected spans to a shared entity map; the merged map is applied as a single longest-first pass
after every preflight check has run.

Keywords participates in this same PREFLIGHT pass but does not mask — it only emits violations.
Running Keywords at PREFLIGHT against the raw input is deliberate: a deny-list entry that
overlapped with PII / secret / URL substrings would otherwise be replaced with a placeholder
before the keyword check ever saw it.

LLM-based guardrails (Jailbreak, NSFW, Topical Alignment, Custom, LLM PII) then run against the
*masked* text from the preflight pass. This prevents PII from being sent to the classifier model
while still giving it enough context to judge prompt-injection, safety, and on-topic-ness
signals.

If you want raw text to reach the LLM overall, remove the rule-based checks from
`CheckForViolations` and leave them only in `SanitizeText`.

## Aggregation semantics

Every child runs, even after a sibling flags a violation — the advisor does not short-circuit on the
first hit. The returned `List<Violation>` captures matches, classifier scores, and any execution
failures side by side so the caller can report all findings at once. When a check cannot run (LLM
outage, missing `MODEL`, bad configuration), a `Violation.ofExecutionFailure(...)` is emitted
alongside the rest and the advisor fails closed.
