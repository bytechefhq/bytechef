# @bytechef/app

A server-only TypeScript SDK for apps that run on ByteChef. An app calls ByteChef actions, tools, workflows and
connections with its app key; the key never reaches the browser.

## Install

```bash
npm install @bytechef/app
```

## Entry points

| Import                                    | Purpose                                                                                      |
| ----------------------------------------- | -------------------------------------------------------------------------------------------- |
| `@bytechef/app`                           | The framework-neutral client over the app API (`/api/app/v1`).                               |
| `@bytechef/app/tanstack-start`            | Middleware, handlers and validators for TanStack Start apps (needs `@tanstack/react-start`). |
| `@bytechef/app/tanstack-start/server-fns` | The nine ready-made server functions; importing one registers all nine.                      |
| `@bytechef/app/ai-sdk`                    | Adapters that expose an app's tools to the Vercel AI SDK (needs `ai`).                       |

`@tanstack/react-start` and `ai` are optional peer dependencies: install only the ones you use.

## Try it in a TanStack Start app

`test-app/` is a blank TanStack Start app that calls the SDK. It resolves `@bytechef/app` through the workspace link, so
build the library first:

```bash
npm install
npm run build:all
cd test-app
cp .env.example .env   # fill in BYTECHEF_URL and BYTECHEF_APP_KEY
npm run dev
```

Create the app key on the app in ByteChef with **App → Rotate key**. See [test-app/README.md](./test-app/README.md) for
what the home route exercises.

## Start a new app

`create/` is `create-bytechef-app`, a non-interactive scaffolder for a TanStack Start app that is safe by default and
built to be extended by an AI coding agent. It copies a vendored template that pins every dependency, writes
`.env.local` (mode `0600`) and installs. The generated app's development build renders what the app may call, from
ByteChef; its production build serves a placeholder page and registers no ByteChef-calling server function until you
write purpose-built ones.

```bash
npm create bytechef-app@latest my-app -- --url https://bytechef.example.com
```

npm takes every flag that is not preceded by `--` as its own option, so the scaffolder's flags must follow `--`; with
`npx create-bytechef-app my-app --url …` or `pnpm create bytechef-app my-app --url …` no separator is needed. There is
no flag for the app key (it would land in shell history and agent transcripts): set `BYTECHEF_APP_KEY` in
`.env.local`, or export it before scaffolding. See [create/README.md](./create/README.md) for the flags and
`create/template/AGENTS.md` for what the generated app tells a coding agent.

`npm run check:template` proves the published shape end to end: it packs both packages, scaffolds an app from the
tarballs, runs the app's own `npm run check`, and drives `bytechef:status` and the development and production builds
against a mock ByteChef. It needs registry access and runs on Linux or macOS; CI runs it in the `sdk-app` job.

## Documentation

The library README (`library/README.md`) documents the API in full. For working on the SDK itself, see
[DEVELOPMENT.md](./DEVELOPMENT.md).
