# ByteChef app

A TanStack Start app that calls ByteChef — actions, tools, workflows and HTTP through bound connections — with one
app key. Third-party systems are reached only through ByteChef; this app holds no vendor credentials.

## Run

Requires Node.js 22.18 or newer (24 recommended): the development server fails on older Node.js 22 releases.

1. Run `bytechef automation app create --name my-app` here (it creates the app in ByteChef's DEVELOPMENT environment
   and writes `BYTECHEF_URL` and `BYTECHEF_APP_KEY` into `.env.local` and the app's id and instance id into
   `.bytechef/app.json`; its first instance requires sign-in unless you pass `--user-auth-mode NONE`, and gets `NONE`
   when the OAuth2 authorization server is off; see `AGENTS.md`), or copy `.env.example` to
   `.env.local` and set both, taking the key from an app instance in ByteChef (Rotate key). The shell environment wins over
   `.env.local`: an exported `BYTECHEF_APP_KEY` replaces the file's value. The scaffolder also wrote a random
   `BYTECHEF_SESSION_SECRET` into `.env.local`; if you copied `.env.example`, set it yourself
   (`openssl rand -base64 48`). It seals the session cookie of signed-in users, stays on the server and is never
   committed.
   Before the first sign-in, set where the app runs:
   `bytechef automation app instance update --url http://localhost:3000`. Until the instance has a URL it has no
   sign-in client, and `/auth/login` answers 503 saying so.
   On a local ByteChef server, sign-in redirects to the dev `public-url` (`http://127.0.0.1:5173`, the Studio client dev
   server), so that client dev server must be running.
2. `npm run dev` — in development the home page lists what this app may call and lets you try an action.
3. `npm run -s bytechef:status` — the same, as JSON (for scripts and coding agents).

## Sign-in

Whether users sign in is set on the app instance in ByteChef, not in this code (`userAuthMode` in
`npm run -s bytechef:status`): with `REQUIRED` a visitor is sent to sign in first, with `OPTIONAL` pages render with or
without a user and offer a Sign in link, and with `NONE` nothing changes. `src/start.ts` registers the SDK's request
middleware (a CSRF check for server functions, then sign-in), and `src/routes/auth/` holds the login, callback and
logout routes; server functions read the user from `context.user`. Sign-out clears this app's session only; it does
not revoke the user's tokens. Behind a reverse proxy, set `APP_URL` to the app's public URL so the callback address
and the cookie's `Secure` flag are right; it must be an absolute URL with scheme, such as `https://crm.example.com`, and
an invalid value makes every request fail. See `AGENTS.md` for the rules.

The app needs ByteChef reachable to serve pages: the SDK reads the app's settings on every request, cached for about 60
seconds, and when ByteChef is unreachable and the cache is cold, pages answer 500 in every sign-in mode, `NONE`
included.

## Dependencies

`package-lock.json` locks every dependency, direct and transitive, so each npm install of this app gets the versions
it was tested with; commit it. It does not list `@bytechef/app` until the first install adds it, so if the app was
scaffolded with `--no-install`, run `npm install` once before deploying (the Docker build's `npm ci` needs the
complete lockfile). An app scaffolded for pnpm, yarn or bun has no `package-lock.json`: those package managers
resolve the transitive dependencies afresh.

## Check

`npm run check` typechecks, builds, and verifies that no secret reached the browser bundle (the app key,
`BYTECHEF_SESSION_SECRET` and the session cookie names), that no development-only server function was compiled, and
that the public ready-made server functions from `@bytechef/app/tanstack-start/server-fns` are not in the build. If
you do want those, set `"bytechef": {"allowReadyMadeServerFns": true}` in `package.json`; the check then prints a
warning instead of failing. Run it before every deploy. A Vercel deploy runs it for you (`vercel.json` sets it as the
build command) and the Docker build runs the leak check; anywhere else, run it yourself.

## Deploy

The production build shows a placeholder page until you replace `src/routes/index.tsx`; the development overview
and "Try it" are never part of a production build.

- **Vercel:** `vercel env add BYTECHEF_URL production`, `vercel env add BYTECHEF_APP_KEY production`,
  `vercel env add BYTECHEF_SESSION_SECRET production`, and `vercel env add APP_URL production` with the app's public
  address (an absolute URL such as `https://crm.example.com`) when it is reached through a custom domain, then
  `vercel deploy --prod`. Reach the app only through the origin of `APP_URL`. The build runs `npm run check`, so a leak
  or a development-only build fails the deploy.
- **Docker:** `docker build -t my-app .` then
  `docker run -e BYTECHEF_URL=https://… -e BYTECHEF_APP_KEY=… -e BYTECHEF_SESSION_SECRET=… -p 3000:3000 my-app`
  (add `-e APP_URL=https://…` behind a reverse proxy). The image is `node:24-alpine`
  and installs with `npm ci`; if the lockfile was written by an npm version whose `npm ci` it does not satisfy, the
  build prints `package-lock.json does not match this npm version` and falls back to `npm install`, which keeps the
  locked versions.
- After rotating the key in ByteChef, update the host's variable and redeploy.
- The instance URL registered in ByteChef must be the app's public URL: sign-in returns to `<that URL>/auth/callback`.

### Tell ByteChef about the deployment

Every command below needs a profile (`--profile`, or `--host` and `--token`) and addresses an app instance: from the
linked directory, or with `--app-id` and `--instance-id`. `list` and `status` work in every environment; so do the
deployment commands, with `APP_DEPLOY` in that environment.

- **Record what CI deployed** (any host): `bytechef automation app deployment record --version <name> --commit <sha>
  --url <url>`. Recording an existing version name with a different commit is refused. List them with
  `bytechef automation app deployment list`, and the versions with `bytechef automation app version list`.
- **Let ByteChef deploy it:** set the instance's hosting target and its deployment connection and settings in ByteChef
  (a Vercel instance needs a Vercel connection and `projectId` and `repository`; a self-hosted instance needs an HTTP
  Client connection and `deployUrl`; the CLI sets them with `bytechef automation app instance deployment-settings set
  --connection-id <id> --setting key=value`, in DEVELOPMENT only), then run `bytechef automation app deployment deploy
  --version <name> --commit <sha> --wait`. `--wait` polls until the deployment is READY, FAILED or CANCELED (up to
  `--timeout` seconds, default 600) and exits 1 on a failure. `deployment rollback --version <name>` deploys an earlier
  version again and `deployment cancel` stops one in progress.
- **Self-hosted without writing an endpoint:** `npm run deploy-agent` runs the reference deploy agent
  (`scripts/deploy-agent.mjs`, one app on one machine). Set `DEPLOY_AGENT_TOKEN` (at least 32 characters) and
  `DEPLOY_COMMAND`, put it behind HTTPS, and use its URL as the instance's `deployUrl`; `AGENTS.md` describes the rest.

### Reaching people

Members of the instance's workspace open a published app from the App Portal in ByteChef (an administrator turns on
"Show in App Portal" on the app, and the instance needs an `ACTIVE` status and a URL). The portal lists only apps the
member may use, so who can open the app follows the instance's visibility and the `APP_USE` scope.

See `AGENTS.md` for how features are built.
