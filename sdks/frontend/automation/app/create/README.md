# create-bytechef-app

Scaffold a ByteChef app: a TanStack Start starter that calls ByteChef through `@bytechef/app`.

## Usage

```bash
npm create bytechef-app@latest my-app -- --url https://bytechef.example.com
```

```bash
pnpm create bytechef-app my-app --url https://bytechef.example.com
```

```bash
npx create-bytechef-app my-app --url https://bytechef.example.com
```

Under `npm create`, npm consumes any flag that is not preceded by `--` as its own option, so flags must follow a
`--` separator. `pnpm create` and `npx create-bytechef-app` pass everything after the package name straight through.
`--help` (or `-h`) prints the usage.

## Arguments and flags

| Argument / flag               | Meaning                                                                                                                               |
| ----------------------------- | ------------------------------------------------------------------------------------------------------------------------------------- |
| `[directory]`                 | Directory to create, default `bytechef-app`; its name becomes the package name, and `@scope/name` creates `name/` named `@scope/name` |
| `--url <url>`                 | ByteChef base URL, http or https, written to `.env.local` (falls back to `BYTECHEF_URL`)                                              |
| `--no-install`                | Skip installing dependencies                                                                                                          |
| `--package-manager <manager>` | One of `npm`, `pnpm`, `yarn`, `bun`; detected from the invoking package manager                                                       |
| `-h`, `--help`                | Print the usage and exit                                                                                                              |

There is no flag for the app key: a key on the command line ends up in shell history and process listings.

## What gets written

The template is copied into the directory (the target must be empty or absent), `_gitignore` and `_dockerignore`
become `.gitignore` and `.dockerignore`, `package.json` gets the directory name and the matching `@bytechef/app`
version, and `.env.local` (mode `0600`) is written with `BYTECHEF_URL` and `BYTECHEF_APP_KEY`. The key is copied only
from a `BYTECHEF_APP_KEY` already set in the environment; otherwise it is left empty.
`.env.local` also gets `BYTECHEF_SESSION_SECRET`, 48 bytes from the operating system's cryptographic random source
encoded as base64url, which seals the session cookies of the app's signed-in users. It is generated for every
scaffold (a value in the shell environment is not used), written only to `.env.local` (which the generated
`.gitignore` and `.dockerignore` exclude) and never printed. A `.env.local` that already has a session secret keeps it.
A line break in the URL or the key is refused before anything is written.

The template carries a `package-lock.json` that locks every transitive dependency. It is kept for npm, whose install
adds `@bytechef/app` to it and changes nothing else. For pnpm, yarn and bun it is removed (the scaffolder prints one
line saying so): those package managers resolve the transitive dependencies afresh and write their own lockfile.
With `--no-install`, the next steps remind you to install before deploying.

## Next steps

```bash
cd my-app
# set BYTECHEF_APP_KEY in .env.local: `bytechef automation app create --name my-app` writes it (and BYTECHEF_URL),
# or copy the key of an app instance in ByteChef (Rotate key)
npm run dev
npm run -s bytechef:status
npm run check
```

To put the app in front of people, deploy it (see the generated app's `README.md` and `AGENTS.md`: Vercel, Docker, or
`bytechef automation app deployment record`) and publish the app to the App Portal in ByteChef; members of the
instance's workspace then open it from there.

`-s` keeps npm's script banner out of the JSON; under pnpm, yarn or bun the scaffolder prints
`<manager> run bytechef:status` instead.
