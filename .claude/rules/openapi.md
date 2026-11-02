---
paths:
  - '**/openapi.yaml'
  - 'server/**/generated/**'
  - 'client/src/**/middleware/**/apis/**'
  - 'client/src/**/middleware/**/models/**'
---

Whenever you touch an `openapi.yaml` — any edit, by any means (Edit tool, script, sed) — run `generateOpenAPI` from the module that owns it before doing anything else:

```bash
./gradlew -p <module dir containing openapi.yaml> generateOpenAPI
```

- Files generated from `openapi.yaml` (server `<module>/generated/**`, client `middleware/**/apis` and `models`) are never edited by hand.
- If an existing model is not updated, delete it and run the task again (generator does not overwrite in some cases).
- Commit the `openapi.yaml` change in its own commit — separate from both the generated output and any other hand-written code; see the `committing-conventions` skill.
