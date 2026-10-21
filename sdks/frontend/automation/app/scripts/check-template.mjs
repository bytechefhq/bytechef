import {spawn, spawnSync} from 'node:child_process';
import {
    existsSync,
    mkdirSync,
    mkdtempSync,
    readdirSync,
    readFileSync,
    renameSync,
    rmSync,
    statSync,
    writeFileSync,
} from 'node:fs';
import {createServer} from 'node:net';
import {tmpdir} from 'node:os';
import {delimiter, join, resolve} from 'node:path';

import {installDrift, lockfilePinProblems} from './template-lockfile.mjs';

const W = resolve(import.meta.dirname, '..');
const CANARY = 'sk-ci-canary-4b9e';
const SESSION_CANARY = 'session-secret-canary-0d1c7a9e5b3f4a2c8e6d';
const SESSION_SECRET_LINE = /^BYTECHEF_SESSION_SECRET=([A-Za-z0-9_-]{64})$/m;
const FIXTURE = join(W, 'scripts/fixtures/app.json');
const MOCK = join(W, 'scripts/mock-bytechef.mjs');
const PRETTIER = join(W, 'node_modules/prettier/bin/prettier.cjs');
const NO_ENV_FILE_MESSAGE = 'No .env.local here; BYTECHEF_URL and BYTECHEF_APP_KEY come from the environment only.\n';
const DEV_ONLY_HANDLERS = ['getAppOverview_createServerFn_handler', 'tryAction_createServerFn_handler'];
const PACK_EXCLUDED = /^template\/(node_modules|\.output|dist|\.tanstack|\.vercel|\.nitro|\.env$|\.env\.local$)/;
const READY_MADE_MESSAGE =
    'registers the public ready-made server functions from @bytechef/app/tanstack-start/server-fns';
const READY_MADE_ROUTE = `import {executeAction} from '@bytechef/app/tanstack-start/server-fns';
import {createFileRoute} from '@tanstack/react-router';

function ReadyMade() {
    return (
        <button onClick={() => executeAction({data: {actionName: 'createContact', bindingKey: 'crm', input: {}}})} type="button">
            Run
        </button>
    );
}

export const Route = createFileRoute('/ready-made')({component: ReadyMade});
`;
const LEAK_ROUTE = (text) => `import {createFileRoute} from '@tanstack/react-router';

function Leak() {
    return <p>{${JSON.stringify(text)}}</p>;
}

export const Route = createFileRoute('/leak')({component: Leak});
`;
const AI_PROVIDER_STUB =
    "declare module '@ai-sdk/anthropic' {\n    export function anthropic(model: string): any;\n}\n";
const RUN_TIMEOUT_MS = 15 * 60_000;
const FETCH_TIMEOUT_MS = 60_000;
const startedAt = Date.now();
const children = new Set();
const NPM_SCRIPT_ENV = new Set([
    'INIT_CWD',
    'npm_command',
    'npm_config_local_prefix',
    'npm_config_prefix',
    'npm_execpath',
    'npm_node_execpath',
]);
const baseEnv = Object.fromEntries(
    Object.entries(process.env).filter(([name]) => !NPM_SCRIPT_ENV.has(name) && !/^npm_(lifecycle|package)_/.test(name))
);

delete baseEnv.BYTECHEF_URL;
delete baseEnv.BYTECHEF_APP_KEY;
delete baseEnv.NODE_ENV;

baseEnv.PATH = (process.env.PATH ?? '')
    .split(delimiter)
    .filter((entry) => !entry.includes('node_modules/.bin') && !entry.includes('node-gyp-bin'))
    .join(delimiter);

function step(name) {
    console.log(`== [${Math.round((Date.now() - startedAt) / 1000)}s] ${name}`);
}

function assert(condition, message) {
    if (!condition) {
        throw new Error(`Assertion failed: ${message}`);
    }
}

function run(command, args, {cwd, env = {}, expectStatus = 0, label}) {
    const result = spawnSync(command, args, {
        cwd,
        encoding: 'utf8',
        env: {...baseEnv, ...env},
        maxBuffer: 64 * 1024 * 1024,
        timeout: RUN_TIMEOUT_MS,
    });

    if (result.status !== expectStatus) {
        const reason = result.error ? ` (${result.error.message})` : '';

        throw new Error(
            `${label}: exit ${result.status}${reason} (expected ${expectStatus})\n${result.stdout}\n${result.stderr}`
        );
    }

    return result;
}

function freePort() {
    return new Promise((resolvePort, reject) => {
        const probe = createServer();

        probe.once('error', reject);
        probe.listen(0, '127.0.0.1', () => {
            const {port} = probe.address();

            probe.close(() => resolvePort(port));
        });
    });
}

function delay(milliseconds) {
    return new Promise((resolveDelay) => setTimeout(resolveDelay, milliseconds));
}

async function waitFor(url, handle, timeoutMs) {
    const deadline = Date.now() + timeoutMs;
    let last = 'no answer';

    while (Date.now() < deadline) {
        if (handle.child.exitCode !== null || handle.child.signalCode !== null) {
            throw new Error(`${url}: the server exited (${handle.child.exitCode ?? handle.child.signalCode})`);
        }

        try {
            const response = await fetch(url, {signal: AbortSignal.timeout(5_000)});

            if (response.status < 500) {
                return;
            }

            last = `status ${response.status}`;
        } catch (error) {
            last = error.message;
        }

        await delay(250);
    }

    throw new Error(`${url} did not answer within ${timeoutMs} ms (${last})`);
}

function spawnGroup(command, args, options) {
    const child = spawn(command, args, {...options, detached: true});

    children.add(child);
    child.once('exit', () => children.delete(child));

    return child;
}

function signalGroup(pid, signal) {
    try {
        process.kill(-pid, signal);
    } catch (error) {
        if (error.code !== 'ESRCH') {
            throw error;
        }
    }
}

function startMock(flags = []) {
    return new Promise((resolveMock, reject) => {
        const child = spawnGroup('node', [MOCK, FIXTURE, ...flags], {
            cwd: W,
            env: baseEnv,
            stdio: ['ignore', 'pipe', 'inherit'],
        });
        let output = '';

        child.stdout.on('data', (chunk) => {
            output += chunk;

            const line = output.split('\n').find((candidate) => candidate.startsWith('{'));

            if (line) {
                resolveMock({child, url: `http://127.0.0.1:${JSON.parse(line).port}`});
            }
        });
        child.once('exit', (code) => reject(new Error(`mock exited with ${code}`)));
    });
}

async function startApp(command, args, {cwd, env, portArgs, portEnv, timeoutMs}) {
    const port = await freePort();
    const child = spawnGroup(command, [...args, ...(portArgs ? portArgs(port) : [])], {
        cwd,
        env: {...baseEnv, ...env, ...(portEnv ? portEnv(port) : {})},
        stdio: ['ignore', 'pipe', 'pipe'],
    });
    const handle = {child, output: '', url: `http://127.0.0.1:${port}`};

    child.stdout.on('data', (chunk) => {
        handle.output += chunk;
    });
    child.stderr.on('data', (chunk) => {
        handle.output += chunk;
    });

    try {
        await waitFor(`${handle.url}/`, handle, timeoutMs);
    } catch (error) {
        await stop(handle);

        throw new Error(`${error.message}\n${handle.output}`);
    }

    return handle;
}

async function stop(handle) {
    if (!handle) {
        return;
    }

    const {child} = handle;

    if (child.exitCode === null && child.signalCode === null) {
        const exited = new Promise((resolveExit) => child.once('exit', resolveExit));
        const escalation = setTimeout(() => signalGroup(child.pid, 'SIGKILL'), 5_000);

        signalGroup(child.pid, 'SIGTERM');
        await exited;
        clearTimeout(escalation);
    }

    signalGroup(child.pid, 'SIGKILL');
}

function filesUnder(dir) {
    return readdirSync(dir).flatMap((name) => {
        const path = join(dir, name);

        return statSync(path).isDirectory() ? filesUnder(path) : [path];
    });
}

async function requestsSeen(mockUrl) {
    return (await fetch(`${mockUrl}/__requests`, {signal: AbortSignal.timeout(FETCH_TIMEOUT_MS)})).json();
}

async function issuedTokens(mockUrl) {
    return (await fetch(`${mockUrl}/__issued`, {signal: AbortSignal.timeout(FETCH_TIMEOUT_MS)})).json();
}

function fetchManually(url, init = {}) {
    return fetch(url, {...init, redirect: 'manual', signal: AbortSignal.timeout(FETCH_TIMEOUT_MS)});
}

function cookieNamed(response, name) {
    const cookie = response.headers.getSetCookie().find((candidate) => candidate.startsWith(`${name}=`));

    return cookie === undefined ? undefined : cookie.split(';')[0];
}

async function startSignIn(appUrl, returnTo) {
    const login = await fetchManually(`${appUrl}/auth/login?returnTo=${encodeURIComponent(returnTo)}`);
    const pendingCookie = cookieNamed(login, 'bytechef_login');
    const authorizeUrl = new URL(login.headers.get('location') ?? '', appUrl);

    assert(
        login.status === 302 && pendingCookie,
        `sign-in starts with a redirect and a pending cookie: ${login.status}`
    );
    assert(
        authorizeUrl.pathname === '/oauth2/authorize' &&
            authorizeUrl.searchParams.get('code_challenge_method') === 'S256' &&
            authorizeUrl.searchParams.get('redirect_uri') === `${appUrl}/auth/callback`,
        `sign-in redirects to the mock's authorize endpoint with PKCE S256: ${authorizeUrl}`
    );

    const authorized = await fetchManually(authorizeUrl);
    const callbackUrl = new URL(authorized.headers.get('location') ?? '', appUrl);

    assert(
        authorized.status === 302 && callbackUrl.pathname === '/auth/callback',
        `the mock authorizes and redirects to the callback: ${authorized.status} ${await authorized.text()}`
    );

    return {callbackUrl, pendingCookie};
}

async function startProductionApp(mockUrl, extraEnv = {}) {
    return startApp('node', ['.output/server/index.mjs'], {
        cwd: appDir,
        env: {BYTECHEF_APP_KEY: CANARY, BYTECHEF_URL: mockUrl, ...extraEnv},
        portEnv: (port) => ({HOST: '127.0.0.1', PORT: String(port)}),
        timeoutMs: 60_000,
    });
}

function pack(dir, destination) {
    const [packed] = JSON.parse(
        run('npm', ['pack', '--json', '--pack-destination', destination], {cwd: dir, label: `pack ${dir}`}).stdout
    );

    return {files: packed.files.map((file) => file.path), tarball: join(destination, packed.filename)};
}

function writeRecipes(dir) {
    const blocks = [...readFileSync(join(dir, 'AGENTS.md'), 'utf8').matchAll(/```tsx?\n([\s\S]*?)```/g)].map(
        (match) => match[1]
    );
    const recipeDir = join(dir, 'src/agents-recipes');

    function recipeExporting(name) {
        const index = blocks.findIndex((code) =>
            new RegExp(`export (const|function|interface|type) ${name}\\b`).test(code)
        );

        if (index < 0) {
            throw new Error(`No AGENTS.md recipe exports ${name}`);
        }

        return `./recipe${index + 1}`;
    }

    mkdirSync(recipeDir, {recursive: true});
    writeFileSync(join(recipeDir, 'stubs.d.ts'), AI_PROVIDER_STUB);

    blocks.forEach((code, index) => {
        const rewritten = code
            .replaceAll("from './bytechef'", "from '@/server/bytechef'")
            .replace(/import \{([^}]*)\} from '@\/server\/([\w-]+)';/g, (statement, names, module) => {
                if (existsSync(join(dir, 'src/server', `${module}.ts`))) {
                    return statement;
                }

                const firstName = names
                    .split(',')[0]
                    .trim()
                    .replace(/^type\s+/, '');

                return `import {${names}} from '${recipeExporting(firstName)}';`;
            });

        writeFileSync(join(recipeDir, `recipe${index + 1}.tsx`), rewritten);
    });

    return {count: blocks.length, needsAiSdk: blocks.some((code) => /from '(ai|zod)'/.test(code)), recipeDir};
}

for (const signal of ['SIGINT', 'SIGTERM']) {
    process.once(signal, () => process.exit(130));
}

process.once('exit', () => {
    for (const child of children) {
        signalGroup(child.pid, 'SIGKILL');
    }
});

const work = mkdtempSync(join(tmpdir(), 'bytechef-template-'));
const appDir = join(work, 'app');
let mock;
let app;

try {
    step('template formatting');
    run(process.execPath, [PRETTIER, '--check', 'create/template'], {
        cwd: W,
        label: 'prettier --check create/template',
    });

    step('pack');
    run('npm', ['run', 'build'], {cwd: join(W, 'library'), label: 'build library'});

    const library = pack(join(W, 'library'), work);
    const create = pack(join(W, 'create'), work);

    assert(
        create.files.includes('bin/create-bytechef-app.mjs') && create.files.includes('template/_gitignore'),
        'the scaffolder tarball carries its bin and template'
    );
    assert(
        !create.files.some((file) => PACK_EXCLUDED.test(file)),
        `the scaffolder tarball carries no build output or secrets: ${create.files.filter((file) => PACK_EXCLUDED.test(file))}`
    );

    step('scaffold');
    const scaffolded = run(
        'npm',
        [
            'exec',
            `--package=${create.tarball}`,
            '--',
            'create-bytechef-app',
            'app',
            '--url',
            'http://127.0.0.1:9',
            '--no-install',
        ],
        {cwd: work, label: 'scaffold'}
    );
    assert(existsSync(join(appDir, '.gitignore')) && existsSync(join(appDir, '.dockerignore')), 'dotfiles renamed');
    const scaffoldedEnv = readFileSync(join(appDir, '.env.local'), 'utf8');
    const scaffoldedSecret = SESSION_SECRET_LINE.exec(scaffoldedEnv)?.[1];

    assert(
        scaffoldedEnv.startsWith('BYTECHEF_URL=http://127.0.0.1:9\nBYTECHEF_APP_KEY=\nBYTECHEF_SESSION_SECRET=') &&
            scaffoldedSecret !== undefined,
        `.env.local carries --url, an empty key and a generated session secret: ${scaffoldedEnv.replace(/=.+$/gm, '=…')}`
    );
    assert(
        !`${scaffolded.stdout}${scaffolded.stderr}`.includes(scaffoldedSecret),
        'the scaffolder never prints the session secret'
    );
    assert(
        readFileSync(join(appDir, '.gitignore'), 'utf8').split('\n').includes('.env*') &&
            readFileSync(join(appDir, '.dockerignore'), 'utf8').split('\n').includes('.env*'),
        '.env.local is git-ignored and docker-ignored'
    );
    assert(
        !filesUnder(appDir).some(
            (file) => !file.endsWith('/.env.local') && readFileSync(file, 'utf8').includes(scaffoldedSecret)
        ),
        'the session secret is in .env.local only'
    );

    step('template lockfile matches the pins');
    const packageJson = JSON.parse(readFileSync(join(appDir, 'package.json'), 'utf8'));

    assert(
        create.files.includes('template/package-lock.json') && existsSync(join(appDir, 'package-lock.json')),
        'the scaffolder tarball carries the template lockfile and npm scaffolds keep it'
    );

    const templateLockfile = JSON.parse(readFileSync(join(appDir, 'package-lock.json'), 'utf8'));
    const pinProblems = lockfilePinProblems(packageJson, templateLockfile);

    assert(
        pinProblems.length === 0,
        `the template lockfile matches package.json; run npm run template:lock\n${pinProblems.join('\n')}`
    );

    step('install');

    assert(packageJson.name === 'app', 'package name taken from the directory');
    assert(packageJson.dependencies['@bytechef/app'] === '0.1.0', '@bytechef/app pinned to the scaffolder version');

    packageJson.dependencies['@bytechef/app'] = `file:${library.tarball}`;

    writeFileSync(join(appDir, 'package.json'), `${JSON.stringify(packageJson, null, 4)}\n`);
    run('npm', ['install', '--no-audit', '--no-fund'], {cwd: appDir, label: 'install'});

    step('install keeps the locked versions');
    const drift = installDrift(templateLockfile, JSON.parse(readFileSync(join(appDir, 'package-lock.json'), 'utf8')));

    assert(drift.changed.length === 0, `install changed locked versions:\n${drift.changed.join('\n')}`);
    assert(
        drift.unexpectedAdded.length === 0,
        `install added entries besides @bytechef/app and optional peers:\n${drift.unexpectedAdded.join('\n')}`
    );
    assert(
        drift.unexpectedRemoved.length === 0,
        `install removed top-level entries:\n${drift.unexpectedRemoved.join('\n')}`
    );
    assert(
        drift.added.includes('node_modules/@bytechef/app'),
        `install added node_modules/@bytechef/app: ${drift.added}`
    );

    if (drift.optionalPeersAdded.length > 0) {
        console.log(`install added optional peer entries: ${drift.optionalPeersAdded.join(', ')}`);
    }

    if (drift.removed.length > 0) {
        console.log(`install removed ${drift.removed.length} lockfile entries: ${drift.removed.join(', ')}`);
    }

    step('npm ci accepts the installed lockfile');
    run('npm', ['ci', '--dry-run', '--no-audit', '--no-fund'], {cwd: appDir, label: 'npm ci --dry-run'});

    step('check');
    writeFileSync(
        join(appDir, '.env.local'),
        `BYTECHEF_URL=https://example.invalid\nBYTECHEF_APP_KEY=${CANARY}\nBYTECHEF_SESSION_SECRET=${SESSION_CANARY}\n`
    );
    run('npm', ['run', 'check'], {
        cwd: appDir,
        env: {BYTECHEF_APP_KEY: CANARY, BYTECHEF_URL: 'https://example.invalid'},
        label: 'check',
    });

    step('the session secret or a session cookie name in the client fails the bundle check');
    const leakRoute = join(appDir, 'src/routes/leak.tsx');

    for (const [leaked, expected] of [
        [SESSION_CANARY, 'contains the session secret value'],
        ['BYTECHEF_SESSION_SECRET', 'contains BYTECHEF_SESSION_SECRET'],
        ['bytechef_session', 'contains bytechef_session'],
        ['bytechef_login', 'contains bytechef_login'],
    ]) {
        writeFileSync(leakRoute, LEAK_ROUTE(leaked));
        run('npm', ['run', 'generate-routes'], {cwd: appDir, label: 'generate routes with the leaking route'});
        run('npx', ['vite', 'build'], {cwd: appDir, label: `build leaking ${expected}`});

        const leak = run('node', ['scripts/check-bundle.mjs'], {
            cwd: appDir,
            expectStatus: 1,
            label: `bundle check with a client file that ${expected}`,
        });

        assert(
            leak.stderr.includes(expected),
            `the bundle check reports that a client file ${expected}: ${leak.stderr}`
        );
        assert(!leak.stderr.includes(SESSION_CANARY), 'the bundle check does not print the secret');
    }

    rmSync(leakRoute);
    run('npm', ['run', 'generate-routes'], {cwd: appDir, label: 'generate routes without the leaking route'});

    step('AGENTS.md recipes typecheck');
    const recipes = writeRecipes(appDir);

    assert(recipes.count >= 6, `AGENTS.md has its recipes: ${recipes.count}`);

    if (recipes.needsAiSdk) {
        const {devDependencies} = JSON.parse(readFileSync(join(W, 'library/package.json'), 'utf8'));

        run(
            'npm',
            [
                'install',
                '--no-save',
                '--no-audit',
                '--no-fund',
                `ai@${devDependencies.ai}`,
                `zod@${devDependencies.zod}`,
            ],
            {
                cwd: appDir,
                label: 'install ai and zod for the recipes',
            }
        );
    }

    run('npx', ['tsc', '--noEmit', '-p', 'tsconfig.json'], {cwd: appDir, label: 'typecheck the AGENTS.md recipes'});
    rmSync(recipes.recipeDir, {force: true, recursive: true});

    step('ready-made server functions fail the check');
    const readyMadeRoute = join(appDir, 'src/routes/ready-made.tsx');

    writeFileSync(readyMadeRoute, READY_MADE_ROUTE);
    run('npm', ['run', 'generate-routes'], {cwd: appDir, label: 'generate routes with the ready-made route'});
    const readyMade = run('npm', ['run', 'check'], {
        cwd: appDir,
        env: {BYTECHEF_APP_KEY: CANARY, BYTECHEF_URL: 'https://example.invalid'},
        expectStatus: 1,
        label: 'check with ready-made server functions',
    });

    assert(
        readyMade.stderr.includes(READY_MADE_MESSAGE) && readyMade.stderr.includes('allowReadyMadeServerFns'),
        `the check names the ready-made server functions and the opt-in: ${readyMade.stderr}`
    );
    rmSync(readyMadeRoute);
    run('npm', ['run', 'generate-routes'], {cwd: appDir, label: 'generate routes without the ready-made route'});

    step('dev-mode build fails the bundle check');
    run('npx', ['vite', 'build'], {cwd: appDir, env: {NODE_ENV: 'development'}, label: 'dev-mode build'});
    const devBundle = run('node', ['scripts/check-bundle.mjs'], {
        cwd: appDir,
        expectStatus: 1,
        label: 'bundle check on a dev-mode build',
    });

    assert(
        /development-only server function|from a dev- chunk/.test(devBundle.stderr),
        `the bundle check names the development-only code: ${devBundle.stderr}`
    );

    step('production rebuild passes the bundle check');
    run('npx', ['vite', 'build'], {cwd: appDir, label: 'rebuild'});
    run('node', ['scripts/check-bundle.mjs'], {cwd: appDir, label: 'bundle check on the rebuild'});
    assert(
        !filesUnder(join(appDir, '.output/server')).some((file) => {
            const text = readFileSync(file, 'utf8');

            return DEV_ONLY_HANDLERS.some((name) => text.includes(name));
        }),
        'no dev-only server function in the production build'
    );

    step('status');
    mock = await startMock();
    const status = JSON.parse(
        run('npm', ['run', '-s', 'bytechef:status'], {cwd: appDir, env: {BYTECHEF_URL: mock.url}, label: 'status'})
            .stdout
    );
    const [crm, mail] = status.bindings;

    assert(status.bytechefUrl === mock.url, 'the shell BYTECHEF_URL wins over .env.local');
    assert(status.actionSchemas === true, 'actionSchemas true');
    assert(
        crm.actions[0].name === 'createContact' && crm.actions[0].inputSchema.required[0] === 'email',
        'action schema attached'
    );
    assert(
        mail.componentVersion === null && mail.actions[0].inputSchema === null && mail.actions[0].name === 'send',
        'unavailable component still lists its actions'
    );
    assert(
        crm.tools[0].name === 'CRM__SEARCH_CONTACTS' && crm.tools[0].parameters.properties.q.type === 'string',
        'tools nested with parsed parameters'
    );
    assert(
        mail.tools.length === 1 && mail.tools[0].name === 'MAIL__SEND_MESSAGE' && mail.tools[0].parameters === null,
        'an allowed tool missing from the tool listing is still shown, with null parameters'
    );
    assert(status.workflows[0].workflowKey === 'onboard-customer', 'workflows listed');
    assert(status.userAuthMode === 'NONE', `the sign-in mode from /context: ${status.userAuthMode}`);
    assert(
        status.apiCollections.length === 2 &&
            status.apiCollections[0].path === 'v1/orders' &&
            status.apiCollections[0].available === true &&
            status.apiCollections[0].endpoints[0] === 'GET /orders' &&
            status.apiCollections[1].available === false,
        'API collections listed'
    );
    assert(
        (await requestsSeen(mock.url)).every((request) => request.authorization === `Bearer ${CANARY}`),
        'the key from .env.local is sent as the bearer'
    );

    step('status without a key');
    const missing = run('node', ['scripts/bytechef-status.mjs'], {
        cwd: appDir,
        env: {BYTECHEF_APP_KEY: '', BYTECHEF_URL: mock.url},
        expectStatus: 1,
        label: 'status without key',
    });

    assert(
        missing.stdout === '' &&
            missing.stderr.trim().split('\n').length === 1 &&
            /BYTECHEF_APP_KEY/.test(missing.stderr),
        `empty stdout and one stderr line naming the variable: ${missing.stderr}`
    );

    step('status reads .env.local when run without npm');
    const direct = run('node', ['scripts/bytechef-status.mjs'], {
        cwd: appDir,
        env: {BYTECHEF_URL: mock.url},
        label: 'status run directly, key from .env.local',
    });

    assert(direct.stderr === '', `no stderr when .env.local is present: ${direct.stderr}`);

    step('status without .env.local');
    renameSync(join(appDir, '.env.local'), join(appDir, '.env.local.moved'));

    try {
        const withoutFile = run('node', ['scripts/bytechef-status.mjs'], {
            cwd: appDir,
            env: {BYTECHEF_APP_KEY: CANARY, BYTECHEF_URL: mock.url},
            label: 'status without .env.local',
        });

        assert(JSON.parse(withoutFile.stdout).bytechefUrl === mock.url, 'the environment alone configures the status');
        assert(
            withoutFile.stderr === NO_ENV_FILE_MESSAGE,
            `one stderr line saying .env.local is absent, and no Node notice: ${withoutFile.stderr}`
        );
    } finally {
        renameSync(join(appDir, '.env.local.moved'), join(appDir, '.env.local'));
    }

    step('status against a server without /actions');
    await stop(mock);
    mock = await startMock(['--no-actions']);
    const old = JSON.parse(
        run('node', ['scripts/bytechef-status.mjs'], {
            cwd: appDir,
            env: {BYTECHEF_APP_KEY: CANARY, BYTECHEF_URL: mock.url},
            label: 'status old server',
        }).stdout
    );

    assert(
        old.actionSchemas === false &&
            old.bindings[0].actions[0].inputSchema === null &&
            old.bindings[0].actions[0].name === 'createContact',
        'names without schemas'
    );
    await stop(mock);

    step('status when /actions fails');
    mock = await startMock(['--actions-500']);
    const failing = run('node', ['scripts/bytechef-status.mjs'], {
        cwd: appDir,
        env: {BYTECHEF_APP_KEY: CANARY, BYTECHEF_URL: mock.url},
        label: 'status with /actions failing',
    });
    const degraded = JSON.parse(failing.stdout);

    assert(
        degraded.actionSchemas === false &&
            degraded.bindings[0].actions[0].inputSchema === null &&
            degraded.bindings[0].actions[0].name === 'createContact',
        'a failing /actions keeps the names without schemas'
    );
    assert(
        failing.stderr === 'Action schemas unavailable: MOCK_INTERNAL_ERROR\n',
        `one stderr warning line: ${failing.stderr}`
    );
    await stop(mock);
    mock = await startMock();

    step('production server renders the placeholder with sign-in off and no session secret');
    app = await startProductionApp(mock.url);
    const html = await (await fetch(app.url, {signal: AbortSignal.timeout(FETCH_TIMEOUT_MS)})).text();

    assert(html.includes('id="placeholder"') && !html.includes('id="overview"'), 'placeholder page');
    assert(!html.includes('id="sign-in"') && !html.includes('id="signed-in-user"'), 'no sign-in control with NONE');
    assert(!/createContact|hubspot|binding-crm/.test(html), 'no fixture data in production');
    assert(!html.includes(CANARY), 'no app key in the production page');
    assert(
        (await requestsSeen(mock.url)).every(
            (request) => request.url === '/api/app/v1/context' && request.userToken === null
        ),
        'the production page only read the sign-in mode'
    );
    await stop(app);
    app = undefined;
    await stop(mock);

    step('OPTIONAL sign-in renders pages without a user');
    mock = await startMock(['--user-auth-mode=OPTIONAL']);
    app = await startProductionApp(mock.url, {BYTECHEF_SESSION_SECRET: SESSION_CANARY});
    const optional = await fetchManually(`${app.url}/`);
    const optionalHtml = await optional.text();

    assert(optional.status === 200, `OPTIONAL renders the page: ${optional.status}`);
    assert(
        optionalHtml.includes('id="placeholder"') &&
            optionalHtml.includes('id="sign-in"') &&
            optionalHtml.includes('href="/auth/login?returnTo=%2F"'),
        'OPTIONAL shows the page and a sign-in link'
    );
    assert(
        !(await requestsSeen(mock.url)).some((request) => request.url === '/api/app/v1/me'),
        'no /me without a session'
    );
    await stop(app);
    app = undefined;
    await stop(mock);

    step('a sign-in that is not set up is explained');
    mock = await startMock(['--user-auth-mode=OPTIONAL', '--no-client']);
    app = await startProductionApp(mock.url, {BYTECHEF_SESSION_SECRET: SESSION_CANARY});
    const notSetUp = await fetchManually(`${app.url}/auth/login?returnTo=%2F`);

    assert(
        notSetUp.status === 503 && (await notSetUp.text()).includes("set this app instance's URL"),
        `a sign-in without a registered client explains what to set: ${notSetUp.status}`
    );
    await stop(app);
    app = undefined;
    await stop(mock);

    step('REQUIRED sign-in round trip');
    mock = await startMock(['--user-auth-mode=REQUIRED']);
    app = await startProductionApp(mock.url, {BYTECHEF_SESSION_SECRET: SESSION_CANARY});

    const anonymous = await fetchManually(`${app.url}/`);

    assert(
        anonymous.status === 302 && anonymous.headers.get('location') === '/auth/login?returnTo=%2F',
        `an unauthenticated page request is redirected to sign-in: ${anonymous.status} ${anonymous.headers.get('location')}`
    );

    const forged = await startSignIn(app.url, '/');

    forged.callbackUrl.searchParams.set('state', `${forged.callbackUrl.searchParams.get('state')}x`);

    const tokenRequestsBefore = (await requestsSeen(mock.url)).filter((request) => request.url === '/oauth2/token');
    const forgedCallback = await fetchManually(forged.callbackUrl, {headers: {cookie: forged.pendingCookie}});

    assert(
        forgedCallback.status === 400 && cookieNamed(forgedCallback, 'bytechef_session') === undefined,
        `a callback with another state is refused: ${forgedCallback.status}`
    );
    assert(
        (await requestsSeen(mock.url)).filter((request) => request.url === '/oauth2/token').length ===
            tokenRequestsBefore.length,
        'a callback with another state makes no token request'
    );

    const signIn = await startSignIn(app.url, '/');
    const callback = await fetchManually(signIn.callbackUrl, {headers: {cookie: signIn.pendingCookie}});
    const sessionCookie = cookieNamed(callback, 'bytechef_session');

    assert(
        callback.status === 302 && callback.headers.get('location') === '/' && sessionCookie,
        `the callback sets the session cookie and returns to the page: ${callback.status} ${await callback.text()}`
    );
    assert(
        callback.headers
            .getSetCookie()
            .some((cookie) => /^bytechef_session=[^;]+; Path=\/; Max-Age=\d+; HttpOnly; SameSite=Lax$/.test(cookie)),
        `the session cookie is HttpOnly and SameSite=Lax: ${callback.headers.getSetCookie()}`
    );

    const signedIn = await fetchManually(`${app.url}/`, {headers: {cookie: sessionCookie}});
    const signedInHtml = await signedIn.text();
    const tokens = await issuedTokens(mock.url);

    assert(
        signedIn.status === 200 && signedInHtml.includes('<span id="signed-in-user">Ada Lovelace</span>'),
        `the next request renders as the user: ${signedIn.status}`
    );
    assert(signedInHtml.includes('id="sign-out"') && signedInHtml.includes('action="/auth/logout"'), 'a sign-out form');
    assert(tokens.length === 2, `one token response: ${tokens.length}`);
    assert(
        ![...tokens, sessionCookie.slice('bytechef_session='.length), SESSION_CANARY, CANARY, 'ada@example.com'].some(
            (value) => signedInHtml.includes(value)
        ),
        'the page carries no token, session cookie value, secret, app key or user login'
    );
    assert(
        (await requestsSeen(mock.url)).some(
            (request) => request.url === '/api/app/v1/me' && tokens.includes(request.userToken)
        ),
        '/me was called with the user token'
    );

    const foreignLogout = await fetchManually(`${app.url}/auth/logout`, {
        headers: {cookie: sessionCookie, origin: 'https://evil.example'},
        method: 'POST',
    });

    assert(foreignLogout.status === 403, `a cross-site sign-out is refused: ${foreignLogout.status}`);

    const logout = await fetchManually(`${app.url}/auth/logout`, {
        headers: {cookie: sessionCookie, origin: app.url},
        method: 'POST',
    });

    assert(
        logout.status === 303 && cookieNamed(logout, 'bytechef_session') === 'bytechef_session=',
        `sign-out clears the session cookie: ${logout.status}`
    );
    await stop(app);
    app = undefined;
    await stop(mock);
    mock = await startMock();

    step('development server renders the overview');
    app = await startApp('npx', ['vite', 'dev', '--host', '127.0.0.1', '--strictPort'], {
        cwd: appDir,
        env: {BYTECHEF_APP_KEY: CANARY, BYTECHEF_URL: mock.url},
        portArgs: (port) => ['--port', String(port)],
        timeoutMs: 180_000,
    });
    const devHtml = await (await fetch(app.url, {signal: AbortSignal.timeout(FETCH_TIMEOUT_MS)})).text();

    assert(
        devHtml.includes('id="overview"') && devHtml.includes('id="binding-crm"') && devHtml.includes('createContact'),
        'overview rendered'
    );
    assert(
        devHtml.includes('id="binding-mail"') && devHtml.includes('component unavailable'),
        'unavailable component flagged'
    );
    assert(devHtml.includes('MAIL__SEND_MESSAGE'), 'an allowed tool missing from the tool listing is shown');
    assert(devHtml.includes('id="workflows"') && devHtml.includes('onboard-customer'), 'workflows rendered');
    assert(devHtml.includes('id="api-collections"') && devHtml.includes('v1/orders'), 'API collections rendered');
    assert(!devHtml.includes(CANARY), 'no app key in the development page');
    const seen = await requestsSeen(mock.url);

    assert(
        seen.every((request) => request.method === 'GET'),
        'no POST reached ByteChef'
    );
    assert(
        seen.every((request) => request.authorization === `Bearer ${CANARY}`),
        'every request carried the app key'
    );
    assert(
        [
            '/api/app/v1/connections',
            '/api/app/v1/actions',
            '/api/app/v1/tools',
            '/api/app/v1/workflows',
            '/api/app/v1/api-collections',
        ].every((path) => seen.some((request) => request.url === path)),
        'overview fetched all five listings'
    );
    await stop(app);
    app = undefined;

    step('check:template passed');
} catch (error) {
    console.error(error.message);

    if (app) {
        console.error(`--- app output ---\n${app.output}`);
    }

    process.exitCode = 1;
} finally {
    await stop(app);
    await stop(mock);
    try {
        rmSync(work, {force: true, maxRetries: 3, recursive: true});
    } catch (error) {
        console.error(`Could not remove ${work}: ${error.message}`);
    }
}
