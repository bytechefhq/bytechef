# ByteChef v0.25 Changelog

This release covers milestones v0.2 through v0.25 — the culmination of our initial development phase leading up to public launch.

## New Components

Over 30 new integrations added across this release cycle:

**Google Workspace:** Google Drive, Docs, Sheets, Gmail, Calendar, Contacts, Maps, Meet, Chat, Slides, Forms, Photos, Search Console, BigQuery, Tasks, YouTube

**Microsoft 365:** Outlook 365, Teams, SharePoint, Excel, To Do

**Communication & Messaging:** Telegram, Rocket.Chat, Pushover, Brevo, Resend

**Project Management & Productivity:** Trello, Monday, Todoist, NiftyPM

**Developer Tools & DevOps:** Jenkins, DocuSign, urlscan.io, ScrapeGraphAI

**Utility Components:** Math Helper, Object Helper, Image Helper, PDF Helper, Date Helper, JWT Helper, Wolfram Alpha (Short Answers & Full Results), Dropbox

## Workflow Engine Improvements

- **Map task dispatcher** — UI support for parallel mapping over collections
- **OnError task dispatcher** — error handling at the task dispatcher level
- **Subflow support** — design and execution of subflow task dispatchers
- **Workflow editor context menu** — right-click actions on workflow nodes
- **Drag-and-drop data pills** — drag data pills directly into property inputs
- **Task execution tree** — moved recursion logic to backend for better performance
- **Branch improvements** — proper integer support in branch cases

## AI & MCP

- **MCP Server** — initial automation MCP server enabled
- **OpenAI response object changes** — updated to latest OpenAI API format

## Platform & Infrastructure

- **OAuth2 scope selection** — users can select optional OAuth2 scopes when creating connections
- **Connection version select** — choose component version in the connection dialog
- **Connection grouping** — connections grouped by component for clarity
- **Documentation links** — added to connection dialog and trigger configuration panel
- **OpenTelemetry logging** — observability support added
- **Kubernetes Helm chart** — updated and fixed to reflect recent features
- **Deployment improvements** — hidden disabled workflows in deployments, fixed schedule conflicts
- **Actuator endpoint** — for starting and stopping message broker listeners
- **Generated components** — backend support for tools, fixed array request body execution, alphabetical action ordering
- **Outlook send-on-behalf** — support for sending messages on behalf of another user

## UI/UX Improvements

- **Simplified property state buttons** — cleaner interface for toggling property states
- **Component configuration panel** — improved information display
- **Toast system migration** — migrated from useToast hook to Sonner for better notifications
- **Formula control type** — locked formula state for property inputs
- **Narrow screen support** — runtime environment selection on smaller screens
- **UserGuiding integration** — guided onboarding for new users
- **Node labels** — used instead of workflow node names in deployment dialogs

## Bug Fixes

141 issues resolved across all milestones, including:

- Fixed workflow duplication errors and display issues
- Resolved data pill autocomplete positioning and cleanup of deleted task references
- Fixed display conditions, dynamic properties, and hidden property access
- Corrected workflow execution history in production environments
- Fixed rich text editor HTML tag preservation
- Resolved subflow and loop execution errors
- Fixed connection creation flows (Slack, Jira, and others)
- Corrected deployment enable errors on cloud
- Fixed branch node rendering after setting expression properties
- Resolved mention input issues with special characters and data pill deletion
- Fixed generated component versioning issues
