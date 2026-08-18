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

## Components

### AutomationHub

Embeds the end-user Automation Hub in an iframe: an Automations tab (the published template catalog plus the user's own automations) and a Connections tab, with the workflow builder as an internal route reached from the Automations tab.

```tsx
<AutomationHub
    baseUrl="https://app.bytechef.io"
    className="h-[800px] w-full"
    environment="PRODUCTION"
    jwtToken={jwtToken}
    tabs={{connections: true, newWorkflow: false}}
    theme={{mode: 'light', primaryColor: '#2563eb', borderRadius: '0.5rem'}}
/>
```

`theme.fontFamily` must be a font the iframe can load (system/web-safe); host-page `@font-face` does not cross the iframe boundary.

## Development

### Local Development Against a Consumer App

The workspace (`sdks/frontend/embedded`) uses a local [Verdaccio](https://verdaccio.org/) registry
and `npm link`.

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

Develop against the embedded sample app in [`../test-apps`](../test-apps) -- a standalone Next.js
consumer that calls `useConnectDialog` the same way a customer's app would. Do **not** wire the SDK
into the ByteChef client (`client/`) itself; that is not a consumer of this package.

1. Start the ByteChef server and client, since the sample app opens the connect dialog served by the
   client (`http://127.0.0.1:5173` -- the sample app's default Base URL).
2. `cd ~/.../bytechef/sdks/frontend/embedded`
3. `npm run setup:link` (one time -- builds the library and links it into `test-apps`)
4. `npm run dev` -- runs the library in watch mode plus the sample app on http://localhost:3000
5. In the sample app, fill in Key ID / Private Key / External User ID, click **Calculate JWT Token**,
   pick an integration, then **Connect** to open the dialog.
6. Edit `library/src/`; Vite rebuilds and Next.js hot reloads the sample app.

The sample app's `useConnectDialog({baseUrl, environment, integrationId, jwtToken})` call in
`test-apps/app/page.tsx` is the reference usage -- all options are described in
`UseConnectDialogProps`. See [`../test-apps/README.md`](../test-apps/README.md) for how it generates
JWTs server-side, and [`../DEVELOPMENT.md`](../DEVELOPMENT.md) for the full development guide.

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

|      Script       | Function                                                                                                    |
| :---------------: | ----------------------------------------------------------------------------------------------------------- |
|      `build`      | Build the `dist`, with types declarations, after checking types with TypeScript.                            |
|      `lint`       | Lint the project with **Eslint**.                                                                           |
|    `lint:fix`     | Lint and fix the project with **Eslint**.                                                                   |
|     `format`      | Check the project format with **Prettier**.                                                                 |
|   `format:fix`    | Format the project code with **Prettier**.                                                                  |
|    `stylelint`    | Lint the styles code with **Stylelint**.                                                                    |
|  `stylelint:fix`  | Lint and fix the styles code with **Stylelint**.                                                            |
|    `storybook`    | Start a Storybook development server.                                                                       |
| `build-storybook` | Build the Storybook `dist`.                                                                                 |
|      `test`       | Run the tests with **Vitest** using `jsdom` and starts a **Vitest UI** dev server.                          |
|    `coverage`     | Generate a coverage report, with **v8**.                                                                    |
|      `watch`      | Rebuilds the project and watches for file changes to trigger automatic rebuilds.                            |
|  `publish:local`  | Publish the built package to the local Verdaccio registry (`npm run registry:start` at the workspace root). |
|   `link:local`    | `npm link` the library for consumption via `npm link @bytechef/embedded`.                                   |
|  `unlink:local`   | Remove the global `npm link` registration.                                                                  |
