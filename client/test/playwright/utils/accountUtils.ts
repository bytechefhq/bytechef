import {APIRequestContext, APIResponse, expect, request} from '@playwright/test';

import {TEST_USER} from './constants';

export const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://127.0.0.1:5173';

const MAILPIT_URL = process.env.PLAYWRIGHT_MAILPIT_URL || 'http://localhost:8025';

async function getXsrfToken(apiContext: APIRequestContext): Promise<string> {
    await apiContext.get('/api/authenticate');

    const storageState = await apiContext.storageState();

    return storageState.cookies.find((cookie) => cookie.name === 'XSRF-TOKEN')?.value || '';
}

export async function createAnonymousApiContext(): Promise<APIRequestContext> {
    return request.newContext({baseURL: BASE_URL});
}

export async function signIn(apiContext: APIRequestContext, email: string, password: string): Promise<APIResponse> {
    const xsrfToken = await getXsrfToken(apiContext);

    return apiContext.post('/api/authentication', {
        form: {password, 'remember-me': 'false', username: email},
        headers: {'X-XSRF-TOKEN': xsrfToken},
    });
}

export async function createUserApiContext(email: string, password: string): Promise<APIRequestContext> {
    const apiContext = await createAnonymousApiContext();

    const response = await signIn(apiContext, email, password);

    expect(response.ok(), `login of ${email} failed: ${response.status()}`).toBeTruthy();

    return apiContext;
}

export async function createAdminApiContext(): Promise<APIRequestContext> {
    return createUserApiContext(TEST_USER.email, TEST_USER.password);
}

export async function graphql<T>(
    apiContext: APIRequestContext,
    query: string,
    variables: Record<string, unknown>
): Promise<T> {
    const xsrfToken = await getXsrfToken(apiContext);

    const response = await apiContext.post('/graphql', {
        data: {query, variables},
        headers: {'X-XSRF-TOKEN': xsrfToken},
    });

    const body = await response.json();

    expect(body.errors, `GraphQL errors: ${JSON.stringify(body.errors)}`).toBeUndefined();

    return body.data as T;
}

export async function inviteUser(apiContext: APIRequestContext, email: string, role = 'ROLE_USER'): Promise<void> {
    await graphql(
        apiContext,
        'mutation InviteUser($email: String!, $role: String!) { inviteUser(email: $email, role: $role) }',
        {email, role}
    );
}

export async function deleteUser(apiContext: APIRequestContext, login: string): Promise<void> {
    await graphql(apiContext, 'mutation DeleteUser($login: String!) { deleteUser(login: $login) }', {login}).catch(
        () => undefined
    );
}

export async function readMail(email: string, linkPattern: RegExp): Promise<{content: string; key: string}> {
    const mailpitContext = await request.newContext({baseURL: MAILPIT_URL});

    try {
        let content = '';
        let key = '';

        await expect(async () => {
            const searchResponse = await mailpitContext.get('/api/v1/search', {
                params: {limit: '1', query: `to:"${email}"`},
            });

            const messageId = (await searchResponse.json()).messages?.[0]?.ID;

            expect(messageId, `no mail for ${email} yet`).toBeTruthy();

            const message = await (await mailpitContext.get(`/api/v1/message/${messageId}`)).json();

            content = `${message.HTML} ${message.Text}`;

            const match = content.match(linkPattern);

            expect(match, `no matching link in the mail to ${email}`).toBeTruthy();

            key = match![1];
        }).toPass({timeout: 30000});

        return {content, key};
    } finally {
        await mailpitContext.dispose();
    }
}

export async function readInvitationKey(email: string): Promise<{content: string; key: string}> {
    return readMail(email, /accept-invitation\?key=([A-Za-z0-9]+)/);
}
