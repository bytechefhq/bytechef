import {basename, resolve} from 'node:path';

const NAME_RULES = [
    [/^[a-z0-9]/, 'must start with a lower-case letter or digit'],
    [/^[a-z0-9][a-z0-9._-]*$/, 'may contain only lower-case letters, digits, ".", "_" and "-"'],
];
const MAXIMUM_NAME_LENGTH = 214;
const SCOPED_NAME = /^@([^/]*)\/([^/]*)$/;
const LINE_BREAK = /[\r\n]/;
const TRAILING_SLASHES = /\/+$/;

export function validatePackageName(name) {
    if (name.length > MAXIMUM_NAME_LENGTH) {
        throw new Error(`"${name}" is not a valid package name: it must be at most ${MAXIMUM_NAME_LENGTH} characters`);
    }

    const scoped = SCOPED_NAME.exec(name);
    const parts = scoped ? [scoped[1], scoped[2]] : [name];

    for (const part of parts) {
        for (const [pattern, rule] of NAME_RULES) {
            if (!pattern.test(part)) {
                throw new Error(`"${name}" is not a valid package name: it ${rule}`);
            }
        }
    }

    return name;
}

export function resolveTarget(directory, cwd = process.cwd()) {
    const withoutTrailingSlashes = directory.replace(TRAILING_SLASHES, '');
    const scoped = SCOPED_NAME.exec(withoutTrailingSlashes);

    if (scoped) {
        return {
            directory: scoped[2],
            packageName: validatePackageName(withoutTrailingSlashes),
            targetDir: resolve(cwd, scoped[2]),
        };
    }

    const targetDir = resolve(cwd, directory);

    return {directory, packageName: validatePackageName(basename(targetDir)), targetDir};
}

export function detectPackageManager(env = process.env) {
    const agent = env.npm_config_user_agent ?? '';

    for (const candidate of ['pnpm', 'yarn', 'bun']) {
        if (agent.startsWith(`${candidate}/`)) {
            return candidate;
        }
    }

    return 'npm';
}

export function buildEnvLocal({env = process.env, url}) {
    const key = env.BYTECHEF_APP_KEY ?? '';
    const bytechefUrl = url ?? env.BYTECHEF_URL ?? '';

    if (LINE_BREAK.test(bytechefUrl)) {
        throw new Error('The ByteChef URL must not contain a line break');
    }

    if (LINE_BREAK.test(key)) {
        throw new Error('BYTECHEF_APP_KEY must not contain a line break');
    }

    return {
        content: `BYTECHEF_URL=${bytechefUrl}\nBYTECHEF_APP_KEY=${key}\n`,
        keyFromEnvironment: key.length > 0,
    };
}

export const MINIMUM_APP_NODE_VERSION = '22.18.0';

export function nodeVersionWarning(version = process.versions.node) {
    const [major, minor] = version.split('.').map(Number);
    const [minimumMajor, minimumMinor] = MINIMUM_APP_NODE_VERSION.split('.').map(Number);

    if (major > minimumMajor || (major === minimumMajor && minor >= minimumMinor)) {
        return null;
    }

    return `The generated app needs Node.js ${MINIMUM_APP_NODE_VERSION} or newer (24 recommended); this is ${version}. Its development server fails on older Node.js 22 releases.`;
}
