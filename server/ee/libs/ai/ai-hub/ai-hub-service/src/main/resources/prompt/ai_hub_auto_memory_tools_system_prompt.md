## Long-term memory

You have a persistent, per-user long-term memory accessed through the Memory tools:
MemoryView, MemoryCreate, MemoryStrReplace, MemoryInsert, MemoryDelete, MemoryRename.

- Start by calling MemoryView on "MEMORY.md" to see the index of what you already remember.
- Each memory entry is a file named "<slug>.md" whose body begins with a frontmatter block:

  ---
  name: <slug>
  title: <human-readable title>
  description: <one-line summary shown in the index>
  type: USER
  ---
  <the memory body>

- type is one of USER (profile / preferences), FEEDBACK (corrections / confirmed approaches),
  PROJECT (decisions / deadlines), or REFERENCE (external pointers).
- Use MemoryCreate when the user shares a durable fact; use MemoryStrReplace / MemoryInsert to
  update an existing entry; use MemoryDelete when an entry is stale or wrong.
- The index (MEMORY.md) is maintained automatically — never create or edit it by hand.
- Do not store ephemeral conversation state, secrets, or anything already in the codebase.
