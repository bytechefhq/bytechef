// Enforces the Node.js version required to build the client. Vite requires
// Node.js 20.19+ or 22.12+. Without this guard, `vite build` crashes inside
// Node internals with an opaque stack trace (as happened during production
// Docker builds), instead of failing fast with a readable message.

import {pathToFileURL} from 'node:url';

const REQUIRED_DESCRIPTION = 'Node.js 20.19+ or 22.12+';

export function satisfies(version) {
    const [major, minor] = version.split('.').map(Number);

    if (major === 20) {
        return minor >= 19;
    }

    if (major === 22) {
        return minor >= 12;
    }

    return major >= 23;
}

export function assertSupportedNodeVersion(version = process.versions.node) {
    if (satisfies(version)) {
        return;
    }

    const message =
        `\n  Node.js ${version} is not supported for building the client.\n` +
        `  Required: ${REQUIRED_DESCRIPTION}.\n` +
        `  Upgrade Node.js (https://nodejs.org/) and re-run the build.\n`;

    throw new Error(message);
}

const entryPath = process.argv[1];

const isDirectRun = entryPath != null && import.meta.url === pathToFileURL(entryPath).href;

if (isDirectRun) {
    try {
        assertSupportedNodeVersion();
    } catch (error) {
        console.error(error.message);

        process.exit(1);
    }
}
