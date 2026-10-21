const APP_PACKAGE = '@bytechef/app';
const APP_ENTRY = `node_modules/${APP_PACKAGE}`;
const DEPENDENCY_FIELDS = ['dependencies', 'devDependencies'];
const PUBLIC_REGISTRY_HOST = 'registry.npmjs.org';

function withoutAppPackage(dependencies = {}) {
    return Object.fromEntries(Object.entries(dependencies).filter(([name]) => name !== APP_PACKAGE));
}

export function lockfilePinProblems(packageJson, lockfile) {
    const root = lockfile.packages?.[''] ?? {};
    const problems = [];

    for (const field of DEPENDENCY_FIELDS) {
        const pinned = withoutAppPackage(packageJson[field]);
        const locked = root[field] ?? {};

        for (const name of new Set([...Object.keys(pinned), ...Object.keys(locked)])) {
            if (pinned[name] !== locked[name]) {
                problems.push(
                    `package-lock.json lists ${field}.${name} as ${locked[name]}, package.json as ${pinned[name]}`
                );
            }
        }

        for (const [name, version] of Object.entries(pinned)) {
            const lockedVersion = lockfile.packages?.[`node_modules/${name}`]?.version;

            if (lockedVersion !== version) {
                problems.push(`package-lock.json locks ${name} at ${lockedVersion}, package.json pins ${version}`);
            }
        }
    }

    for (const [path, {resolved}] of Object.entries(lockfile.packages ?? {})) {
        if (resolved !== undefined && URL.parse(resolved)?.host !== PUBLIC_REGISTRY_HOST) {
            problems.push(`package-lock.json resolves ${path} from ${resolved}, not ${PUBLIC_REGISTRY_HOST}`);
        }
    }

    return problems;
}

function isNested(path) {
    return path.indexOf('node_modules/') !== path.lastIndexOf('node_modules/');
}

function isAppEntry(path) {
    return path === APP_ENTRY || path.startsWith(`${APP_ENTRY}/`);
}

function isOptionalPeer(path, packages) {
    const separator = path.lastIndexOf('node_modules/');
    const prefix = path.slice(0, separator);
    const name = path.slice(separator + 'node_modules/'.length);

    return Object.entries(packages).some(
        ([dependent, entry]) => dependent.startsWith(prefix) && entry.peerDependenciesMeta?.[name]?.optional === true
    );
}

export function installDrift(templateLockfile, installedLockfile) {
    const before = templateLockfile.packages ?? {};
    const after = installedLockfile.packages ?? {};
    const changed = Object.keys(before)
        .filter((path) => path in after && before[path].version !== after[path].version)
        .map((path) => `${path}: ${before[path].version} -> ${after[path].version}`);
    const added = Object.keys(after).filter((path) => !(path in before));
    const removed = Object.keys(before).filter((path) => !(path in after));

    const optionalPeersAdded = added.filter((path) => !isAppEntry(path) && isOptionalPeer(path, after));

    return {
        added,
        changed,
        optionalPeersAdded,
        removed,
        unexpectedAdded: added.filter((path) => !isAppEntry(path) && !optionalPeersAdded.includes(path)),
        unexpectedRemoved: removed.filter((path) => !isNested(path)),
    };
}
