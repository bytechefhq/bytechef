# Triage Labels

The skills speak in terms of five canonical triage roles. This file maps those roles to the actual
label strings used in this repo's issue tracker (`bytechefhq/bytechef`).

| Label in mattpocock/skills | Label in our tracker | Meaning                                  |
| -------------------------- | -------------------- | ---------------------------------------- |
| `needs-triage`             | `needs triage`       | Maintainer needs to evaluate this issue  |
| `needs-info`               | `needs-info`         | Waiting on reporter for more information |
| `ready-for-agent`          | `ready-for-agent`    | Fully specified, ready for an AFK agent  |
| `ready-for-human`          | `ready-for-human`    | Requires human implementation            |
| `wontfix`                  | `wontfix`            | Will not be actioned                     |

Notes specific to `bytechefhq/bytechef`:

- **`needs triage`** and **`wontfix`** already exist on the repo — use those exact strings
  (note `needs triage` has a space, not a hyphen).
- **`needs-info`**, **`ready-for-agent`**, and **`ready-for-human`** don't exist yet; create them
  with `gh label create --repo bytechefhq/bytechef "<name>"` the first time `triage` applies them.

When a skill mentions a role (e.g. "apply the AFK-ready triage label"), use the corresponding label
string from this table.

Edit the right-hand column to match whatever vocabulary you actually use.
