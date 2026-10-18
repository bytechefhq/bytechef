// A minimal stand-in for GitHub's OAuth2 endpoints, used by the authentication Playwright specs.
//
// GET  /login/oauth/authorize     renders a sign-in form: a JSON "claims" field holding the GitHub user profile
// POST /login/oauth/authorize     redirects back to redirect_uri with a code
// POST /login/oauth/access_token  exchanges the code for a token
// GET  /user                      returns the profile; GET /user/emails returns its emails (primary, verified)
import {randomBytes} from 'node:crypto';
import {createServer} from 'node:http';

const codes = new Map();
const tokens = new Map();

function readBody(request) {
    return new Promise((resolve) => {
        let body = '';

        request.on('data', (chunk) => {
            body += chunk;
        });

        request.on('end', () => resolve(new URLSearchParams(body)));
    });
}

function sendJson(response, status, body) {
    response.writeHead(status, {'Content-Type': 'application/json'});
    response.end(JSON.stringify(body));
}

function escapeHtml(value) {
    return value.replace(/[&<>"']/g, (character) => `&#${character.charCodeAt(0)};`);
}

function profileFor(request) {
    const authorization = request.headers.authorization || '';

    return tokens.get(authorization.replace(/^(Bearer|token)\s+/i, ''));
}

const server = createServer(async (request, response) => {
    const url = new URL(request.url, 'http://localhost');

    if (url.pathname === '/login/oauth/authorize' && request.method === 'GET') {
        response.writeHead(200, {'Content-Type': 'text/html'});
        response.end(`<!doctype html><title>GitHub mock sign-in</title>
<form method="post" action="${escapeHtml(url.pathname + url.search)}">
<label>Claims <textarea name="claims" aria-label="Claims">{"id": 1, "email": "octocat@example.com", "name": "Octo Cat"}</textarea></label>
<button type="submit">Sign in to GitHub</button>
</form>`);

        return;
    }

    if (url.pathname === '/login/oauth/authorize' && request.method === 'POST') {
        const form = await readBody(request);
        const code = randomBytes(12).toString('hex');

        codes.set(code, JSON.parse(form.get('claims')));

        const redirectUri = new URL(url.searchParams.get('redirect_uri'));

        redirectUri.searchParams.set('code', code);
        redirectUri.searchParams.set('state', url.searchParams.get('state'));

        response.writeHead(302, {Location: redirectUri.toString()});
        response.end();

        return;
    }

    if (url.pathname === '/login/oauth/access_token' && request.method === 'POST') {
        const form = await readBody(request);
        const profile = codes.get(form.get('code'));

        if (!profile) {
            sendJson(response, 400, {error: 'bad_verification_code'});

            return;
        }

        codes.delete(form.get('code'));

        const token = randomBytes(16).toString('hex');

        tokens.set(token, profile);

        sendJson(response, 200, {access_token: token, scope: 'read:user,user:email', token_type: 'bearer'});

        return;
    }

    if (url.pathname === '/user' || url.pathname === '/user/emails') {
        const profile = profileFor(request);

        if (!profile) {
            sendJson(response, 401, {message: 'Bad credentials'});

            return;
        }

        if (url.pathname === '/user') {
            sendJson(response, 200, {...profile, email: profile.public_email === false ? null : profile.email});
        } else {
            sendJson(response, 200, [{email: profile.email, primary: true, verified: profile.verified !== false}]);
        }

        return;
    }

    sendJson(response, 404, {message: 'Not Found'});
});

server.listen(Number(process.env.PORT || 8080));
