import {readdirSync, readFileSync, statSync} from 'node:fs';
import {dirname, join, resolve} from 'node:path';
import {fileURLToPath} from 'node:url';

const CANARY_KEY = process.env.BYTECHEF_APP_KEY ?? '';
const CLIENT_DIRS = ['dist/client', '.output/public'];
const FORBIDDEN = ['BYTECHEF_APP_KEY', '/api/app/v1'];
const RPC_MARKER = '_serverFn';
const READY_MADE_SERVER_FNS = [
    'executeAction',
    'executeTool',
    'executeToolByName',
    'executeWorkflow',
    'getWorkflowRun',
    'listConnections',
    'listTools',
    'listWorkflows',
    'proxy',
];

function walk(dir) {
    return readdirSync(dir).flatMap((name) => {
        const path = join(dir, name);

        return statSync(path).isDirectory() ? walk(path) : [path];
    });
}

function readModuleGraph(entryFile) {
    const texts = new Map();
    const pending = [entryFile];

    while (pending.length > 0) {
        const file = pending.pop();

        if (texts.has(file)) {
            continue;
        }

        const text = readFileSync(file, 'utf8');

        texts.set(file, text);

        for (const match of text.matchAll(/(?:from|import)\s*["'](\.\/[^"']+\.js)["']/g)) {
            pending.push(resolve(dirname(file), match[1]));
        }
    }

    return texts;
}

function isDirectory(dir) {
    try {
        return statSync(dir).isDirectory();
    } catch {
        return false;
    }
}

const clientDir = CLIENT_DIRS.find(isDirectory);

if (!clientDir) {
    console.error(`No client build found in ${CLIENT_DIRS.join(' or ')}`);
    process.exit(1);
}

const files = walk(clientDir).filter((file) => /\.(js|mjs|html|css|json|map)$/.test(file));
const problems = [];
let rpcFiles = 0;

for (const file of files) {
    const text = readFileSync(file, 'utf8');

    for (const forbidden of FORBIDDEN) {
        if (text.includes(forbidden)) {
            problems.push(`${file}: contains ${forbidden}`);
        }
    }

    if (CANARY_KEY && text.includes(CANARY_KEY)) {
        problems.push(`${file}: contains the app key value`);
    }

    if (text.includes(RPC_MARKER)) {
        rpcFiles += 1;
    }
}

if (rpcFiles === 0) {
    problems.push(`no client file contains ${RPC_MARKER}: the server functions were not compiled into RPC stubs`);
}

const tanstackStartEntry = fileURLToPath(import.meta.resolve('@bytechef/app/tanstack-start'));
const serverFnsEntry = fileURLToPath(import.meta.resolve('@bytechef/app/tanstack-start/server-fns'));

for (const [file, text] of readModuleGraph(tanstackStartEntry)) {
    if (text.includes('createServerFn(')) {
        problems.push(`${file}: reachable from @bytechef/app/tanstack-start and calls createServerFn(`);
    }
}

const serverFnsText = [...readModuleGraph(serverFnsEntry).values()].join('\n');

for (const name of READY_MADE_SERVER_FNS) {
    if (!new RegExp(`^(?:export )?(?:const|let|var) ${name} = createServerFn\\(`, 'm').test(serverFnsText)) {
        problems.push(`${serverFnsEntry}: no module-level ${name} = createServerFn( declaration`);
    }
}

if (problems.length > 0) {
    console.error(problems.join('\n'));
    process.exit(1);
}

console.log(`Checked ${files.length} client files under ${clientDir}: RPC stubs in ${rpcFiles}, nothing server-only`);
console.log(
    `Checked the library: no createServerFn( reachable from tanstack-start, ${READY_MADE_SERVER_FNS.length} ready-made server functions in server-fns`
);
