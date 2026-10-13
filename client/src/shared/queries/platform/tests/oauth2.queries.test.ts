import {describe, expect, it} from 'vitest';

import {OAuth2Keys, getOAuth2AuthorizationParametersQueryOptions} from '../oauth2.queries';

describe('getOAuth2AuthorizationParametersQueryOptions', () => {
    it('never refetches once the authorization parameters were fetched', () => {
        const options = getOAuth2AuthorizationParametersQueryOptions(
            {
                authorizationType: 'OAUTH2_AUTHORIZATION_CODE_PKCE',
                componentName: 'mcpClient',
                connectionVersion: 1,
                parameters: {},
            },
            true
        );

        expect(options.staleTime).toBe(Infinity);
        expect(options.refetchOnWindowFocus).toBe(false);
        expect(options.refetchOnReconnect).toBe(false);
    });

    it('neither caches nor retries the authorization parameters, since each fetch can register a client', () => {
        const options = getOAuth2AuthorizationParametersQueryOptions(
            {
                authorizationType: 'OAUTH2_AUTHORIZATION_CODE_PKCE',
                componentName: 'mcpClient',
                connectionVersion: 1,
                parameters: {},
            },
            true
        );

        expect(options.gcTime).toBe(0);
        expect(options.retry).toBe(false);
    });

    it('keys every authorization-parameters query under a shared prefix', () => {
        const options = getOAuth2AuthorizationParametersQueryOptions({
            authorizationType: 'OAUTH2_AUTHORIZATION_CODE_PKCE',
            componentName: 'mcpClient',
            connectionId: 7,
            connectionVersion: 1,
            parameters: {url: 'https://mcp.example.com/mcp'},
        });

        expect(options.queryKey.slice(0, OAuth2Keys.oAuth2AuthorizationParametersPrefix.length)).toEqual([
            ...OAuth2Keys.oAuth2AuthorizationParametersPrefix,
        ]);
        expect(options.queryKey).toContain(7);
    });
});
