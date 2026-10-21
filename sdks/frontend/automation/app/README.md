# @bytechef/app

A server-only TypeScript SDK for apps that run on ByteChef. An app calls ByteChef actions, tools, workflows and
connections with the secret key of its app instance, and can sign its users in through ByteChef; neither the key nor a
user's token reaches the browser.

## Install

```bash
npm install @bytechef/app
```

## Entry points

| Import                                    | Purpose                                                                                              |
| ----------------------------------------- | ---------------------------------------------------------------------------------------------------- |
| `@bytechef/app`                           | The framework-neutral client over the app API (`/api/app/v1`), with `auth` and `context`.            |
| `@bytechef/app/tanstack-start`            | Middleware, sign-in handlers and validators for TanStack Start apps (needs `@tanstack/react-start`). |
| `@bytechef/app/tanstack-start/server-fns` | The eleven ready-made server functions; importing one registers all eleven.                          |
| `@bytechef/app/ai-sdk`                    | Adapters that expose an app's tools to the Vercel AI SDK (needs `ai`).                               |

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

Create the key on an app instance in ByteChef with **Rotate key** (or `bytechef automation app key rotate`). See [test-app/README.md](./test-app/README.md) for
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

## Deploy and reach members

An app in ByteChef has one or more instances, each with its own key, bindings and sign-in mode. Create the app and its
first instance with `bytechef automation app create`, add another with `bytechef automation app instance create`, and
record or run a deployment with `bytechef automation app deployment record` or
`bytechef automation app deployment deploy --wait`; the template's `AGENTS.md` and `create/template/README.md` give the
sequence. Members of an instance's workspace open published apps from the App Portal in ByteChef (an administrator turns
on "Show in App Portal" on the app).

## Documentation

The library README (`library/README.md`) documents the API in full. For working on the SDK itself, see
[DEVELOPMENT.md](./DEVELOPMENT.md).
