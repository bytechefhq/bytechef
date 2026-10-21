import {readFileSync} from 'node:fs';
import {createServer} from 'node:http';

const [fixturePath, ...flags] = process.argv.slice(2);
const fixture = JSON.parse(readFileSync(fixturePath, 'utf8'));
const noActions = flags.includes('--no-actions');
const actions500 = flags.includes('--actions-500');
const requests = [];

const routes = {
    '/api/app/v1/actions': noActions ? null : fixture.actions,
    '/api/app/v1/api-collections': fixture.apiCollections,
    '/api/app/v1/connections': fixture.connections,
    '/api/app/v1/tools': fixture.tools,
    '/api/app/v1/workflows': fixture.workflows,
};

function sendJson(response, status, body) {
    response.writeHead(status, {'content-type': 'application/json'});
    response.end(JSON.stringify(body));
}

const server = createServer((request, response) => {
    if (request.url === '/__requests') {
        sendJson(response, 200, requests);

        return;
    }

    requests.push({authorization: request.headers.authorization ?? null, method: request.method, url: request.url});

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
