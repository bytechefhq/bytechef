# Gap #17 — Human-in-the-Loop Annotation Queues

**Date:** 2026-04-22
**Status:** Stub — follow-up
**Tier:** 3 (P2)
**Edition:** ee
**Parent spec:** [2026-04-21-ai-gateway-gaps-spec.md](2026-04-21-ai-gateway-gaps-spec.md) §5 Tier 3 row 17

## Goal

Queue traces for human review, with a UI for reviewers to apply labels/scores, and automatic dataset promotion on approval. Closes the loop between production traffic and regression datasets.

## Comparators

- **Langfuse** — annotation queues with reviewer assignment.
- **Latitude** — labeling UI on top of datasets.

## Key Constraints

- Queue filtering: by score threshold, tag, or manual sample.
- Per-reviewer task lists (round-robin or explicit assignment).
- Conflict resolution when multiple reviewers disagree — default to majority, escalate to admin on tie.
- Approved annotations land in a Spec C dataset automatically.
- Reviewer actions are audited.

## Dependencies

- **Blocked by Spec C (Datasets + Experiments)** — annotations produce dataset items.
- Gap #2 (Scores API) overlaps — annotations are just human-sourced scores.
- Large frontend work — reviewer UI, assignment dashboard.

## Open Questions

- Integration with external tools (Prolific, Label Studio) — plug-in or copy-paste workflow?
- Payment rails (reviewer pay per label) — out of scope for v1?
- Adjudication: reviewers disagree → third reviewer breaks tie, or admin manually?

## Spec-writing next step

Deferred. Requires Spec C to ship.
