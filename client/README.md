# React Tailwindcss Boilerplate build with Vite

This is a [ReactJS](https://reactjs.org) + [Vite](https://vitejs.dev) boilerplate to be used with [Tailwindcss](https://tailwindcss.com).

## What is inside?

This project uses many tools and libs like:

-   [ReactJS](https://reactjs.org)
-   [Vite](https://vitejs.dev)
-   [TypeScript](https://www.typescriptlang.org)
-   [Vitest](https://vitest.dev/)
-   [Testing Library](https://testing-library.com)
-   [Tailwindcss](https://tailwindcss.com)
-   [Eslint](https://eslint.org)
-   [Prettier](https://prettier.io)
-   [shadcn/ui](https://ui.shadcn.com/)
-   [Radix UI](https://www.radix-ui.com/)
-   [TanStack](https://tanstack.com/)
-   [zustand](https://github.com/pmndrs/zustand)

For details on setting up your development machine, please refer to the [Setup Guide](../CONTRIBUTING.md#client-side)

## HTTPS in Development mode

To enable HTTPS in development mode, create `.env.local` file inside the `client` directory and add `VITE_HTTPS=true`

## Feature Flags

To enable feature flags, create `.env.local` file inside the `client` directory and set to `true` the following feature flags:

-   `VITE_FF_EMBEDDED_TYPE_ENABLED` - enables Embedded mode in UI
