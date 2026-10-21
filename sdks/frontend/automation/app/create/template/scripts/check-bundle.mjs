import {existsSync, readdirSync, readFileSync, statSync} from 'node:fs';
import {join} from 'node:path';
import {parseEnv} from 'node:util';

const CLIENT_DIRS = ['.output/public', '.vercel/output/static', 'dist/client'];
const SERVER_DIRS = ['.output/server', '.vercel/output/functions'];
const DEV_MODULE = 'src/server/dev.ts';
const ENV_FILES = ['.env', '.env.local', '.env.production', '.env.production.local'];
const FORBIDDEN = ['BYTECHEF_APP_KEY', '/api/app/v1'];
const SERVER_FN_URL_PREFIX = '/_serverFn/';
const DEV_CHUNK_IMPORTER = /importer:\s*\(\)\s*=>\s*import\(\s*["']\.\/dev-[A-Za-z0-9_-]{8}\.mjs["']\s*\)/;
const KEY_NAME_SUFFIX = 'BYTECHEF_APP_KEY';
const READY_MADE_SERVER_FNS = [
    'callApi',
    'executeAction',
    'executeTool',
    'executeToolByName',
    'executeWorkflow',
    'getWorkflowRun',
    'listApiCollections',
    'listConnections',
    'listTools',
    'listWorkflows',
    'proxy',
].map((name) => ({name, pattern: new RegExp(`(?<![\\w$])${name}_createServerFn_handler(?![\\w$])`)}));

function fail(message) {
    console.error(message);
    process.exit(1);
}

function readDevOnlyServerFns() {
    if (!existsSync(DEV_MODULE)) {
        fail(`${DEV_MODULE} is missing; it declares the development-only server functions this check looks for`);
    }

    const names = [...readFileSync(DEV_MODULE, 'utf8').matchAll(/export const (\w+) = createServerFn/g)].map(
        (match) => match[1]
    );

    if (names.length === 0) {
        fail(`${DEV_MODULE} declares no server functions (export const <name> = createServerFn); the check cannot run`);
    }

    return names.map((name) => `${name}_createServerFn_handler`);
}

function readAllowReadyMadeServerFns() {
    if (!existsSync('package.json')) {
        return false;
    }

    return JSON.parse(readFileSync('package.json', 'utf8')).bytechef?.allowReadyMadeServerFns === true;
}

function keyValuesOf(variables) {
    return Object.entries(variables)
        .filter(([name]) => name.endsWith(KEY_NAME_SUFFIX))
        .map(([, value]) => value ?? '');
}

function readEnvFileKeys() {
    return ENV_FILES.filter((file) => existsSync(file)).flatMap((file) =>
        keyValuesOf(parseEnv(readFileSync(file, 'utf8')))
    );
}

function existingDirs(dirs) {
    return dirs.filter((dir) => existsSync(dir) && statSync(dir).isDirectory());
}

function walk(dir) {
    return readdirSync(dir).flatMap((name) => {
        const path = join(dir, name);

        return statSync(path).isDirectory() ? walk(path) : [path];
    });
}

const devOnlyServerFns = readDevOnlyServerFns();
const allowReadyMadeServerFns = readAllowReadyMadeServerFns();
const clientDirs = existingDirs(CLIENT_DIRS);
const serverDirs = existingDirs(SERVER_DIRS);
const clientDirList = clientDirs.join(', ');
const serverDirList = serverDirs.join(', ');

if (clientDirs.length === 0) {
    fail(`No client build found in ${CLIENT_DIRS.join(' or ')}; run npm run build first`);
}

if (serverDirs.length === 0) {
    fail(`No server build found in ${SERVER_DIRS.join(' or ')}; run npm run build first`);
}

const keyValues = [...new Set([...keyValuesOf(process.env), ...readEnvFileKeys()])].filter((value) => value.length > 0);
const problems = [];
const clientFiles = clientDirs.flatMap(walk).filter((file) => /\.(js|mjs|html|css|json|map)$/.test(file));
let serverFnUrlFound = false;

for (const file of clientFiles) {
    const text = readFileSync(file, 'utf8');

    if (text.includes(SERVER_FN_URL_PREFIX)) {
        serverFnUrlFound = true;
    }

    for (const forbidden of FORBIDDEN) {
        if (text.includes(forbidden)) {
            problems.push(`${file}: contains ${forbidden}`);
        }
    }

    for (const value of keyValues) {
        if (text.includes(value)) {
            problems.push(`${file}: contains the app key value`);
        }
    }
}

if (!serverFnUrlFound) {
    problems.push(
        `${clientDirList}: no file contains ${SERVER_FN_URL_PREFIX}; this does not look like a TanStack Start client build`
    );
}

const serverFiles = serverDirs.flatMap(walk).filter((file) => /\.(js|mjs)$/.test(file));
const readyMadeFound = new Set();

if (serverFiles.length === 0) {
    problems.push(`${serverDirList}: no server files; run npm run build first`);
}

for (const file of serverFiles) {
    const text = readFileSync(file, 'utf8');

    for (const name of devOnlyServerFns) {
        if (text.includes(name)) {
            problems.push(
                `${file}: contains the development-only server function ${name}; build without NODE_ENV=development`
            );
        }
    }

    for (const {name, pattern} of READY_MADE_SERVER_FNS) {
        if (pattern.test(text)) {
            readyMadeFound.add(name);
        }
    }

    if (DEV_CHUNK_IMPORTER.test(text)) {
        problems.push(`${file}: registers server functions from a dev- chunk; build without NODE_ENV=development`);
    }

    for (const value of keyValues) {
        if (text.includes(`${KEY_NAME_SUFFIX}=${value}`)) {
            problems.push(`${file}: contains the app key value`);
        }
    }
}

const readyMadeMessage =
    `${serverDirList} registers the public ready-made server functions from @bytechef/app/tanstack-start/server-fns ` +
    `(${[...readyMadeFound].sort().join(', ')}); each one lets any visitor run allowed operations.`;

if (readyMadeFound.size > 0 && !allowReadyMadeServerFns) {
    problems.push(
        `${readyMadeMessage} If the human asked for them, set "bytechef": {"allowReadyMadeServerFns": true} in package.json. ` +
            'A server function of your own with one of these names also matches; rename it.'
    );
}

if (problems.length > 0) {
    fail(problems.join('\n'));
}

if (readyMadeFound.size > 0) {
    console.warn(`WARNING: ${readyMadeMessage} Allowed by "bytechef.allowReadyMadeServerFns" in package.json.`);
}

console.log(
    `Checked ${clientFiles.length} client files under ${clientDirList} and ${serverFiles.length} server files under ` +
        `${serverDirList}: ` +
        'no app key name or value and no ByteChef API path in the client; no development-only' +
        `${readyMadeFound.size > 0 ? '' : ' or ready-made'} server functions on the server`
);
