# Check For Violations

`CheckForViolations` is the parent cluster element that runs rule-based and LLM-based guardrails over
an input text and aggregates their verdicts. Attach one or more guardrail children (Keywords, PII,
Secret Keys, URLs, Custom Regex, Jailbreak, NSFW, Topical Alignment, Custom, LLM PII) plus — if any
LLM-based child is present — a single shared `MODEL` child. The resolved `ChatClient` is injected
into every child via `GuardrailContext.chatClient()` (returns `Optional<ChatClient>`), so
individual LLM guardrails no longer declare their own model.

## Preflight stage

Masking rule-based guardrails (PII, Secret Keys, URLs, Custom Regex) run in a preflight stage.
When they detect a match, two things happen:

1. The violation is recorded.
2. The matched substring is masked with its placeholder (e.g. `<EMAIL>`, `<AWS_ACCESS_KEY>`, `<URL>`).

LLM-based guardrails (Jailbreak, NSFW, Topical Alignment, Custom, LLM PII) then run against the
*masked* text. This prevents PII from being sent to the classifier model while still giving it
enough context to judge prompt-injection, safety, and on-topic-ness signals.

**Keyword checks run in the LLM stage, not preflight.** They see PII/URL/secret-masked text so
keyword lists don't need to anticipate every variant of an e-mail local-part or a URL fragment.
If you want keywords to match against raw text (pre-masking), move the `Keywords` child above the
other rule-based children and wrap it in `SanitizeText` instead of `CheckForViolations`.

If you want raw text to reach the LLM overall, remove the rule-based checks from
`CheckForViolations` and leave them only in `SanitizeText`.

## Aggregation semantics

Every child runs, even after a sibling flags a violation — the advisor does not short-circuit on the
first hit. The returned `List<Violation>` captures matches, classifier scores, and any execution
failures side by side so the caller can report all findings at once. When a check cannot run (LLM
outage, missing `MODEL`, bad configuration), a `Violation.ofExecutionFailure(...)` is emitted
alongside the rest and the advisor fails closed.
