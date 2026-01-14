# ByteChef Embedded SDK

This workspace contains the ByteChef Embedded SDK for integrating workflow automation capabilities into your applications.

## Structure

- **library/react/** - The React SDK library for embedding ByteChef workflows
- **test-apps/react/** - Next.js test application for development and testing

## Quick Start

### For Development

```bash
# Install dependencies
npm install

# Set up npm link for hot reload development
npm run setup:link

# Start development (runs library watch + test app)
npm run dev
```

Then open http://localhost:3000 in your browser.

### For Testing Publishing

```bash
# Start local registry
npm run registry:start

# Publish library to local registry
npm run publish:library

# Install in test app
npm run install:test-app
```

## Documentation

- [DEVELOPMENT.md](DEVELOPMENT.md) - Detailed development guide with hot reload setup
- [library/react/README.md](library/react/README.md) - Library documentation

## Package Versions

All packages are synchronized with the following versions:

- **Storybook**: 10.1.11
- **Vite**: 7.3.1
- **React**: 19.2.3
- **Next.js**: 16.1.1
- **TypeScript**: 5.7.3

## Key Features

- ✅ Hot reload development with npm link
- ✅ Local registry support with Verdaccio
- ✅ Storybook 10 for component development
- ✅ Vite 7 for fast builds
- ✅ TypeScript support
- ✅ Testing with Vitest

## Available Scripts

| Script                     | Description                                     |
| -------------------------- | ----------------------------------------------- |
| `npm run dev`              | Run library watch + test app dev server         |
| `npm run setup:link`       | Set up npm link for hot reload                  |
| `npm run registry:start`   | Start local npm registry                        |
| `npm run registry:stop`    | Stop local registry                             |
| `npm run publish:library`  | Publish library to local registry               |
| `npm run install:test-app` | Install library in test app from local registry |
| `npm run build:all`        | Build the library                               |
| `npm run clean`            | Clean all build artifacts                       |

## Contributing

See [DEVELOPMENT.md](DEVELOPMENT.md) for detailed development workflows.

## License

MIT
