import {describe, expect, it} from 'vitest';

import {buildCursorInstallUrl, buildVsCodeInstallUrl} from '../mcpConnectDeepLinks';

const MCP_SERVER_URL = 'https://example.com/api/management/mcp';
const RESERVED_CHARACTERS_URL = 'https://h.example/api/automation/MDAw+a/b=c?x=1/mcp';

const CURSOR_PREFIX = 'cursor://anysphere.cursor-deeplink/mcp/install?';
const VS_CODE_PREFIX = 'vscode:mcp/install?';

const decodeCursorInstallUrl = (installUrl: string) => {
    expect(installUrl.startsWith(CURSOR_PREFIX)).toBe(true);

    const searchParams = new URLSearchParams(installUrl.slice(CURSOR_PREFIX.length));

    return {config: JSON.parse(atob(searchParams.get('config')!)), name: searchParams.get('name')};
};

const decodeVsCodeInstallUrl = (installUrl: string) => {
    expect(installUrl.startsWith(VS_CODE_PREFIX)).toBe(true);

    return JSON.parse(decodeURIComponent(installUrl.slice(VS_CODE_PREFIX.length)));
};

describe('buildCursorInstallUrl', () => {
    it('builds a Cursor link without headers', () => {
        const {config, name} = decodeCursorInstallUrl(
            buildCursorInstallUrl({apiKeyHeader: false, url: MCP_SERVER_URL})
        );

        expect(name).toBe('ByteChef');
        expect(config).toEqual({url: MCP_SERVER_URL});
    });

    it('builds a Cursor link with the API key header', () => {
        const {config} = decodeCursorInstallUrl(buildCursorInstallUrl({apiKeyHeader: true, url: MCP_SERVER_URL}));

        expect(config).toEqual({headers: {Authorization: 'Bearer YOUR_API_KEY'}, url: MCP_SERVER_URL});
    });
});

describe('buildVsCodeInstallUrl', () => {
    it('builds a VS Code link without headers', () => {
        expect(decodeVsCodeInstallUrl(buildVsCodeInstallUrl({apiKeyHeader: false, url: MCP_SERVER_URL}))).toEqual({
            name: 'ByteChef',
            type: 'http',
            url: MCP_SERVER_URL,
        });
    });

    it('builds a VS Code link with an input prompt for the key', () => {
        const config = decodeVsCodeInstallUrl(buildVsCodeInstallUrl({apiKeyHeader: true, url: MCP_SERVER_URL}));

        expect(config.headers).toEqual({Authorization: 'Bearer ${input:bytechef-api-key}'});
        expect(config.inputs).toEqual([
            {description: 'ByteChef API key', id: 'bytechef-api-key', password: true, type: 'promptString'},
        ]);
    });
});

describe('install links', () => {
    it('round-trips a URL with reserved characters', () => {
        expect(
            decodeCursorInstallUrl(buildCursorInstallUrl({apiKeyHeader: false, url: RESERVED_CHARACTERS_URL})).config
                .url
        ).toBe(RESERVED_CHARACTERS_URL);
        expect(
            decodeVsCodeInstallUrl(buildVsCodeInstallUrl({apiKeyHeader: false, url: RESERVED_CHARACTERS_URL})).url
        ).toBe(RESERVED_CHARACTERS_URL);
    });
});
