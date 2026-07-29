# @bytechef/embedded

> Previously published as `@bytechef/embedded-react`. That package is deprecated — use this one.

## Install

```bash
npm install @bytechef/embedded
```

Requires `react` and `react-dom` >= 19.2.3 as peer dependencies.

## Features

- ⚛️ **React** component library with **TypeScript**.

- 🏗️ **Vite** as development environment.

- 🌳 **Tree shaking**, for not distributing dead-code.

- 📚 **Storybook** for live viewing the components.

- 🖌️ **CSS Modules** in development, compiled CSS for production builds.

- 🧪 Testing with **Vitest** and **React Testing Library**.

- ✅ Code quality tools with **ESLint**, **Prettier** and **Stylelint**.

## Development

### Local Development Against a Consumer App

The workspace (`sdks/frontend/embedded`) uses a local [Verdaccio](https://verdaccio.org/) registry
and `npm link` -- there are no `yalc:*` scripts.

#### Option A: npm link (quickest)

From the workspace root (`sdks/frontend/embedded`):

```bash
npm run setup:link
```

This builds the library, runs `npm link` in it, and links `@bytechef/embedded` into
`test-apps`. Inside the library you can also run `npm run link:local` / `npm run unlink:local`
directly.

#### Option B: Local Verdaccio registry (closest to a real publish)

```bash
# from the workspace root
npm run registry:start        # starts Verdaccio on http://localhost:4873
npm run publish:library       # builds and publishes @bytechef/embedded to it
npm run install:test-app      # installs the published package into test-apps
npm run registry:stop
```

Inside the library, `npm run publish:local` publishes to the running registry.

#### Watch mode

From the workspace root, `npm run dev` runs the library in watch mode (`npm run watch`)
concurrently with the `test-apps` Next.js dev server.

#### Suggested workflow steps

1. In the ByteChef `DesktopSidebar` component initialize the dialog with `const {openDialog} = useConnectDialog({options})`
   a. `options` are described in `UseConnectDialogProps`
2. `cd ~/.../bytechef/sdks/frontend/embedded`
3. Run `npm run dev`
4. On change inside the `sdk/index.tsx` the ByteChef dev server needs to be restarted to see the changes
   a. This is because of Vite's caching

#### Troubleshooting

Most common error is the `Incompatible React versions`:

```
Uncaught Error: Incompatible React versions: The "react" and "react-dom" packages must have the exact same version. Instead got:
  - react:      19.2.0
  - react-dom:  19.1.1
```

To fix this run these commands in both `client/` and `sdks/.../library/` (make sure both client servers are not running):

```
rm -rf node_modules
rm package-lock.json
npm cache clean --force
npm install
```

## 🤖 Scripts

|      Script       | Function                                                                                                                               |
| :---------------: | -------------------------------------------------------------------------------------------------------------------------------------- |
|      `build`      | Build the `dist`, with types declarations, after checking types with TypeScript.                                                       |
|      `lint`       | Lint the project with **Eslint**.                                                                                                      |
|    `lint:fix`     | Lint and fix the project with **Eslint**.                                                                                              |
|     `format`      | Check the project format with **Prettier**.                                                                                            |
|   `format:fix`    | Format the project code with **Prettier**.                                                                                             |
|    `stylelint`    | Lint the styles code with **Stylelint**.                                                                                               |
|  `stylelint:fix`  | Lint and fix the styles code with **Stylelint**.                                                                                       |
|    `storybook`    | Start a Storybook development server.                                                                                                  |
| `build-storybook` | Build the Storybook `dist`.                                                                                                            |
|      `test`       | Run the tests with **Vitest** using `jsdom` and starts a **Vitest UI** dev server.                                                     |
|    `coverage`     | Generate a coverage report, with **v8**.                                                                                               |
|      `watch`      | Rebuilds the project and watches for file changes to trigger automatic rebuilds.                                                       |
| `publish:local`   | Publish the built package to the local Verdaccio registry (`npm run registry:start` at the workspace root).                            |
|   `link:local`    | `npm link` the library for consumption via `npm link @bytechef/embedded`.                                                              |
| `unlink:local`    | Remove the global `npm link` registration.                                                                                             |
