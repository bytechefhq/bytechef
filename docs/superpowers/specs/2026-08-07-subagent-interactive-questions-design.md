# Subagent Interactive Questions

Let a specialist subagent ask the user a real multiple-choice question — rendered as option buttons,
not paraphrased into prose by the parent agent.

**Depends on:** `2026-08-07-subagent-conversation-memory-design.md`. That dependency is hard, not
sequencing preference: see "Why memory must land first".

## Problem

When a specialist needs a decision it cannot make — which agent did you mean, what should the
instructions say — it has no way to ask. `prompt_personal_agent_manager.txt` instructs it to give up
and hand the question back as text:

> "If the request is ambiguous (which agent, what the instructions should say), return the concrete
> question instead of guessing."

The parent then paraphrases that into its own prose. The user reads a sentence and types a reply,
where the main agent in the same situation would have rendered buttons via `askUserQuestion`.

The gap is narrower than it first appears, and worth stating precisely because an earlier version of
this analysis got it wrong. The specialist is **not** missing a suspend/resume mechanism — the main
agent does not have one either. `askUserQuestion` on the chat surface is a *render* tool: it returns
a payload, the client draws buttons from it, the run finishes, and the user's click arrives as a new
turn. (The `ToolSuspendConstants` sentinel protocol — `SUSPENDED_SENTINEL`, `CONVERSATION_STATE` — is
the *workflow* AI Agent component suspending an Atlas job. Different surface.)

So the specialist is missing exactly one thing: a way to get an `ask-user-question` payload onto the
parent's stream. Everything else — the client renderer, the answer path, the re-delegation — already
exists or is delivered by the memory spec.

## Why memory must land first

Without memory this feature is worse than nothing. A specialist that asks a good question, gets an
answer, and is then re-delegated with total amnesia has to re-derive everything it knew when it asked
— and the user, having answered a specific question, now watches it start over. Buttons make that
failure faster to reach, not less painful.

With memory, the round trip *is* a resume: the specialist picks up holding its own draft, its own
tool results, and its own reasoning. This spec is the interaction layer on top of that.

## Decisions

- **No blocked threads.** The delegation returns; it does not wait on a human. Blocking a
  tool-execution thread would pin it per pending question, need a timeout, die on restart, and fail
  across a distributed hop.
- **Reuse the existing client renderer.** `AskUserQuestionMessage` and its data part already do the
  job; this spec routes a new producer into them rather than adding a parallel UI.
- **The delegate's tool result carries the question.** Not a `CustomEvent` — see "Why not the
  progress channel".
- **One narrow client change**, dispatching on payload kind rather than tool name.

## Component 1 — `SubagentAskChannel` (server)

A thread-bound collector, mirroring the existing `SubagentProgressChannel` exactly — same
`ThreadLocal` + `runWithChannel(Runnable)` LIFO-safe shape, which is already proven for carrying data
out of a delegation.

Where progress events accumulate in a deque and are drained for narration, this holds **at most one**
pending question per delegation. A second `askUserQuestion` call within the same delegation overwrites
nothing and is rejected with a tool error: two simultaneous questions have no sensible rendering, and
the specialist should ask one thing, stop, and continue after the answer.

## Component 2 — The specialist-facing `askUserQuestion` tool

A `ToolCallback` registered on the specialist `ChatClient`s (in the same `*Configuration` classes that
build them), producing the identical payload shape the main agent's `AskUserQuestionToolCallback`
produces — `{kind: "ask-user-question", questions: [{question, header, multiSelect, options}]}`.

It differs from the main agent's version in what it returns to *its own* LLM. The main agent's
version returns the payload, because the payload is the thing the client renders. The specialist's
version writes the payload into `SubagentAskChannel` and returns a short instruction:

> Question posed to the user. Stop now and return a one-line summary; you will be re-invoked with the
> answer.

Without that, the specialist would treat the tool result as an answer and keep going — inventing the
user's decision, which is the exact failure the tool exists to prevent.

## Component 3 — Delegate callbacks relay the question

Every delegate `ToolCallback` (`ManagerSubAgentToolCallback`, `SkillsAgentToolCallback`,
`ResearchToolCallback`, …) wraps its `chatClient.prompt(request).call()` in
`SubagentAskChannel.runWithChannel(...)`. After the call returns, if a question was raised, the
callback returns **the ask payload as its own tool result** instead of the specialist's text summary.

That is what puts the question on the parent's stream: a delegate tool's result *is* a main-agent tool
result, which is precisely the coupling the client requires.

The specialist's text summary is discarded in that branch. It is a one-line "I asked the user
something" by construction, and keeping both would mean rendering a question card next to prose
restating it.

## Component 4 — Client dispatch by payload kind

`toToolResultDataPart.ts` currently dispatches on the tool name:

```ts
if (toolCallName === 'askUserQuestion') { … }
```

A delegate's tool is named `personal_agent_manager`, so no payload it returns will ever render, no
matter how well-formed. This is the single hard blocker on the client side.

Add a fallback after the tool-name branches: if no branch matched, attempt to parse the result and
dispatch on its `kind` field. `ask-user-question` then renders through the existing component
unchanged, and any future kind a specialist relays (`select-property-option`, say) works without
another client change.

Scope the fallback to the known `kind` values already handled in this file. A bare "parse everything
and sniff" would try to JSON-parse every tool result in the system, and would let an unrelated tool
that happens to emit a `kind` field hijack a renderer.

## Component 5 — Prompts

- **Specialist prompts** (`prompt_personal_agent_manager.txt` and its siblings): replace "return the
  concrete question instead of guessing" with instruction to call `askUserQuestion`, then stop.
- **Parent prompts** (`prompt_ai_hub_build.txt`): when a specialist delegate returns an
  ask-user-question payload, do not paraphrase or re-ask — the user is already looking at the
  question. Acknowledge briefly or say nothing and end the turn.

## Why not the progress channel

`SubagentProgressEmitter` already pushes `CustomEvent`s from inside a delegation to the client, and
routing the question through it looks natural. It was rejected because the client builds this UI from
**tool results** (`toToolResultDataPart`), not from custom events. Using the progress channel would
mean writing a second, parallel renderer for a custom event that produces the same card the tool-result
path already produces — two code paths, one of which would drift.

The progress channel stays what it is: narration.

## Known gaps

- **One question per delegation.** A specialist cannot ask two things in one pass; it asks, stops, and
  asks again next turn if needed.
- **No enforcement that the specialist stops.** Component 2 instructs it to; nothing prevents a model
  from calling another tool afterwards. The delegation still terminates normally, so the worst case is
  wasted work before the question surfaces, not a wrong answer reaching the user.
- **The parent may still editorialize.** Prompt guidance, not a hard gate — the same
  advisory-not-enforced posture the personal-agent instructions overlay already has.

## Testing

Unit:

- `SubagentAskChannel` — binding is LIFO-safe under nesting; a second question in one delegation is
  rejected.
- The specialist tool writes the payload to the channel and returns the stop instruction, not the
  payload.
- A delegate callback returns the ask payload when a question was raised, and the specialist's summary
  when none was.

Client (Vitest):

- `toToolResultDataPart` renders `data-ask-user-question` for a delegate tool name when the payload
  kind matches.
- An unknown kind, and a non-JSON result, both fall through without throwing.

Integration:

- A delegation that asks produces a parent tool result carrying the payload; the following delegation
  (with the answer) sees the specialist's prior turn — the memory spec's contribution, pinned here
  because this feature is what makes that continuity visible.

## Verification

- `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test`
- `cd client && npm run check`
