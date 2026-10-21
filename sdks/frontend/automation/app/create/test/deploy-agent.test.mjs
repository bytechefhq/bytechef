import {spawn} from 'node:child_process';
import {existsSync, mkdtempSync, readFileSync, writeFileSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {dirname, join, resolve} from 'node:path';
import {fileURLToPath} from 'node:url';
import {afterEach, describe, expect, it} from 'vitest';

const agentScript = resolve(dirname(fileURLToPath(import.meta.url)), '..', 'template', 'scripts', 'deploy-agent.mjs');
const TOKEN = 'a-deploy-agent-test-token-of-48-characters-long!';
const NODE = JSON.stringify(process.execPath);

const startedAgents = [];

function startAgent(extraEnv) {
    const env = {...process.env};

    for (const name of Object.keys(env)) {
        if (name.startsWith('DEPLOY_') || name.startsWith('BYTECHEF_')) {
            delete env[name];
        }
    }

    const child = spawn(process.execPath, [agentScript], {
        env: {...env, DEPLOY_AGENT_HOST: '127.0.0.1', DEPLOY_AGENT_PORT: '0', ...extraEnv},
        stdio: ['ignore', 'pipe', 'pipe'],
    });

    startedAgents.push(child);

    let stdout = '';
    let stderr = '';

    child.stdout.on('data', (chunk) => {
        stdout += chunk;
    });
    child.stderr.on('data', (chunk) => {
        stderr += chunk;
    });

    const ready = new Promise((resolvePromise, rejectPromise) => {
        child.stdout.on('data', () => {
            const match = /listening on (http:\/\/127\.0\.0\.1:\d+)/.exec(stdout);

            if (match) {
                resolvePromise(match[1]);
            }
        });
        child.once('exit', (code) => rejectPromise(new Error(`agent exited with ${code}: ${stderr}`)));
    });

    ready.catch(() => {});

    return {child, ready, stderr: () => stderr};
}

function waitForExit(child) {
    if (child.exitCode !== null || child.signalCode !== null) {
        return Promise.resolve(child.exitCode);
    }

    return new Promise((resolvePromise) => child.once('exit', (code) => resolvePromise(code)));
}

async function call(baseUrl, path, {body, method = 'GET', token = TOKEN} = {}) {
    const headers = {};

    if (token !== null) {
        headers.Authorization = `Bearer ${token}`;
    }

    if (body !== undefined) {
        headers['Content-Type'] = 'application/json';
    }

    const response = await fetch(baseUrl + path, {
        body: body === undefined ? undefined : JSON.stringify(body),
        headers,
        method,
    });
    const text = await response.text();

    return {body: text ? JSON.parse(text) : null, status: response.status, text};
}

async function pollUntilFinished(baseUrl, id) {
    for (let attempt = 0; attempt < 200; attempt++) {
        const status = await call(baseUrl, `/${encodeURIComponent(id)}`);

        if (status.body.status !== 'BUILDING') {
            return status.body;
        }

        await new Promise((resolvePromise) => setTimeout(resolvePromise, 25));
    }

    throw new Error(`deployment ${id} did not finish`);
}

async function waitFor(condition) {
    for (let attempt = 0; attempt < 200; attempt++) {
        if (condition()) {
            return;
        }

        await new Promise((resolvePromise) => setTimeout(resolvePromise, 25));
    }

    throw new Error('condition not met');
}

function isRunning(pid) {
    try {
        process.kill(pid, 0);

        return true;
    } catch {
        return false;
    }
}

afterEach(async () => {
    while (startedAgents.length > 0) {
        const child = startedAgents.pop();

        if (child.exitCode === null && child.signalCode === null) {
            child.kill('SIGTERM');
        }

        await waitForExit(child);
    }
});

describe('deploy agent', () => {
    it('refuses to start without a long enough token or a command', async () => {
        const withoutToken = startAgent({DEPLOY_COMMAND: `${NODE} -e "process.exit(0)"`});

        expect(await waitForExit(withoutToken.child)).toBe(1);
        expect(withoutToken.stderr()).toContain('DEPLOY_AGENT_TOKEN');

        const shortToken = startAgent({DEPLOY_AGENT_TOKEN: 'short', DEPLOY_COMMAND: `${NODE} -e "process.exit(0)"`});

        expect(await waitForExit(shortToken.child)).toBe(1);
        expect(shortToken.stderr()).not.toContain('short');

        const withoutCommand = startAgent({DEPLOY_AGENT_TOKEN: TOKEN});

        expect(await waitForExit(withoutCommand.child)).toBe(1);
        expect(withoutCommand.stderr()).toContain('DEPLOY_COMMAND');
    });

    it('warns at startup that a non-loopback host serves plain HTTP beyond this machine', async () => {
        const agent = startAgent({
            DEPLOY_AGENT_HOST: '0.0.0.0',
            DEPLOY_AGENT_TOKEN: TOKEN,
            DEPLOY_COMMAND: `${NODE} -e "process.exit(0)"`,
        });

        await new Promise((resolvePromise) => {
            const check = () => {
                if (agent.stderr().includes('DEPLOY_AGENT_HOST')) {
                    agent.child.stderr.off('data', check);
                    resolvePromise();
                }
            };

            agent.child.stderr.on('data', check);
            check();
        });

        expect(agent.stderr()).toContain('plain HTTP');
        expect(agent.stderr()).not.toContain(TOKEN);
    });

    it('starts on the loopback host without a warning', async () => {
        const agent = startAgent({DEPLOY_AGENT_TOKEN: TOKEN, DEPLOY_COMMAND: `${NODE} -e "process.exit(0)"`});

        await agent.ready;

        expect(agent.stderr()).toBe('');
    });

    it('answers 401 with an empty body without the token', async () => {
        const agent = startAgent({DEPLOY_AGENT_TOKEN: TOKEN, DEPLOY_COMMAND: `${NODE} -e "process.exit(0)"`});
        const baseUrl = await agent.ready;

        for (const token of [null, 'wrong', TOKEN.slice(0, -1) + '?', TOKEN + 'x']) {
            const response = await call(baseUrl, '/', {body: {gitRef: 'main'}, method: 'POST', token});

            expect(response.status).toBe(401);
            expect(response.text).toBe('');
        }

        expect((await call(baseUrl, '/missing', {token: null})).status).toBe(401);
    });

    it('deploys and reports READY with the public url', async () => {
        const agent = startAgent({
            DEPLOY_AGENT_PUBLIC_URL: 'https://crm.example.com',
            DEPLOY_AGENT_TOKEN: TOKEN,
            DEPLOY_COMMAND: `${NODE} -e "process.exit(0)"`,
        });
        const baseUrl = await agent.ready;

        const started = await call(baseUrl, '/', {
            body: {environment: 'PRODUCTION', gitCommitSha: 'abc1234', gitRef: 'main', version: '1.2'},
            method: 'POST',
        });

        expect(started.status).toBe(202);
        expect(started.body.status).toBe('BUILDING');
        expect(started.body.id).toMatch(/^[0-9a-f-]{36}$/);

        expect(await pollUntilFinished(baseUrl, started.body.id)).toEqual({
            id: started.body.id,
            status: 'READY',
            url: 'https://crm.example.com',
        });

        expect((await call(baseUrl, '/00000000-0000-0000-0000-000000000000')).status).toBe(404);
    });

    it('reports FAILED with only the exit code', async () => {
        const agent = startAgent({
            DEPLOY_AGENT_TOKEN: TOKEN,
            DEPLOY_COMMAND: `${NODE} -e "console.log('secret build output'); process.exit(1)"`,
        });
        const baseUrl = await agent.ready;

        const started = await call(baseUrl, '/', {body: {gitRef: 'main'}, method: 'POST'});

        expect(await pollUntilFinished(baseUrl, started.body.id)).toEqual({
            errorMessage: 'Exit code 1',
            id: started.body.id,
            status: 'FAILED',
        });
    });

    it('refuses a second deployment while one is building and cancels the running one', async () => {
        const directory = mkdtempSync(join(tmpdir(), 'deploy-agent-'));
        const pidFile = join(directory, 'pid');
        const script = join(directory, 'long.mjs');

        writeFileSync(
            script,
            `import {writeFileSync} from 'node:fs';\nwriteFileSync(${JSON.stringify(pidFile)}, String(process.pid));\nsetTimeout(() => {}, 60000);\n`
        );

        const agent = startAgent({DEPLOY_AGENT_TOKEN: TOKEN, DEPLOY_COMMAND: `${NODE} ${JSON.stringify(script)}`});
        const baseUrl = await agent.ready;

        const started = await call(baseUrl, '/', {body: {gitRef: 'main'}, method: 'POST'});

        expect(started.status).toBe(202);
        expect((await call(baseUrl, '/', {body: {gitRef: 'main'}, method: 'POST'})).status).toBe(409);

        await waitFor(() => existsSync(pidFile) && readFileSync(pidFile, 'utf8').length > 0);

        const pid = Number(readFileSync(pidFile, 'utf8'));

        const canceled = await call(baseUrl, `/${started.body.id}/cancel`, {body: {}, method: 'POST'});

        expect(canceled.status).toBe(200);
        expect(canceled.body).toEqual({id: started.body.id, status: 'CANCELED'});
        expect((await call(baseUrl, `/${started.body.id}`)).body.status).toBe('CANCELED');

        await waitFor(() => !isRunning(pid));

        const canceledAgain = await call(baseUrl, `/${started.body.id}/cancel`, {body: {}, method: 'POST'});

        expect(canceledAgain.status).toBe(200);
        expect(canceledAgain.body).toEqual({id: started.body.id, status: 'CANCELED'});
    });

    it('answers a cancel of a finished deployment with its current state', async () => {
        const agent = startAgent({DEPLOY_AGENT_TOKEN: TOKEN, DEPLOY_COMMAND: `${NODE} -e "process.exit(0)"`});
        const baseUrl = await agent.ready;

        const started = await call(baseUrl, '/', {body: {gitRef: 'main'}, method: 'POST'});

        expect((await pollUntilFinished(baseUrl, started.body.id)).status).toBe('READY');

        const canceled = await call(baseUrl, `/${started.body.id}/cancel`, {body: {}, method: 'POST'});

        expect(canceled.status).toBe(200);
        expect(canceled.body).toEqual({id: started.body.id, status: 'READY'});
    });

    it('kills a command that ignores SIGTERM after the grace period', async () => {
        const directory = mkdtempSync(join(tmpdir(), 'deploy-agent-'));
        const pidFile = join(directory, 'pid');
        const script = join(directory, 'stubborn.mjs');

        writeFileSync(
            script,
            `import {writeFileSync} from 'node:fs';\nprocess.on('SIGTERM', () => {});\nwriteFileSync(${JSON.stringify(pidFile)}, String(process.pid));\nsetInterval(() => {}, 1000);\n`
        );

        const agent = startAgent({
            DEPLOY_AGENT_KILL_GRACE_MS: '200',
            DEPLOY_AGENT_TOKEN: TOKEN,
            DEPLOY_COMMAND: `${NODE} ${JSON.stringify(script)}`,
        });
        const baseUrl = await agent.ready;

        const started = await call(baseUrl, '/', {body: {gitRef: 'main'}, method: 'POST'});

        await waitFor(() => existsSync(pidFile) && readFileSync(pidFile, 'utf8').length > 0);

        const pid = Number(readFileSync(pidFile, 'utf8'));

        expect((await call(baseUrl, `/${started.body.id}/cancel`, {body: {}, method: 'POST'})).status).toBe(200);

        await waitFor(() => !isRunning(pid));

        let next;

        for (let attempt = 0; attempt < 200; attempt++) {
            next = await call(baseUrl, '/', {body: {gitRef: 'main'}, method: 'POST'});

            if (next.status !== 409) {
                break;
            }

            await new Promise((resolvePromise) => setTimeout(resolvePromise, 25));
        }

        expect(next.status).toBe(202);

        await waitFor(() => existsSync(pidFile) && Number(readFileSync(pidFile, 'utf8')) !== pid);

        const nextPid = Number(readFileSync(pidFile, 'utf8'));

        expect((await call(baseUrl, `/${next.body.id}/cancel`, {body: {}, method: 'POST'})).status).toBe(200);

        await waitFor(() => !isRunning(nextPid));
    });

    it('kills a command that ignores SIGTERM when the agent itself is stopped', async () => {
        const directory = mkdtempSync(join(tmpdir(), 'deploy-agent-'));
        const pidFile = join(directory, 'pid');
        const script = join(directory, 'stubborn.mjs');

        writeFileSync(
            script,
            `import {writeFileSync} from 'node:fs';\nprocess.on('SIGTERM', () => {});\nwriteFileSync(${JSON.stringify(pidFile)}, String(process.pid));\nsetInterval(() => {}, 1000);\n`
        );

        const agent = startAgent({
            DEPLOY_AGENT_KILL_GRACE_MS: '200',
            DEPLOY_AGENT_TOKEN: TOKEN,
            DEPLOY_COMMAND: `${NODE} ${JSON.stringify(script)}`,
        });
        const baseUrl = await agent.ready;

        expect((await call(baseUrl, '/', {body: {gitRef: 'main'}, method: 'POST'})).status).toBe(202);

        await waitFor(() => existsSync(pidFile) && readFileSync(pidFile, 'utf8').length > 0);

        const pid = Number(readFileSync(pidFile, 'utf8'));

        agent.child.kill('SIGTERM');

        expect(await waitForExit(agent.child)).toBe(0);

        try {
            await waitFor(() => !isRunning(pid));
        } finally {
            if (isRunning(pid)) {
                process.kill(pid, 'SIGKILL');
            }
        }
    });

    it('passes request values to the command only as environment variables', async () => {
        const directory = mkdtempSync(join(tmpdir(), 'deploy-agent-'));
        const recordFile = join(directory, 'record.json');
        const markerFile = join(directory, 'injected');
        const script = join(directory, 'record.mjs');

        writeFileSync(
            script,
            `import {writeFileSync} from 'node:fs';\nwriteFileSync(${JSON.stringify(recordFile)}, JSON.stringify({argv: process.argv.slice(2), environment: process.env.BYTECHEF_ENVIRONMENT, gitCommitSha: process.env.BYTECHEF_GIT_COMMIT_SHA, gitRef: process.env.BYTECHEF_GIT_REF, tokenPresent: 'DEPLOY_AGENT_TOKEN' in process.env, version: process.env.BYTECHEF_VERSION}));\n`
        );

        const agent = startAgent({DEPLOY_AGENT_TOKEN: TOKEN, DEPLOY_COMMAND: `${NODE} ${JSON.stringify(script)}`});
        const baseUrl = await agent.ready;
        const injectedRef = `main; touch ${markerFile} $(touch ${markerFile})`;

        const started = await call(baseUrl, '/', {
            body: {environment: 'STAGING', gitCommitSha: 'abc1234', gitRef: injectedRef, version: '`touch x`'},
            method: 'POST',
        });

        expect((await pollUntilFinished(baseUrl, started.body.id)).status).toBe('READY');
        expect(JSON.parse(readFileSync(recordFile, 'utf8'))).toEqual({
            argv: [],
            environment: 'STAGING',
            gitCommitSha: 'abc1234',
            gitRef: injectedRef,
            tokenPresent: false,
            version: '`touch x`',
        });
        expect(existsSync(markerFile)).toBe(false);
    });

    it('rejects invalid request values and oversized bodies', async () => {
        const agent = startAgent({DEPLOY_AGENT_TOKEN: TOKEN, DEPLOY_COMMAND: `${NODE} -e "process.exit(0)"`});
        const baseUrl = await agent.ready;

        for (const body of [
            {gitRef: 'main\nrm -rf /'},
            {gitRef: 'x'.repeat(256)},
            {gitRef: 42},
            [{gitRef: 'main'}],
            {gitRef: '--upload-pack=touch x'},
            {gitRef: 'main', version: '-1'},
            {gitCommitSha: 'abc12'},
            {gitCommitSha: 'a'.repeat(65)},
            {gitCommitSha: 'abc123g'},
            {gitCommitSha: '-abc1234'},
        ]) {
            expect((await call(baseUrl, '/', {body, method: 'POST'})).status).toBe(400);
        }

        expect((await call(baseUrl, '/', {body: {gitRef: 'x'.repeat(17 * 1024)}, method: 'POST'})).status).toBe(413);
    });
});
