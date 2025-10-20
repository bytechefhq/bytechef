# bytechef-embedded-react-sdk

## Features

- ⚛️ **React** component library with **TypeScript**.

- 🏗️ **Vite** as development environment.

- 🌳 **Tree shaking**, for not distributing dead-code.

- 📚 **Storybook** for live viewing the components.

- 🖌️ **CSS Modules** in development, compiled CSS for production builds.

- 🧪 Testing with **Vitest** and **React Testing Library**.

- ✅ Code quality tools with **ESLint**, **Prettier** and **Stylelint**.

## Development

### Setting up Yalc for Local Development

[Yalc](https://github.com/wclr/yalc) is a tool for local package development that simulates publishing a package without actually publishing to a registry.

#### Prerequisites

- Install Yalc globally: `npm install -g yalc`

#### Publishing the Library

- **One-time build and publish**:

    ```bash
    npm run yalc:publish
    ```

    This builds the library and publishes it to the local Yalc store.

- **Update after changes:**

    ```
    npm run yalc:push
    ```

    This rebuilds the library and pushes updates to all projects using it.

- **Add the package to your project:**
    ```
    yalc add @bytechef/embedded-react
    npm install
    ```
    This adds the package from your local Yalc store to the project.

#### Suggested workflow steps

1. In the Bytechef `DesktopSidebar` component initialize the dialog with `const {openDialog} = useConnectDialog({options})`
   a. `options` are described in `UseConnectDialogProps`
2. `cd ~/.../bytechef/sdks/frontend/embedded/library/react`
3. Run `npm run dev:yalc`
4. On change inside the `sdk/index.tsx` the Bytechef dev server needs to be restarted to see the changes
   a. This is because of Vite's caching

#### Troubleshooting

Most common error is the `Incompatible React versions`:

```
Uncaught Error: Incompatible React versions: The "react" and "react-dom" packages must have the exact same version. Instead got:
  - react:      19.2.0
  - react-dom:  19.1.1
```

To fix this run these commands in both `client/` and `sdks/.../react/` (make sure both client servers are not running):

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
|    `dev:yalc`     | Rebuilds the project and watches for file changes to trigger automatic rebuilds. Also, publishes it via yalc to be consumed elsewhere. |
