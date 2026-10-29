---
paths:
  - '**/openapi.yaml'
  - 'server/**/generated/**'
  - 'client/src/**/middleware/**/apis/**'
  - 'client/src/**/middleware/**/models/**'
---

Files generated from `openapi.yaml` (server `<module>/generated/**`, client `middleware/**/apis` and `models`) are never edited by hand.

After changing an `openapi.yaml`, regenerate from the module that owns it:

```bash
./gradlew -p <module dir containing openapi.yaml> generateOpenAPI
```

- If an existing model is not updated, delete it and run the task again (generator does not overwrite in some cases).
- Commit the `openapi.yaml` change and the generated output separately; see the `committing-conventions` skill.
