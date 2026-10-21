# ByteChef app

A TanStack Start app that calls ByteChef — actions, tools, workflows and HTTP through bound connections — with one
app key. Third-party systems are reached only through ByteChef; this app holds no vendor credentials.

## Run

Requires Node.js 22.18 or newer (24 recommended): the development server fails on older Node.js 22 releases.

1. Run `bytechef automation app create --name my-app` here (it creates the app in ByteChef's DEVELOPMENT environment
   and writes `BYTECHEF_URL` and `BYTECHEF_APP_KEY` into `.env.local`; see `AGENTS.md`), or copy `.env.example` to
   `.env.local` and set both, taking the key from the app's page in ByteChef. The shell environment wins over
   `.env.local`: an exported `BYTECHEF_APP_KEY` replaces the file's value.
2. `npm run dev` — in development the home page lists what this app may call and lets you try an action.
3. `npm run -s bytechef:status` — the same, as JSON (for scripts and coding agents).

## Dependencies

`package-lock.json` locks every dependency, direct and transitive, so each npm install of this app gets the versions
it was tested with; commit it. It does not list `@bytechef/app` until the first install adds it, so if the app was
scaffolded with `--no-install`, run `npm install` once before deploying (the Docker build's `npm ci` needs the
complete lockfile). An app scaffolded for pnpm, yarn or bun has no `package-lock.json`: those package managers
resolve the transitive dependencies afresh.

## Check

`npm run check` typechecks, builds, and verifies that no secret reached the browser bundle, that no
development-only server function was compiled, and that the public ready-made server functions from
`@bytechef/app/tanstack-start/server-fns` are not in the build. If you do want those, set
`"bytechef": {"allowReadyMadeServerFns": true}` in `package.json`; the check then prints a warning instead of failing.
Run it before every deploy. A Vercel deploy runs it for you (`vercel.json` sets it as the build command) and the
Docker build runs the leak check; anywhere else, run it yourself.

## Deploy

The production build shows a placeholder page until you replace `src/routes/index.tsx`; the development overview
and "Try it" are never part of a production build.

- **Vercel:** `vercel env add BYTECHEF_URL production`, `vercel env add BYTECHEF_APP_KEY production`, then
  `vercel deploy --prod`. The build runs `npm run check`, so a leak or a development-only build fails the deploy.
- **Docker:** `docker build -t my-app .` then
  `docker run -e BYTECHEF_URL=https://… -e BYTECHEF_APP_KEY=… -p 3000:3000 my-app`. The image is `node:24-alpine`
  and installs with `npm ci`; if the lockfile was written by an npm version whose `npm ci` it does not satisfy, the
  build prints `package-lock.json does not match this npm version` and falls back to `npm install`, which keeps the
  locked versions.
- After rotating the key in ByteChef, update the host's variable and redeploy.

See `AGENTS.md` for how features are built.
