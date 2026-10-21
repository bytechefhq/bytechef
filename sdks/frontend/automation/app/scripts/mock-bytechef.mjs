import {createHash, randomBytes} from 'node:crypto';
import {readFileSync} from 'node:fs';
import {createServer} from 'node:http';

const [fixturePath, ...flags] = process.argv.slice(2);
const fixture = JSON.parse(readFileSync(fixturePath, 'utf8'));
const noActions = flags.includes('--no-actions');
const actions500 = flags.includes('--actions-500');
const noClient = flags.includes('--no-client');
const userAuthModeFlag = flags.find((flag) => flag.startsWith('--user-auth-mode='));
const userAuthMode = userAuthModeFlag ? userAuthModeFlag.slice('--user-auth-mode='.length) : 'NONE';
const CLIENT_ID = 'app-mock-client';
const USER_TOKEN_HEADER = 'x-bytechef-user-token';
const ACCESS_TOKEN_SECONDS = 300;
const requests = [];
const pendingCodes = new Map();
const accessTokens = new Set();
const refreshTokens = new Set();
const issued = [];

const USER = {
    email: 'ada@example.com',
    firstName: 'Ada',
    id: 7,
    lastName: 'Lovelace',
    login: 'ada@example.com',
    scopes: ['APP_USE'],
    workspaceRole: 'VIEWER',
};

if (!['NONE', 'OPTIONAL', 'REQUIRED'].includes(userAuthMode)) {
    throw new Error(`--user-auth-mode must be NONE, OPTIONAL or REQUIRED, got ${userAuthMode}`);
}

const routes = {
    '/api/app/v1/actions': noActions ? null : fixture.actions,
    '/api/app/v1/api-collections': fixture.apiCollections,
    '/api/app/v1/connections': fixture.connections,
    '/api/app/v1/tools': fixture.tools,
    '/api/app/v1/workflows': fixture.workflows,
};

function sendJson(response, status, body) {
    response.writeHead(status, {'cache-control': 'no-store', 'content-type': 'application/json'});
    response.end(JSON.stringify(body));
}

function newToken(prefix) {
    return `${prefix}${randomBytes(24).toString('base64url')}`;
}

function origin() {
    return `http://127.0.0.1:${server.address().port}`;
}

function appContext() {
    const signInOn = userAuthMode !== 'NONE';

    return {
        appId: 1,
        appInstanceId: 1,
        appInstanceName: 'mock',
        appName: 'mock',
        authorizationEndpoint: signInOn ? `${origin()}/oauth2/authorize` : null,
        clientId: signInOn && !noClient ? CLIENT_ID : null,
        configuration: {},
        environment: 'DEVELOPMENT',
        issuer: signInOn ? origin() : null,
        tokenEndpoint: signInOn ? `${origin()}/oauth2/token` : null,
        userAuthMode,
    };
}

function readBody(request) {
    return new Promise((resolveBody, reject) => {
        let body = '';

        request.setEncoding('utf8');
        request.on('data', (chunk) => {
            body += chunk;
        });
        request.once('end', () => resolveBody(body));
        request.once('error', reject);
    });
}

function oauthError(response, error) {
    sendJson(response, 400, {error});
}

function authorize(url, response) {
    const query = url.searchParams;
    const challenge = query.get('code_challenge') ?? '';
    const redirectUri = query.get('redirect_uri') ?? '';
    const state = query.get('state') ?? '';

    if (userAuthMode === 'NONE') {
        response.writeHead(404, {'content-type': 'text/plain'});
        response.end('The authorization server is off');

        return;
    }

    if (
        query.get('client_id') !== CLIENT_ID ||
        query.get('response_type') !== 'code' ||
        query.get('code_challenge_method') !== 'S256' ||
        !/^[A-Za-z0-9_-]{43}$/.test(challenge) ||
        state.length < 16 ||
        !URL.canParse(redirectUri) ||
        !(query.get('scope') ?? '').split(' ').includes('app:use')
    ) {
        oauthError(response, 'invalid_request');

        return;
    }

    const code = newToken('mock-code-');
    const location = new URL(redirectUri);

    pendingCodes.set(code, {challenge, redirectUri});
    location.searchParams.set('code', code);
    location.searchParams.set('state', state);

    response.writeHead(302, {location: location.toString()});
    response.end();
}

function issueTokens(response) {
    const accessToken = newToken('mock-access-');
    const refreshToken = newToken('mock-refresh-');

    accessTokens.add(accessToken);
    refreshTokens.add(refreshToken);
    issued.push(accessToken, refreshToken);

    sendJson(response, 200, {
        access_token: accessToken,
        expires_in: ACCESS_TOKEN_SECONDS,
        refresh_token: refreshToken,
        scope: 'openid profile app:use',
        token_type: 'Bearer',
    });
}

async function token(request, response) {
    if (
        request.method !== 'POST' ||
        !(request.headers['content-type'] ?? '').startsWith('application/x-www-form-urlencoded')
    ) {
        oauthError(response, 'invalid_request');

        return;
    }

    const form = new URLSearchParams(await readBody(request));

    if (form.get('client_id') !== CLIENT_ID) {
        sendJson(response, 401, {error: 'invalid_client'});

        return;
    }

    if (form.get('grant_type') === 'refresh_token') {
        const refreshToken = form.get('refresh_token') ?? '';

        if (!refreshTokens.delete(refreshToken)) {
            oauthError(response, 'invalid_grant');

            return;
        }

        issueTokens(response);

        return;
    }

    if (form.get('grant_type') !== 'authorization_code') {
        oauthError(response, 'unsupported_grant_type');

        return;
    }

    const code = form.get('code') ?? '';
    const pending = pendingCodes.get(code);

    pendingCodes.delete(code);

    const verifier = form.get('code_verifier') ?? '';
    const verifierChallenge = createHash('sha256').update(verifier).digest('base64url');

    if (
        pending === undefined ||
        form.get('redirect_uri') !== pending.redirectUri ||
        !/^[A-Za-z0-9._~-]{43,128}$/.test(verifier) ||
        verifierChallenge !== pending.challenge
    ) {
        oauthError(response, 'invalid_grant');

        return;
    }

    issueTokens(response);
}

const server = createServer(async (request, response) => {
    const url = new URL(request.url, 'http://mock.invalid');

    if (request.url === '/__requests') {
        sendJson(response, 200, requests);

        return;
    }

    if (request.url === '/__issued') {
        sendJson(response, 200, issued);

        return;
    }

    requests.push({
        authorization: request.headers.authorization ?? null,
        method: request.method,
        url: request.url,
        userToken: request.headers[USER_TOKEN_HEADER] ?? null,
    });

    if (url.pathname === '/oauth2/authorize' && request.method === 'GET') {
        authorize(url, response);

        return;
    }

    if (url.pathname === '/oauth2/token') {
        await token(request, response);

        return;
    }

    const authorization = request.headers.authorization ?? '';

    if (!authorization.startsWith('Bearer ') || authorization === 'Bearer ') {
        sendJson(response, 401, {code: 'APP_UNAUTHORIZED', message: 'Missing app key'});

        return;
    }

    if (request.method !== 'GET') {
        sendJson(response, 405, {
            code: 'MOCK_UNEXPECTED_METHOD',
            message: `Unexpected ${request.method} ${request.url}`,
        });

        return;
    }

    if (request.url === '/api/app/v1/context') {
        sendJson(response, 200, appContext());

        return;
    }

    if (request.url === '/api/app/v1/me') {
        const userToken = request.headers[USER_TOKEN_HEADER];

        if (userToken === undefined) {
            sendJson(response, 401, {code: 'APP_USER_REQUIRED', message: 'No user token'});
        } else if (!accessTokens.has(userToken)) {
            sendJson(response, 401, {code: 'APP_USER_TOKEN_INVALID', message: 'Unknown user token'});
        } else {
            sendJson(response, 200, USER);
        }

        return;
    }

    if (actions500 && request.url === '/api/app/v1/actions') {
        sendJson(response, 500, {code: 'MOCK_INTERNAL_ERROR', message: 'Internal server error'});

        return;
    }

    if (!(request.url in routes) || routes[request.url] === null) {
        response.writeHead(404, {'content-type': 'text/plain'});
        response.end('Not found');

        return;
    }

    sendJson(response, 200, routes[request.url]);
});

server.listen(0, '127.0.0.1', () => {
    process.stdout.write(`${JSON.stringify({port: server.address().port})}\n`);
});
