---
title: Sample App
description: A reference Next.js + Fastify implementation that demonstrates the embedded SDK and APIs end-to-end.
---

![Sample App overview](sample-app-overview.png)

---

[**bytechef-embedded-sample-app**](https://github.com/bytechefhq/bytechef-samples/tree/main/bytechef-embedded-sample-app) is a runnable reference implementation of an embedded integration. Clone it to see how every piece described in the rest of this section fits together in a real app.

It's split into two pieces:

- **`back-end/`** — a small Fastify + TypeScript server that signs JWTs (`RS256`) for end-user sessions using a Signing Key.
- **`front-end/`** — a Next.js app that consumes those JWTs to drive the embedded SDK and several embedded APIs.

---

## What the sample demonstrates

| Page in the sample | Demonstrates | Underlying API or SDK |
|---|---|---|
| **Integrations** (`/integrations`) | Letting an end user connect a third-party service via the ByteChef connect dialog. | `useConnectDialog` from [`@bytechef/embedded`](/embedded/quickstart#6-install-the-react-sdk) |
| **Automations** (`/automations`) | Customer-facing workflow management — list / create / enable / disable / delete the user's own workflows. | `GET/POST/DELETE /api/embedded/v1/automation/workflows` |
| **ComponentKit Playground** (`/component-kit`) | Invoking a single component action directly, without authoring a workflow first. | `POST /api/embedded/v1/{externalUserId}/components/{componentName}/versions/{componentVersion}/actions/{actionName}` |
| **Chat MCP** (`/chat-mcp`) | Using a ByteChef **MCP Server** as the tool source for an AI assistant chat. | MCP transport against your MCP Server URL, authenticated with the end-user JWT |
| **Chat Component Kit** (`/chat-component-kit`) | Exposing every component action available to a user as tools for an AI assistant. | `GET /api/embedded/v1/{externalUserId}/tools` |
| **Create From Chat** (`/create-from-chat`) | An agent that creates and refines the user's workflows from chat, then returns to the list when done. | [Workflow Builder Tools](/embedded/workflow-builder-tools) via `POST /api/embedded/v1/{externalUserId}/tools` |
| **App Event** (`/app-event`) | Firing an **App Event** from your application to trigger every workflow whose connected-user trigger subscribes to it. | [`POST /api/embedded/v1/app-events`](/openapi/embedded-webhook) |
| **Request** (`/request`) | Triggering a single workflow synchronously via its **Request trigger** and reading the workflow's response back. | [`POST /api/embedded/v1/workflows/{workflowUuid}`](/openapi/embedded-webhook) |

Together these cover the most common embedded integration patterns: the connect flow, programmatic workflow CRUD, ad-hoc action execution, MCP-based tooling, tool-by-tool LLM integration, agent-driven workflow authoring, App Event firing, and synchronous request-triggered workflows.

---

## How the pieces wire up

```
┌──────────────────────────────────────────┐
│  Browser (Next.js front-end :3000)       │
│  ──────────────────────────────────────  │
│  - useConnectDialog (Integrations)       │
│  - fetch() to its own /api/* routes      │
└──────────────┬───────────────────────────┘
               │  (1) request JWT
               ▼
┌──────────────────────────────────────────┐
│  Fastify back-end (:3001)                │
│  ──────────────────────────────────────  │
│  POST /api/token                         │
│    body: { externalUserId, name, ... }   │
│  → signs RS256 JWT with private key      │
│    and kid from your Signing Key         │
└──────────────┬───────────────────────────┘
               │  (2) JWT returned to browser
               ▼
┌──────────────────────────────────────────┐
│  Browser uses JWT to call ByteChef       │
│  Authorization: Bearer <jwt>             │
│  X-Environment: DEVELOPMENT              │
└──────────────┬───────────────────────────┘
               ▼
        ByteChef Embedded API (:5173/9555)
```

The back-end never proxies ByteChef traffic — it only mints JWTs. The browser (or the Next.js API routes acting on its behalf) calls ByteChef directly with that JWT.

---

## Running the sample locally

### 1. Prerequisites

- A ByteChef EE instance running locally (default: `http://localhost:5173`).
- A **Signing Key** created in **Embedded → Settings → Signing Keys**. Copy the **private key** (shown once) and the **Key Id** (`kid`).
- Node.js 18+ and npm.

### 2. Configure the back-end

```bash
cd bytechef-embedded-sample-app/back-end
npm install
```

Create `.env` in `back-end/`:

```bash
PORT=3001
TOKEN_EXPIRY=1h
BYTECHEF_PRIVATE_KEY="-----BEGIN PRIVATE KEY-----
...your private key from Signing Keys...
-----END PRIVATE KEY-----"
BYTECHEF_KID=<your-key-id>
```

Run it:

```bash
npm run dev
# Backend listening on http://localhost:3001
```

### 3. Configure the front-end

```bash
cd ../front-end
npm install
```

Create `.env.local` if you need non-default URLs:

```bash
NEXT_PUBLIC_BACKEND_APP_BASE_URL=http://localhost:3001
NEXT_PUBLIC_BYTECHEF_APP_BASE_URL=http://localhost:5173
NEXT_PUBLIC_BYTECHEF_ENVIRONMENT=DEVELOPMENT
NEXT_PUBLIC_BYTECHEF_EXTERNAL_USER_ID=1234567890
# Only needed for the Chat MCP page:
NEXT_PUBLIC_BYTECHEF_MCP_SERVER_URL=http://localhost:5173/<your-mcp-server-path>
```

Run it:

```bash
npm run dev
# Frontend on http://localhost:3000
```

Open `http://localhost:3000`, visit the **Integrations** page, click an integration card, complete the connect dialog, and you'll see your new connection appear in **Embedded → Connections** in the ByteChef UI.

---

## Developing against a local SDK build

If you're modifying the embedded React SDK and want the sample app to pick up your changes:

```bash
# Terminal 1 — rebuild the SDK on save
cd <repo-root>/sdks/frontend/embedded/library/react
npm run watch

# Terminal 2 — after each rebuild, re-link into the sample app
cd <repo-root>/../bytechef-samples/bytechef-embedded-sample-app/front-end
npm install --install-links
rm -rf .next
npm run dev
```

`--install-links` is required because Next.js Turbopack can't resolve symlinked packages — the flag copies the SDK build into the sample's `node_modules` instead of symlinking.

---

## Where to look in the source

| You want to see... | Open... |
|---|---|
| How to wire `useConnectDialog` | `front-end/src/app/integrations/page.tsx` |
| Listing / creating / enabling user workflows | `front-end/src/lib/api.ts` + `front-end/src/app/automations/page.tsx` |
| Running a single component action | `front-end/src/app/api/component-kit/route.ts` |
| Using ByteChef as an MCP tool source | `front-end/src/app/api/chat-mcp/route.ts` |
| Loading per-user tools into an LLM | `front-end/src/app/api/chat-component-kit/route.ts` |
| Firing an App Event for the connected user | `front-end/src/app/app-event/page.tsx` + `front-end/src/app/api/app-event/route.ts` |
| Triggering a workflow via its Request trigger | `front-end/src/app/request/page.tsx` + `front-end/src/app/api/request/route.ts` |
| The JWT signing reference | `back-end/src/index.ts` |

Once you have the sample running, the rest of the docs in this section describe the corresponding admin UI surface that backs each demo.
