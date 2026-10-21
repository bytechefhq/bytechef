# Development Guide for @bytechef/app

This guide explains how to develop and test the `@bytechef/app` library.

## Table of Contents

1. [Setup Options](#setup-options)
2. [Option 1: npm link](#option-1-npm-link)
3. [Option 2: Local Registry with Verdaccio](#option-2-local-registry-with-verdaccio)
4. [Project Structure](#project-structure)
5. [Available Scripts](#available-scripts)
6. [Generated API types](#generated-api-types)
7. [Test app](#test-app)
8. [The app template and scaffolder](#the-app-template-and-scaffolder)
9. [Verdaccio flow and publishing](#verdaccio-flow-and-publishing)
10. [Server functions from a package: spike result (2026-10-07)](#server-functions-from-a-package-spike-result-2026-10-07)
11. [Troubleshooting](#troubleshooting)

## Setup Options

1. **npm link** - direct symlink, best for hot reload during active development
2. **Verdaccio** - local npm registry, best for testing the full publishing workflow

## Option 1: npm link

```bash
# From the automation/app directory
npm install

# Build the library, then link it globally
cd library
npm run build
npm run link:local
```

In the consuming app, run `npm link @bytechef/app`. Keep the library rebuilding while you edit:

```bash
cd library
npm run watch
```

To unlink:

```bash
cd library
npm run unlink:local
```

## Option 2: Local Registry with Verdaccio

```bash
# Terminal 1 - start the registry on http://localhost:4873
npm run registry:start

# Terminal 2 - build and publish the library to it
npm run publish:library
```

In the consuming app, install from the local registry:

```bash
npm install @bytechef/app@latest --registry http://localhost:4873
```

To use the local registry for every `@bytechef` package, uncomment this line in `.npmrc`:

```
@bytechef:registry=http://localhost:4873
```

After each change, rebuild and republish (`npm run publish:library`; bump the version if the registry rejects the
republish), then reinstall in the consuming app. Stop the registry with `npm run registry:stop`.

## Project Structure

```
automation/app/
├── library/                    # The SDK
│   ├── src/                   # Source code
│   │   └── generated/         # Types generated from the OpenAPI contract (do not edit)
│   ├── dist/                  # Built output (generated)
│   └── package.json
├── create/                     # create-bytechef-app, the scaffolder
│   ├── src/ bin/ test/        # Scaffolder code and its tests
│   └── template/              # The vendored app template (copied verbatim)
├── test-app/                   # Blank TanStack Start app that exercises the SDK
├── scripts/                    # check-template.mjs, the mock ByteChef and its fixture
├── verdaccio.config.yaml      # Local registry config
├── .npmrc                     # NPM configuration
├── package.json               # Workspace root
└── DEVELOPMENT.md             # This file
```

## Available Scripts

### Workspace Root (`automation/app/`)

| Script                    | Description                                                |
| ------------------------- | ---------------------------------------------------------- |
| `npm run registry:start`  | Start Verdaccio local registry                             |
| `npm run registry:stop`   | Stop Verdaccio                                             |
| `npm run publish:library` | Build and publish library to local registry                |
| `npm run build:all`       | Build the library                                          |
| `npm run check:all`       | Run all checks of the library                              |
| `npm run check:test-app`  | Build and type-check the test app and check its bundle     |
| `npm run test:create`     | Run the scaffolder's unit tests (`create/`)                |
| `npm run check:template`  | Scaffold from packed tarballs and prove the app end to end |
| `npm run template:lock`   | Regenerate `create/template/package-lock.json`             |
| `npm run format`          | Check formatting                                           |
| `npm run format:fix`      | Fix formatting                                             |
| `npm run clean`           | Remove build artifacts and node_modules                    |

### Library (`library/`)

| Script                  | Description                                                               |
| ----------------------- | ------------------------------------------------------------------------- |
| `npm run build`         | Type-check, then build the four entry points with their declaration files |
| `npm run watch`         | Rebuild on change                                                         |
| `npm run typecheck`     | Type-check the sources and vite.config.ts without emitting                |
| `npm run lint`          | Lint the sources and vite.config.ts                                       |
| `npm run test`          | Run the unit tests                                                        |
| `npm run generate:api`  | Regenerate `src/generated/openapi.ts` from the server's OpenAPI contract  |
| `npm run check:api`     | Fail when `src/generated/openapi.ts` has drifted from the contract        |
| `npm run check`         | `check:api`, lint, format, typecheck and test                             |
| `npm run publish:local` | Publish to the local registry                                             |

## Generated API types

`library/src/generated/openapi.ts` is generated from
`server/ee/libs/automation/automation-app/automation-app-public-rest/openapi.yaml` and committed. After the contract
changes, regenerate with `npm run generate:api` from `library/` and commit the result. CI runs `check:api` (it is part of
`npm run check`), which fails when the committed file is out of date.

## Test app

`test-app/` is a blank TanStack Start app that uses the library's `tanstack-start`, `tanstack-start/server-fns` and
`ai-sdk` entry points. It resolves `@bytechef/app` through the workspace link to `library/dist`, so run
`npm run build:all` first, then `npm run dev` in `test-app/` with a `.env` copied from `.env.example`.

`npm run check:test-app` (workspace root) builds and type-checks the app and runs `test-app/scripts/check-bundle.mjs`,
which fails when the client bundle contains the app key variable name, the key value or the `/api/app/v1` request layer,
or when no RPC stubs were compiled. It also fails when anything reachable from the built `@bytechef/app/tanstack-start`
entry calls `createServerFn(` - Start registers every server function in an imported module, so that entry must stay
free of them - or when `@bytechef/app/tanstack-start/server-fns` lacks one of the nine module-level declarations. Run
`npm run build:all` before it, and again after every change to the library.

## The app template and scaffolder

`create/` holds the `create-bytechef-app` package (`bin/`, `src/`, `test/`) and, in `create/template/`, the complete
app it copies. The template is a real project: `package.json`, `AGENTS.md`, the scripts and the Dockerfile are exactly
what users get.

**Lockstep versions.** `create-bytechef-app` and `@bytechef/app` carry the same version, and the template's
`@bytechef/app` dependency must equal it (the scaffolder rewrites that one entry and nothing else). Bump both together
and release both together. Every other template dependency is an exact version; change one only after
`npm run check:template` passes.

**Template lockfile.** `create/template/package-lock.json` locks the template's transitive dependencies. It is
generated without `@bytechef/app` (that version is published together with the scaffolder, so it cannot be locked in
advance; the app's first `npm install` adds it). After changing any template pin, run `npm run template:lock`
(`scripts/template-lock.mjs`: it copies the template's `package.json` to a temporary directory, drops `@bytechef/app`,
runs a real `npm install --ignore-scripts` and then `npm prune` there and copies the lockfile back) and commit the
result. It always resolves from the public registry (`--registry=https://registry.npmjs.org/`, with any
`npm_config_registry` removed from its environment), so a Verdaccio or mirror setting cannot leak into the lockfile;
`check:template` fails on any `resolved` URL outside `registry.npmjs.org`. Regenerate with Node 24 (npm 11): npm 11
drops an optional nested peer (`nitro/node_modules/lru-cache`) that npm 10 adds back, and the template's Docker image
is Node 24. `npm run template:lock` again without a pin change also refreshes the transitive versions.

**Try the scaffolder locally.** The template depends on `@bytechef/app` from a registry, so scaffold with packed
tarballs rather than the workspace link. From the workspace root:

```bash
npm run build:all
SCRATCH="$(mktemp -d)"
(cd library && npm pack --pack-destination "$SCRATCH")
(cd create && npm pack --pack-destination "$SCRATCH")
npm exec --yes --package="$SCRATCH/create-bytechef-app-0.1.0.tgz" -- create-bytechef-app "$SCRATCH/my-app" \
  --url http://localhost:9555 --no-install
cd "$SCRATCH/my-app"
npm pkg set "dependencies.@bytechef/app=file:$SCRATCH/bytechef-app-0.1.0.tgz"
npm install
BYTECHEF_APP_KEY=sk-local-canary npm run check
```

`npm run check` ends with the bundle leak check. Prove that it fails when it should: a build made with
`NODE_ENV=development` compiles the development-only server functions into the server output and the check must exit 1.

**Checks.**

- `npm run test:create` (also part of `check:all`) runs the scaffolder's vitest suite and the overview mapping test.
- `npm run check:template` (`scripts/check-template.mjs`) first checks the formatting of the template's own files (generated ones are listed in the workspace .prettierignore), then builds the library, packs both packages, scaffolds an app with
  `npm exec --package=<tarball>`, checks that the scaffolded `package-lock.json` matches the template's pins (it
  fails when a pin was bumped without `npm run template:lock`), installs it from the tarball and checks that the
  install changed no locked version and added nothing but `@bytechef/app` (and, under npm 10, optional peer entries),
  checks that `npm ci --dry-run` accepts the result, runs its `npm run check`, proves a
  `NODE_ENV=development` build fails the check, then drives `bytechef:status`, the production server and the
  development server against `scripts/mock-bytechef.mjs` (fixture `scripts/fixtures/app.json`). It spawns every child
  with an explicit environment, so a `BYTECHEF_URL` or `BYTECHEF_APP_KEY` of your own cannot leak in. It needs registry
  access, runs on Linux and macOS only, and takes a few minutes.

**Verdaccio for both packages.** To try the published shape, start the registry and publish both packages to it
(`npm run publish:library` covers the library; run `npm publish --registry http://localhost:4873` in `create/` for the
scaffolder, after the library, since the template pins it). Then `npm create bytechef-app@latest my-app --registry http://localhost:4873 -- --url …` scaffolds from
the registry. Publishing to npmjs stays manual for both.

## Verdaccio flow and publishing

The local registry is for trying the published package shape end to end:

1. `npm run registry:start` (Terminal 1) serves Verdaccio on `http://localhost:4873`.
2. `npm run publish:library` (Terminal 2) builds the library and publishes it to that registry.
3. In a consuming app, `npm install @bytechef/app@latest --registry http://localhost:4873`.

**Publishing to npmjs is a manual step that you trigger yourself.** No script in this workspace publishes to npmjs;
`publish:local` and `publish:library` only ever target the local registry.

## Server functions from a package: spike result (2026-10-07)

Quoted verbatim from the spike's `VERDICT.md`.

**Spike verdict — server functions shipped in a package (2026-10-07)**

Setup: blank TanStack Start app from `@tanstack/cli@0.71.1` (`@tanstack/react-start` 1.168.60, `@tanstack/react-router`
1.170.41, Vite 8), production build (`vite build`) served by `vite preview`. The fake package `spike-lib` declares
`peerDependencies: {"@tanstack/react-start": "*"}` and was installed as a packed tarball (a real directory, not a symlink).
Variants: A = server function at the package's top level; B = server function built inside a package factory;
C = server function in app source using the package's middleware, validator and handler (control). Handlers are passed by
reference, as `@bytechef/app` will do.

First attempt: B written as `return {ping: createServerFn(...)...}` failed the build with
"createServerFn must be assigned to a variable!" (thrown while compiling `node_modules/spike-lib/index.js`). The Start
compiler (start-plugin-core `compiler.js`) states "Only ServerFn is always declared at module level". B was retried as
`const ping = createServerFn(...)` inside the factory; the build then passed.

Build: exit 0; outputs dist/client and dist/server.

Bundle canaries (files containing each string):
CANARY_HANDLER_A client=0 server=1
CANARY_HANDLER_B client=0 server=1
CANARY_HANDLER_C client=0 server=1
CANARY_MIDDLEWARE_SERVER client=0 server=2
\_serverFn client=1 server=3
spike-lib imported at runtime by server: 0

SSR (loader during server render):
A=CANARY_HANDLER_A:CANARY_MIDDLEWARE_SERVER
B=ERROR Cannot read properties of undefined (reading 'answer')
C=CANARY_HANDLER_C:CANARY_MIDDLEWARE_SERVER

Browser (button, RPC to /\_serverFn):
A=CANARY_HANDLER_A:CANARY_MIDDLEWARE_SERVER
B=ERROR Cannot read properties of undefined (reading 'answer')
C=CANARY_HANDLER_C:CANARY_MIDDLEWARE_SERVER

Reading: the package is inlined into the server build and compiled for both environments; a top-level server function
in the package works over SSR and RPC with its server middleware. A server function created inside a function is not
compiled (the compiler only scans module-level declarations), so a factory cannot produce server functions. B: FAILED.

PACKAGE_SERVER_FN

## Troubleshooting

### Changes not appearing in the consuming app

1. Rebuild the library (`cd library && npm run build`), or keep `npm run watch` running.
2. With the local registry, republish and reinstall.
3. Restart the consuming app's dev server.

### "Cannot find module @bytechef/app"

Check the link or install: `npm ls @bytechef/app` in the consuming app. For a link, rerun `npm run link:local` in
`library/` and `npm link @bytechef/app` in the app.

### Port 4873 already in use

```bash
lsof -i :4873
npm run registry:stop
```

### `check:api` fails

The committed `src/generated/openapi.ts` no longer matches the OpenAPI contract. Run `npm run generate:api` in
`library/` and commit the regenerated file.
