# Organization settings under `/settings` (#7598)

## Intent

Organization settings are organization-wide, but each page has two URLs today because `platformSettingsRoutes` is
mounted under both `/automation/settings` and `/embedded/settings`. The URL suggests the page belongs to Automation or
Embedded, and in-page links have to work out which mount they are on (`getAiSkillsBasePath`). Give every Organization
page one URL, and move the one page that is not organization-wide (Memories) into the Automation section.

## Goals

- Organization pages live once, under `/settings/...`: Users, Billing, AI Providers (`/settings/ai-providers`), AI
  Skills (`/settings/ai/skills`, `/settings/ai/skills/:skillId`), MCP Server, Custom Components, API Connectors (and its
  `new/manual|import|ai` children), Notifications, Identity Providers, OAuth2 Clients, Admin API Keys, Audit Events.
- Section settings stay where they are: workspace settings under `/automation/settings/...`, Signing Keys and API Keys
  under `/embedded/settings/...`.
- Memories moves from Organization to the Automation section, at the same URL `/automation/settings/ai/memories`, and
  always uses the current workspace.
- Embedded settings show no Memories entry until #7595 adds the Embedded Memories page.
- Old URLs keep working: `/automation/settings/<org page>` and `/embedded/settings/<org page>` redirect to
  `/settings/<org page>` (path suffix and query string kept). `/embedded/settings/ai/memories` and the legacy
  `/embedded/ai/memories` keep redirecting to `/automation/settings/ai/memories`.

## Non-goals

- No server change.
- Account pages (`/automation/account`, `/embedded/account`) are not moved.
- No change to page content, permissions (`PrivateRoute`, `EEVersion`) or feature flags.

## Anti-goals

- `/settings` must not lose the user's place: it must not drop the Automation/Embedded app sidebar or the section items
  from the settings menu.

## Design

- **Mode on `/settings`.** The app already persists the last Automation/Embedded mode (`usePlatformTypeStore`,
  `bytechef.mode-type`). On `/settings/...` the URL has no mode, so the persisted mode is used, defaulting to
  Automation. URL-derived mode still wins on `/automation/...` and `/embedded/...`.
- **App shell (`App.tsx`).** Main navigation, workspace scope loading and the trial banner use that resolved mode
  instead of raw pathname checks, so `/settings` shows the sidebar of the mode the user came from.
- **One settings menu.** A single builder returns the menu for a mode: that mode's section items, then the
  Organization heading and Organization items. All hrefs are absolute. The `/automation/settings`, `/embedded/settings`
  and `/settings` layouts all render it, so the menu is identical wherever the user is.
- **`Settings` layout.** Feature-flag/billing visibility checks match on the page key rather than the relative href, so
  they keep working with absolute hrefs.
- **Links.** Hard-coded `/automation/settings/<org page>` links point to `/settings/<org page>` (billing redirect in
  `useFetchInterceptor`, `TrialBanner`, AI Providers links in Copilot, ModelPicker and Knowledge Bases, Skills links in
  the workflow editor, API Connectors). `getAiSkillsBasePath` is replaced by a fixed `/settings/ai/skills`. The footer
  "Settings" item opens the current mode's section settings as today.
- **Memories.** Drops the `/embedded/` pathname branch and always reads the current workspace.

## Approaches

- **Chosen: one `/settings` mount, mode from the persisted store.** One URL per page, the user keeps their sidebar and
  full settings menu.
- Neutral `/settings` page without a mode sidebar: rejected, it loses the user's place and the section items.
- Keep both mounts and only fix links: rejected, pages would still have two URLs.

## Left to the builder

- Exact module for the menu builder and redirect helper.
- Test layout, following the existing route and layout tests.
