import {spawn} from 'node:child_process';
import {randomUUID, timingSafeEqual} from 'node:crypto';
import {createServer} from 'node:http';
import {realpathSync} from 'node:fs';
import {constants} from 'node:os';
import {pathToFileURL} from 'node:url';

const MAX_BODY_BYTES = 16 * 1024;
const MAX_FIELD_LENGTH = 255;
const MAX_KEPT_DEPLOYMENTS = 100;
const MIN_TOKEN_LENGTH = 32;
const COMMIT_SHA = /^[0-9a-fA-F]{7,64}$/;
const DEFAULT_KILL_GRACE_MILLISECONDS = 10000;
const CONTROL_CHARACTERS = /[\p{Cc}\p{Zl}\p{Zp}]/u;

const REQUEST_FIELDS = {
    environment: 'BYTECHEF_ENVIRONMENT',
    gitCommitSha: 'BYTECHEF_GIT_COMMIT_SHA',
    gitRef: 'BYTECHEF_GIT_REF',
    version: 'BYTECHEF_VERSION',
};

function fail(message) {
    process.stderr.write(`${message}\n`);
    process.exit(1);
}

function readConfiguration() {
    const token = process.env.DEPLOY_AGENT_TOKEN || '';
    const command = (process.env.DEPLOY_COMMAND || '').trim();
    const host = process.env.DEPLOY_AGENT_HOST || '127.0.0.1';
    const portText = process.env.DEPLOY_AGENT_PORT || '8787';
    const publicUrl = process.env.DEPLOY_AGENT_PUBLIC_URL || null;
    const killGraceText = process.env.DEPLOY_AGENT_KILL_GRACE_MS || String(DEFAULT_KILL_GRACE_MILLISECONDS);

    if (token.length < MIN_TOKEN_LENGTH) {
        fail(`DEPLOY_AGENT_TOKEN must be set to a secret of at least ${MIN_TOKEN_LENGTH} characters.`);
    }

    if (!command) {
        fail('DEPLOY_COMMAND must be set to the command that deploys the app.');
    }

    if (!/^\d+$/.test(portText) || Number(portText) > 65535) {
        fail('DEPLOY_AGENT_PORT must be a port number.');
    }

    if (!/^\d+$/.test(killGraceText)) {
        fail('DEPLOY_AGENT_KILL_GRACE_MS must be a number of milliseconds.');
    }

    if (publicUrl !== null && !isHttpUrl(publicUrl)) {
        fail('DEPLOY_AGENT_PUBLIC_URL must be an absolute http or https URL.');
    }

    return {
        command,
        host,
        killGraceMilliseconds: Number(killGraceText),
        port: Number(portText),
        publicUrl,
        token: Buffer.from(token, 'utf8'),
    };
}

function isHttpUrl(value) {
    try {
        const url = new URL(value);

        return (url.protocol === 'http:' || url.protocol === 'https:') && !url.username && !url.password;
    } catch {
        return false;
    }
}

function isAuthorized(request, token) {
    const header = request.headers.authorization || '';
    const provided = Buffer.from(header.startsWith('Bearer ') ? header.slice('Bearer '.length) : '', 'utf8');

    if (provided.length !== token.length) {
        timingSafeEqual(token, token);

        return false;
    }

    return timingSafeEqual(provided, token);
}

function send(response, statusCode, body) {
    if (body === undefined) {
        response.writeHead(statusCode, {'Content-Length': '0'});
        response.end();

        return;
    }

    const json = JSON.stringify(body);

    response.writeHead(statusCode, {
        'Cache-Control': 'no-store',
        'Content-Length': Buffer.byteLength(json),
        'Content-Type': 'application/json',
    });
    response.end(json);
}

function readBody(request) {
    return new Promise((resolve, reject) => {
        const chunks = [];
        let size = 0;

        request.on('data', (chunk) => {
            size += chunk.length;

            if (size > MAX_BODY_BYTES) {
                request.removeAllListeners('data');
                request.resume();
                reject(new RangeError('body too large'));

                return;
            }

            chunks.push(chunk);
        });
        request.on('end', () => resolve(Buffer.concat(chunks).toString('utf8')));
        request.on('error', reject);
    });
}

function inheritedEnvironment() {
    const environment = {...process.env};

    delete environment.DEPLOY_AGENT_TOKEN;

    return environment;
}

function isValidField(field, value) {
    if (typeof value !== 'string' || value.length > MAX_FIELD_LENGTH || CONTROL_CHARACTERS.test(value)) {
        return false;
    }

    if (field === 'gitCommitSha') {
        return COMMIT_SHA.test(value);
    }

    return !value.startsWith('-');
}

function toChildEnvironment(payload) {
    const childEnvironment = {};

    for (const [field, variableName] of Object.entries(REQUEST_FIELDS)) {
        const value = payload[field];

        if (value === undefined || value === null) {
            continue;
        }

        if (!isValidField(field, value)) {
            return null;
        }

        childEnvironment[variableName] = value;
    }

    return childEnvironment;
}

function toView(deployment) {
    const view = {id: deployment.id, status: deployment.status};

    if (deployment.url) {
        view.url = deployment.url;
    }

    if (deployment.errorMessage) {
        view.errorMessage = deployment.errorMessage;
    }

    return view;
}

function exitCodeOf(code, signal) {
    if (typeof code === 'number') {
        return code;
    }

    const signalNumber = signal ? constants.signals[signal] : undefined;

    return typeof signalNumber === 'number' ? 128 + signalNumber : 1;
}

const escalationTimers = new WeakMap();

function signalProcess(child, signal) {
    if (hasExited(child)) {
        return;
    }

    try {
        if (process.platform === 'win32') {
            child.kill(signal);
        } else {
            process.kill(-child.pid, signal);
        }
    } catch {
        if (child.exitCode === null && child.signalCode === null) {
            child.kill(signal);
        }
    }
}

function hasExited(child) {
    return child.exitCode !== null || child.signalCode !== null;
}

export function stopProcess(child, killGraceMilliseconds, signalFunction = signalProcess) {
    if (hasExited(child)) {
        return;
    }

    signalFunction(child, 'SIGTERM');

    if (hasExited(child) || escalationTimers.has(child)) {
        return;
    }

    const timer = setTimeout(() => {
        escalationTimers.delete(child);

        if (!hasExited(child)) {
            signalFunction(child, 'SIGKILL');
        }
    }, killGraceMilliseconds);

    timer.unref();

    escalationTimers.set(child, timer);

    child.once('exit', () => {
        clearTimeout(timer);
        escalationTimers.delete(child);
    });
}

function createAgent(configuration) {
    const deployments = new Map();
    let activeDeployment = null;

    function remember(deployment) {
        deployments.set(deployment.id, deployment);

        while (deployments.size > MAX_KEPT_DEPLOYMENTS) {
            const [oldestId, oldest] = deployments.entries().next().value;

            if (oldest === activeDeployment) {
                break;
            }

            deployments.delete(oldestId);
        }
    }

    function finish(deployment, code, signal) {
        if (activeDeployment === deployment) {
            activeDeployment = null;
        }

        deployment.child = null;

        if (deployment.status !== 'BUILDING') {
            return;
        }

        const exitCode = exitCodeOf(code, signal);

        if (exitCode === 0) {
            deployment.status = 'READY';
            deployment.url = configuration.publicUrl;
        } else {
            deployment.status = 'FAILED';
            deployment.errorMessage = `Exit code ${exitCode}`;
        }
    }

    function startDeployment(childEnvironment) {
        const deployment = {child: null, errorMessage: null, id: randomUUID(), status: 'BUILDING', url: null};

        const child = spawn(configuration.command, {
            detached: process.platform !== 'win32',
            env: {...inheritedEnvironment(), ...childEnvironment},
            shell: true,
            stdio: 'ignore',
        });

        deployment.child = child;
        activeDeployment = deployment;

        child.once('error', () => finish(deployment, 127, null));
        child.once('exit', (code, signal) => finish(deployment, code, signal));

        remember(deployment);

        return deployment;
    }

    async function handleDeploy(request, response) {
        let text;

        try {
            text = await readBody(request);
        } catch {
            response.shouldKeepAlive = false;
            send(response, 413);

            return;
        }

        let payload;

        try {
            payload = JSON.parse(text || '{}');
        } catch {
            send(response, 400);

            return;
        }

        if (payload === null || typeof payload !== 'object' || Array.isArray(payload)) {
            send(response, 400);

            return;
        }

        const childEnvironment = toChildEnvironment(payload);

        if (childEnvironment === null) {
            send(response, 400);

            return;
        }

        if (activeDeployment !== null) {
            send(response, 409);

            return;
        }

        send(response, 202, toView(startDeployment(childEnvironment)));
    }

    function handleCancel(response, deployment) {
        if (deployment.status !== 'BUILDING') {
            send(response, 200, toView(deployment));

            return;
        }

        deployment.status = 'CANCELED';

        if (deployment.child) {
            stopProcess(deployment.child, configuration.killGraceMilliseconds);
        }

        send(response, 200, toView(deployment));
    }

    function toDeploymentRoute(pathname) {
        const match = /^\/([^/]+)(\/cancel)?$/.exec(pathname);

        if (!match) {
            return null;
        }

        try {
            return {cancel: Boolean(match[2]), id: decodeURIComponent(match[1])};
        } catch {
            return null;
        }
    }

    async function handle(request, response) {
        if (!isAuthorized(request, configuration.token)) {
            request.resume();
            send(response, 401);

            return;
        }

        const pathname = (request.url || '/').split('?')[0];

        if (pathname === '/') {
            if (request.method !== 'POST') {
                send(response, 405);

                return;
            }

            await handleDeploy(request, response);

            return;
        }

        request.resume();

        const route = toDeploymentRoute(pathname);
        const deployment = route ? deployments.get(route.id) : undefined;

        if (!deployment) {
            send(response, 404);

            return;
        }

        if (route.cancel) {
            if (request.method !== 'POST') {
                send(response, 405);

                return;
            }

            handleCancel(response, deployment);

            return;
        }

        if (request.method !== 'GET') {
            send(response, 405);

            return;
        }

        send(response, 200, toView(deployment));
    }

    function activeChild() {
        return activeDeployment ? activeDeployment.child : null;
    }

    return {activeChild, handle};
}

function shutDown(server, agent, configuration) {
    server.close();

    const child = agent.activeChild();

    if (!child || hasExited(child)) {
        process.exit(0);
    }

    child.once('exit', () => process.exit(0));

    stopProcess(child, configuration.killGraceMilliseconds);

    setTimeout(() => process.exit(0), configuration.killGraceMilliseconds);
}

const LOOPBACK_HOSTS = new Set(['127.0.0.1', '::1', 'localhost']);

function isLoopbackHost(host) {
    return LOOPBACK_HOSTS.has(host.toLowerCase()) || /^127\.\d{1,3}\.\d{1,3}\.\d{1,3}$/.test(host);
}

function main() {
    const configuration = readConfiguration();

    if (!isLoopbackHost(configuration.host)) {
        process.stderr.write(
            'Warning: DEPLOY_AGENT_HOST is not a loopback address, so the agent serves plain HTTP beyond this machine ' +
                'and its token travels unencrypted. Put it behind a TLS reverse proxy or keep it on 127.0.0.1.\n'
        );
    }
    const agent = createAgent(configuration);

    const server = createServer((request, response) => {
        agent.handle(request, response).catch(() => {
            if (!response.headersSent) {
                send(response, 500);
            }
        });
    });

    server.requestTimeout = 30000;
    server.headersTimeout = 10000;

    for (const signal of ['SIGINT', 'SIGTERM']) {
        process.once(signal, () => shutDown(server, agent, configuration));
    }

    server.listen(configuration.port, configuration.host, () => {
        const address = server.address();
        const hostText = address.family === 'IPv6' ? `[${address.address}]` : address.address;

        process.stdout.write(`Deploy agent listening on http://${hostText}:${address.port}\n`);
    });
}

function isEntryPoint() {
    if (!process.argv[1]) {
        return false;
    }

    try {
        return import.meta.url === pathToFileURL(realpathSync(process.argv[1])).href;
    } catch {
        return false;
    }
}

if (isEntryPoint()) {
    main();
}
