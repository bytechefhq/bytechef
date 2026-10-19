import {type APIResponse, type Page} from '@playwright/test';

import {TEST_USER} from './constants';
import {login} from './login';

export const SERVER_URL = process.env.BYTECHEF_SERVER_URL || 'http://localhost:9555';

export const AUTOMATION_API_PATH = '/api/automation/internal';

export const DEVELOPMENT_ENVIRONMENT_ID = 0;

export interface GraphqlErrorI {
    extensions?: {classification?: string};
    message: string;
}

export interface GraphqlResponseI<T> {
    data?: T;
    errors?: GraphqlErrorI[];
}

async function getXsrfToken(page: Page): Promise<string | undefined> {
    const cookies = await page.context().cookies();

    return cookies.find((cookie) => cookie.name === 'XSRF-TOKEN')?.value;
}

interface ApiFetchProps {
    body?: unknown;
    method: string;
    page: Page;
    path: string;
}

/**
 * Calls the app's authenticated internal API through the logged-in browser context, which carries the session
 * cookie, and echoes the XSRF cookie as the header Spring Security expects.
 */
export async function apiFetch({body, method, page, path}: ApiFetchProps): Promise<APIResponse> {
    const sendRequest = async () => {
        const xsrfToken = await getXsrfToken(page);

        return page.request.fetch(path, {
            data: body,
            headers: xsrfToken ? {'X-XSRF-TOKEN': xsrfToken} : {},
            method,
        });
    };

    const response = await sendRequest();

    if (![401, 403].includes(response.status()) || method === 'GET') {
        return response;
    }

    const accountResponse = await page.request.get('/api/account');

    if (accountResponse.status() !== 200) {
        await login(page, TEST_USER.email, TEST_USER.password);

        await page.request.get('/api/account');
    }

    return sendRequest();
}

export async function apiJson<T>(props: ApiFetchProps): Promise<T> {
    const response = await apiFetch(props);

    const responseText = await response.text();

    if (!response.ok()) {
        throw new Error(`${props.method} ${props.path} answered ${response.status()}: ${responseText}`);
    }

    return (responseText ? JSON.parse(responseText) : undefined) as T;
}

export async function graphqlRequest<T>(
    page: Page,
    query: string,
    variables: Record<string, unknown> = {}
): Promise<GraphqlResponseI<T>> {
    const response = await apiFetch({body: {query, variables}, method: 'POST', page, path: '/graphql'});

    const responseText = await response.text();

    if (!responseText) {
        throw new Error(`GraphQL request answered ${response.status()} with an empty body`);
    }

    return JSON.parse(responseText) as GraphqlResponseI<T>;
}

export async function graphqlData<T>(page: Page, query: string, variables: Record<string, unknown> = {}): Promise<T> {
    const graphqlResponse = await graphqlRequest<T>(page, query, variables);

    if (graphqlResponse.errors?.length || !graphqlResponse.data) {
        throw new Error(`GraphQL request failed: ${JSON.stringify(graphqlResponse.errors)}`);
    }

    return graphqlResponse.data;
}

export function toServerUrl(url: string): string {
    const parsedUrl = new URL(url);

    return `${SERVER_URL}${parsedUrl.pathname}${parsedUrl.search}`;
}
