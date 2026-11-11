import {describe, expect, it} from 'vitest';

import buildFirstCallCurl from './buildFirstCallCurl';

const apiBaseUrl = 'https://example.com/api/embedded/v1';

describe('buildFirstCallCurl', () => {
    it('builds a complete GET call for the HTTP Client component', () => {
        const curl = buildFirstCallCurl({
            apiBaseUrl,
            componentName: 'httpClient',
            componentVersion: 2,
            environmentName: 'DEVELOPMENT',
            externalUserId: 'user-42',
        });

        expect(curl).toBe(
            [
                "curl -X POST 'https://example.com/api/embedded/v1/user-42/components/httpClient/versions/2/actions/get' \\",
                '  -H "Authorization: Bearer $BYTECHEF_API_KEY" \\',
                "  -H 'X-Environment: DEVELOPMENT' \\",
                "  -H 'Content-Type: application/json' \\",
                `  -d '{"input": {"uri": "https://httpbin.org/get"}}'`,
            ].join('\n')
        );
    });

    it('leaves the action and input as placeholders for other components', () => {
        const curl = buildFirstCallCurl({
            apiBaseUrl,
            componentName: 'github',
            componentVersion: 1,
            environmentName: 'STAGING',
            externalUserId: 'user-42',
        });

        expect(curl).toContain('/user-42/components/github/versions/1/actions/ACTION_NAME');
        expect(curl).toContain('X-Environment: STAGING');
        expect(curl).toContain(`-d '{"input": {}}'`);
    });

    it('encodes URL delimiters in the external user ID as part of its path segment', () => {
        const curl = buildFirstCallCurl({
            apiBaseUrl,
            componentName: 'github',
            environmentName: 'DEVELOPMENT',
            externalUserId: 'customer#42/a?b',
        });

        expect(curl).toContain(`'${apiBaseUrl}/customer%2342%2Fa%3Fb/components/github/`);
    });

    it('escapes an apostrophe in the external user ID for the shell', () => {
        const curl = buildFirstCallCurl({
            apiBaseUrl,
            componentName: 'github',
            environmentName: 'DEVELOPMENT',
            externalUserId: "o'brien",
        });

        expect(curl).toContain(`curl -X POST '${apiBaseUrl}/o'\\''brien/components/github/`);
    });

    it('uses placeholders when the test user and integration are not set up yet', () => {
        const curl = buildFirstCallCurl({apiBaseUrl, environmentName: 'DEVELOPMENT'});

        expect(curl).toContain(
            `${apiBaseUrl}/EXTERNAL_USER_ID/components/COMPONENT_NAME/versions/1/actions/ACTION_NAME`
        );
    });

    it('reads the API key from an environment variable', () => {
        const curl = buildFirstCallCurl({apiBaseUrl, environmentName: 'DEVELOPMENT'});

        expect(curl).toContain('"Authorization: Bearer $BYTECHEF_API_KEY"');
    });
});
