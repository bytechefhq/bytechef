import {type APIRequestContext, type APIResponse, request} from '@playwright/test';

import {MCP_TEST_CONFIGURATION, type UserCredentialsI} from './mcpTestConfiguration';

interface GraphQlErrorI {
    message: string;
}

export interface GraphQlResponseI<DataType> {
    data?: DataType | null;
    errors?: GraphQlErrorI[];
}

export class BackendSession {
    readonly credentials: UserCredentialsI;
    readonly requestContext: APIRequestContext;

    private constructor(requestContext: APIRequestContext, credentials: UserCredentialsI) {
        this.credentials = credentials;
        this.requestContext = requestContext;
    }

    static async signIn(credentials: UserCredentialsI): Promise<BackendSession> {
        const requestContext = await request.newContext({baseURL: MCP_TEST_CONFIGURATION.baseUrl});
        const backendSession = new BackendSession(requestContext, credentials);

        await requestContext.get('/api/account');

        const signInResponse = await requestContext.post('/api/authentication', {
            form: {password: credentials.password, username: credentials.email},
            headers: await backendSession.getXsrfHeaders(),
            maxRedirects: 0,
        });

        if (signInResponse.status() !== 200) {
            await requestContext.dispose();

            throw new Error(`Sign-in as ${credentials.email} failed with ${signInResponse.status()}`);
        }

        await requestContext.get('/api/account');

        return backendSession;
    }

    async dispose(): Promise<void> {
        await this.requestContext.dispose();
    }

    async getXsrfHeaders(): Promise<Record<string, string>> {
        const storageState = await this.requestContext.storageState();
        const xsrfCookie = storageState.cookies.find((cookie) => cookie.name === 'XSRF-TOKEN');

        return xsrfCookie ? {'X-XSRF-TOKEN': xsrfCookie.value} : {};
    }

    async graphQl<DataType>(
        query: string,
        variables: Record<string, unknown> = {}
    ): Promise<GraphQlResponseI<DataType>> {
        const response = await this.requestContext.post('/graphql', {
            data: {query, variables},
            headers: await this.getXsrfHeaders(),
        });

        return (await response.json()) as GraphQlResponseI<DataType>;
    }

    async graphQlData<DataType>(query: string, variables: Record<string, unknown> = {}): Promise<DataType> {
        const response = await this.graphQl<DataType>(query, variables);

        if (response.errors?.length || !response.data) {
            throw new Error(`GraphQL request failed: ${JSON.stringify(response.errors)}`);
        }

        return response.data;
    }

    async rest(method: 'DELETE' | 'GET' | 'PATCH' | 'POST', path: string, data?: unknown): Promise<APIResponse> {
        return this.requestContext.fetch(path, {
            data,
            headers: await this.getXsrfHeaders(),
            method,
        });
    }

    async restJson<ResponseType>(method: 'GET' | 'POST', path: string, data?: unknown): Promise<ResponseType> {
        const response = await this.rest(method, path, data);

        if (!response.ok()) {
            throw new Error(`${method} ${path} failed with ${response.status()}: ${await response.text()}`);
        }

        return (await response.json()) as ResponseType;
    }
}
