# @bytechef/app test app

A blank TanStack Start app that proves the `@bytechef/app` server functions compile and run. The home route calls four
server functions, each during SSR (the route loader) and from the browser (the button):

- an app-defined server function built on `bytechefMiddleware()`
- a ready-made server function from `@bytechef/app/tanstack-start/server-fns`
- a validated ready-made server function (its input validator accepts real RPC input)
- the AI SDK adapter (`toAiSdkTools` from `@bytechef/app/ai-sdk`)

It also wires sign-in: `src/start.ts` registers `bytechefRequestMiddleware()` (the CSRF check for server functions,
then the auth middleware) for every request, the routes `/auth/login`, `/auth/callback` and `/auth/logout` use
`loginHandler`, `callbackHandler` and `logoutHandler`, and the server route `/api/me` answers the signed-in user's
login. Because the auth middleware reads the instance's sign-in mode from ByteChef, every page needs a reachable
ByteChef; an instance in `REQUIRED` mode sends the browser to sign in first.

With an instance in `NONE` mode (or once signed in), a probe whose call ByteChef refuses or cannot complete answers
`ERROR <code>`, which shows the function ran on the server with a client supplied by the middleware.

## Run it

1. `npm run build:all` at the workspace root (the app resolves `@bytechef/app` through the workspace link to
   `library/dist`).
2. Copy `.env.example` to `.env` and fill in `BYTECHEF_URL`, `BYTECHEF_APP_KEY` and `BYTECHEF_SESSION_SECRET` (at
   least 32 random characters).
3. `npm run dev` in this directory.

## Bundle check

`npm run check:test-app` at the workspace root builds and type-checks the app and runs `scripts/check-bundle.mjs`, which
fails when the client bundle contains the app key or session secret variable name, either value, the session cookie
name or the `/api/app/v1` request layer, or
when no RPC stubs were compiled. It also checks the built library: nothing reachable from `@bytechef/app/tanstack-start`
may call `createServerFn(`, and `@bytechef/app/tanstack-start/server-fns` must declare all eleven ready-made server
functions at module level.
