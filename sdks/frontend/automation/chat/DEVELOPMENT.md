# Development Guide for @bytechef/automation-chat

This guide explains how to develop and test the `@bytechef/automation-chat` library with hot reload capabilities.

## Table of Contents

1. [Setup Options](#setup-options)
2. [Option 1: npm link (Recommended for Hot Reload)](#option-1-npm-link-recommended-for-hot-reload)
3. [Option 2: Local Registry with Verdaccio](#option-2-local-registry-with-verdaccio)
4. [Development Workflow](#development-workflow)
5. [Troubleshooting](#troubleshooting)

## Setup Options

There are two main approaches for local development:

1. **npm link** - Direct symlink, best for hot reload during active development
2. **Verdaccio** - Local npm registry, best for testing the full publishing workflow

## Option 1: npm link (Recommended for Hot Reload)

This approach creates a symlink between the library and test app, allowing immediate hot reload when you make changes.

### Initial Setup

```bash
# From the automation/chat directory
npm install

# Build the library and create a global symlink
npm run setup:link
```

This command does the following:

1. Builds the library
2. Creates a global npm link for `@bytechef/automation-chat`
3. Links the test app to use the globally linked library

### Development Workflow

Open **two terminal windows**:

**Terminal 1 - Library (with watch mode):**

```bash
cd library
npm run watch
```

**Terminal 2 - Test App:**

```bash
cd test-app
npm run dev
```

Now:

- Make changes to the library source code in `library/src/`
- Vite will automatically rebuild the library
- Next.js will detect the changes and hot reload the test app
- See your changes immediately in the browser

### Cleanup

To unlink the library:

```bash
# From test-app directory
npm run unlink:library

# From library directory
npm run unlink:local
```

## Option 2: Local Registry with Verdaccio

This approach sets up a local npm registry, useful for testing the full publish/install workflow.

### Prerequisites

Install dependencies:

```bash
# From the automation/chat directory
npm install
```

### Start the Local Registry

**Terminal 1 - Registry Server:**

```bash
npm run registry:start
```

This starts Verdaccio on `http://localhost:4873`. Keep this terminal running.

### Publish the Library

**Terminal 2 - Publish:**

```bash
# Build and publish to local registry
npm run publish:library
```

### Install in Test App

```bash
# Install from local registry
npm run install:test-app

# Start the test app
cd test-app
npm run dev
```

### Development Workflow with Registry

When using the local registry, you need to republish after changes:

1. Make changes to library code
2. Rebuild and republish:
    ```bash
    cd library
    npm run build
    npm run publish:local
    ```
3. Reinstall in test app:
    ```bash
    cd ../test-app
    npm install @bytechef/automation-chat@latest --registry http://localhost:4873
    ```
4. Next.js will hot reload with the new version

### Configure Registry Scope

To automatically use the local registry for `@bytechef` packages, edit `.npmrc`:

```bash
# Uncomment this line in .npmrc
@bytechef:registry=http://localhost:4873
```

Then you can use standard npm commands:

```bash
npm install @bytechef/automation-chat@latest
```

### Stop the Registry

```bash
npm run registry:stop
```

## Development Workflow

### Hot Reload Development (npm link)

This is the **recommended approach** for active development:

```bash
# One-time setup
npm run setup:link

# Development (in separate terminals)
cd library && npm run watch     # Terminal 1
cd test-app && npm run dev      # Terminal 2
```

**Advantages:**

- ✅ Instant hot reload
- ✅ See changes immediately
- ✅ No need to republish
- ✅ Best developer experience

**When to use:**

- Active feature development
- Debugging issues
- Rapid iteration

### Testing the Full Build

Occasionally test the full build process:

```bash
# Build the library
cd library
npm run build

# Test the built output
npm run test
```

### Before Publishing to npm

Before publishing to the real npm registry, test with Verdaccio:

```bash
# Start local registry
npm run registry:start

# Publish to local registry
npm run publish:library

# Install and test in test-app
npm run install:test-app
cd test-app && npm run build
```

## Project Structure

```
automation/chat/
├── library/                    # The main SDK library
│   ├── src/                   # Source code
│   ├── dist/                  # Built output (generated)
│   └── package.json
├── test-app/                  # Next.js test application
│   ├── app/                   # Next.js pages
│   └── package.json
├── verdaccio.config.yaml      # Local registry config
├── .npmrc                     # NPM configuration
├── package.json               # Workspace root
└── DEVELOPMENT.md             # This file
```

## Available Scripts

### Workspace Root (`automation/chat/`)

| Script                     | Description                                           |
| -------------------------- | ----------------------------------------------------- |
| `npm run dev`              | Run both library watch mode and test app concurrently |
| `npm run setup:link`       | One-time setup for npm link development               |
| `npm run registry:start`   | Start Verdaccio local registry                        |
| `npm run registry:stop`    | Stop Verdaccio                                        |
| `npm run publish:library`  | Build and publish library to local registry           |
| `npm run install:test-app` | Install library from local registry into test app     |
| `npm run build:all`        | Build the library                                     |
| `npm run clean`            | Clean all build artifacts and node_modules            |

### Library (`library/`)

| Script                  | Description                              |
| ----------------------- | ---------------------------------------- |
| `npm run build`         | Build the library once                   |
| `npm run watch`         | Build and watch for changes (hot reload) |
| `npm run link:local`    | Create global npm link                   |
| `npm run unlink:local`  | Remove global npm link                   |
| `npm run publish:local` | Publish to local registry                |
| `npm run test`          | Run tests                                |
| `npm run test:watch`    | Run tests in watch mode                  |

### Test App (`test-app/`)

| Script                   | Description                              |
| ------------------------ | ---------------------------------------- |
| `npm run dev`            | Start Next.js development server         |
| `npm run link:library`   | Link to global @bytechef/automation-chat |
| `npm run unlink:library` | Unlink from global package               |
| `npm run install:local`  | Install from local registry              |
| `npm run build`          | Build production test app                |

## Troubleshooting

### Changes not appearing in test app

**Using npm link:**

1. Ensure `npm run watch` is running in library directory
2. Check that the test app is linked: `ls -la test-app/node_modules/@bytechef/automation-chat` (should show symlink)
3. Try restarting the Next.js dev server

**Using Verdaccio:**

1. You need to republish after changes
2. Ensure test app is installing from local registry: check `@bytechef:registry` in `.npmrc`

### "Cannot find module @bytechef/automation-chat"

1. Check if the library is linked or installed:

    ```bash
    cd test-app
    ls -la node_modules/@bytechef
    ```

2. Try reinstalling:

    ```bash
    # If using npm link
    npm run setup:link

    # If using registry
    npm run install:test-app
    ```

### Port 4873 already in use

Another Verdaccio instance may be running:

```bash
npm run registry:stop
# or manually
pkill -f verdaccio
```

### Build errors after switching methods

Clean and rebuild:

```bash
npm run clean
npm install
npm run setup:link  # or your preferred method
```

### Hot reload not working

1. Ensure library watch mode is running: `cd library && npm run watch`
2. Check Vite output for build errors
3. Restart Next.js dev server: `cd test-app && npm run dev`
4. Check symlink exists: `ls -la test-app/node_modules/@bytechef/automation-chat`

### Type errors in test app

The test app may cache old type definitions:

```bash
cd test-app
rm -rf .next
npm run dev
```

## Best Practices

1. **Use npm link for active development** - It provides the best developer experience with instant hot reload

2. **Build regularly** - Even when using watch mode, occasionally run a full build to catch any build-specific issues

3. **Test the production build** - Before publishing, test with `npm run build` in both library and test-app

4. **Use Verdaccio before publishing** - Test the full publish workflow with the local registry before publishing to npm

5. **Keep terminals organized** - Use separate terminals for:

    - Library watch mode
    - Test app dev server
    - Registry server (if using Verdaccio)

6. **Clean when switching methods** - If switching between npm link and registry, run `npm run clean` and reinstall

## Tips

- **Fast iteration**: Use `npm link` + `npm run watch` for fastest development cycle
- **Test publishing**: Use Verdaccio to test the full npm publish workflow
- **Debugging**: Check the Next.js console and browser console for errors
- **Version conflicts**: If you see React version conflicts, ensure peer dependencies match

## Next Steps

1. Choose your development approach (npm link recommended)
2. Run the setup command
3. Start development servers
4. Make changes and see them live!

For more information about the library itself, see the main [README.md](README.md).
