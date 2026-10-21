import {
    chmodSync,
    cpSync,
    existsSync,
    readdirSync,
    readFileSync,
    renameSync,
    rmSync,
    statSync,
    writeFileSync,
} from 'node:fs';
import {dirname, join, relative} from 'node:path';

const NPM_LOCKFILE = 'package-lock.json';
const RENAMES = [
    ['_gitignore', '.gitignore'],
    ['_dockerignore', '.dockerignore'],
];
const EXCLUDED_NAMES = new Set([
    'node_modules',
    '.output',
    'dist',
    '.tanstack',
    '.vercel',
    '.nitro',
    '.env',
    '.env.local',
]);

function isExcluded(name) {
    return EXCLUDED_NAMES.has(name) || (name.startsWith('.env.') && name.endsWith('.local'));
}

export function writeEnvLocal(targetDir, content) {
    const path = join(targetDir, '.env.local');

    writeFileSync(path, content, {mode: 0o600});
    chmodSync(path, 0o600);
}

export function lockfileRemovedMessage(packageManager) {
    return `Removed ${NPM_LOCKFILE}: ${packageManager} resolves transitive dependencies fresh; npm installs the locked versions.`;
}

export function scaffold({appVersion, envLocal, packageManager = 'npm', packageName, targetDir, templateDir}) {
    if (existsSync(targetDir)) {
        if (!statSync(targetDir).isDirectory()) {
            throw new Error(`${targetDir} exists and is not a directory`);
        }

        if (readdirSync(targetDir).length > 0) {
            throw new Error(`${targetDir} exists and is not empty`);
        }
    }

    cpSync(templateDir, targetDir, {
        filter: (source) => {
            const path = relative(templateDir, source);

            return dirname(path) !== '.' || !isExcluded(path);
        },
        recursive: true,
    });

    for (const [from, to] of RENAMES) {
        renameSync(join(targetDir, from), join(targetDir, to));
    }

    const packageJsonPath = join(targetDir, 'package.json');
    const packageJson = JSON.parse(readFileSync(packageJsonPath, 'utf8'));

    packageJson.name = packageName;
    packageJson.dependencies['@bytechef/app'] = appVersion;

    writeFileSync(packageJsonPath, `${JSON.stringify(packageJson, null, 4)}\n`);

    writeEnvLocal(targetDir, envLocal);

    const lockfilePath = join(targetDir, NPM_LOCKFILE);
    const lockfileRemoved = packageManager !== 'npm' && existsSync(lockfilePath);

    if (lockfileRemoved) {
        rmSync(lockfilePath);
    }

    return {lockfileRemoved};
}
