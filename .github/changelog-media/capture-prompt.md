You are capturing product screenshots and short videos for the ByteChef changelog entry of release `{{TAG}}`.
Work fully autonomously: nobody will answer questions. When something cannot be captured, skip it and say why.

## What is already set up

- The ByteChef release `{{TAG}}` runs at http://127.0.0.1:5173 (client) with the API behind it. Sign in with
  `admin@localhost.com` / `admin`. The database is empty apart from that user.
- `app/` is the ByteChef source at `{{TAG}}`. Read it to find out how the new features look and where they live in
  the UI. Reuse the Playwright helpers, fixtures and page objects in `app/client/test/playwright/` (`utils/login.ts`,
  `utils/projectUtils.ts`, `utils/workflowUtils.ts`, `utils/apiUtils.ts`, `fixtures/`, `pages/`, and the sample workflow
  JSON files next to them).
- `website/app/(marketing)/changelog/data/{{SLUG}}.ts` is the changelog entry. Its headline categories are the
  categories that have `paragraphs`; ignore "More improvements" and any connector list.
- `{{CAPTURE_DIR}}/shot-list.md` is the pull request body. Its "Suggested screenshots" section says what each headline
  category should show.
- Playwright and Chromium are installed in `app/client`. Run your specs from `app/client` with:
  `CAPTURE_DIR={{CAPTURE_DIR}} npx playwright test --config playwright.capture.config.ts`
  The config uses a 1440x900 viewport at device scale factor 2 and runs specs from
  `app/client/test/playwright/changelog-capture/`.

## What to produce

For each headline category, at most one capture, and at most 4 in total:

- Prefer a PNG screenshot. Use a video (5 to 15 seconds) only when the feature is an interaction that a still image
  cannot show. For a video, open a new browser context with
  `recordVideo: {dir: '<a temporary directory>', size: {height: 900, width: 1440}}` and a 1440x900 viewport, perform the
  interaction at a human pace (short waits between steps), close the context, then move the `.webm` file into
  `{{CAPTURE_DIR}}`.
- Save every capture directly in `{{CAPTURE_DIR}}` with a short kebab-case name, e.g. `disable-nodes.png`. Only `.png`
  and `.webm` files whose names use letters, digits and dashes are published; anything else is rejected.
- Create the demo data the shot needs through the UI or the API helpers: realistic project and workflow names such as
  "Lead enrichment" or "Support ticket triage", never "test", "foo" or lorem ipsum. Use only built-in components that
  need no credentials (for example Manual trigger, Script, Data Mapper, Condition, Loop, HTTP Client against
  `https://httpbin.org`). Never type real credentials and never call third-party services that need an account.
- Skip a category when it needs a third-party account, an external system (for example an embedding host app), paid
  features that are not enabled, or anything you cannot make look right.

## Quality bar

After each capture, open the PNG (or a frame of the video, extracted with `ffmpeg -ss 2 -i <file> -frames:v 1 <png>`)
with the Read tool and look at it. Retake it if it shows a loading spinner, a toast, an error, an empty state, a
cut-off dialog, a cookie or onboarding banner, or if the feature is not clearly visible. Frame the relevant part of the
UI: crop with `page.screenshot({clip: ...})` or screenshot a locator when the full page would make the feature tiny.
Aim for a clean, light-themed, realistic-looking product shot.

## Finish

Write `{{CAPTURE_DIR}}/manifest.json`:

```json
{
    "shots": [
        {"category": "<exact category heading from the entry>", "type": "image", "file": "disable-nodes.png",
         "alt": "<one sentence describing what the image shows>"},
        {"category": "<exact category heading>", "type": "video", "file": "agent-tools.webm",
         "label": "<one sentence describing what the video shows>"}
    ],
    "skipped": [{"shot": "<checklist line>", "reason": "<short reason>"}]
}
```

Rules: do not modify files outside `app/client/test/playwright/changelog-capture/` and `{{CAPTURE_DIR}}`, do not edit
the changelog entry, do not commit or push anything. Always write `manifest.json`, even if every shot was skipped.
