import {describe, expect, it} from 'vitest';

import CursorLogo from '../client-logos/CursorLogo';
import McpLogo from '../client-logos/McpLogo';
import {McpConnectAuthModeType} from '../mcpConnectAuthMode';
import {CHATGPT_API_KEY_NOTICE, MCP_CONNECT_CLIENTS, McpConnectContextI, McpConnectTabI} from '../mcpConnectClients';

const MCP_SERVER_URL = 'https://example.com/api/automation/secret/mcp';
const AUTH_MODES: McpConnectAuthModeType[] = ['URL_ONLY', 'API_KEY', 'API_KEY_OR_OAUTH'];
const CURSOR_PREFIX = 'cursor://anysphere.cursor-deeplink/mcp/install?';

const createContext = (mode: McpConnectAuthModeType): McpConnectContextI => ({
    apiKeySource: 'an API key created under Settings',
    mode,
    url: MCP_SERVER_URL,
});

const decodeHref = (href: string): string => {
    if (href.startsWith(CURSOR_PREFIX)) {
        const searchParams = new URLSearchParams(href.slice(CURSOR_PREFIX.length));

        return `${href} ${atob(searchParams.get('config') ?? '')}`;
    }

    return decodeURIComponent(href);
};

const collectTabText = (tab: McpConnectTabI): string =>
    [
        tab.notice,
        ...tab.steps.flatMap((step) => [
            step.title,
            step.description,
            step.codeSnippet,
            ...(step.path ?? []),
            step.action ? decodeHref(step.action.href) : undefined,
        ]),
    ]
        .filter((text) => text !== undefined)
        .join('\n');

describe('MCP_CONNECT_CLIENTS', () => {
    it('lists the v1 clients in order', () => {
        expect(MCP_CONNECT_CLIENTS.map((client) => client.title)).toEqual([
            'Claude',
            'Claude Code',
            'Cursor',
            'VS Code',
            'ChatGPT',
            'Codex CLI',
            'Devin Desktop',
            'Any MCP client',
        ]);
    });

    it('uses the brand marks for Cursor and Any MCP client', () => {
        const logosById = Object.fromEntries(MCP_CONNECT_CLIENTS.map((client) => [client.id, client.logo]));

        expect(logosById['cursor']).toBe(CursorLogo);
        expect(logosById['any-mcp-client']).toBe(McpLogo);
    });

    it('returns at least one tab for every client and mode', () => {
        for (const client of MCP_CONNECT_CLIENTS) {
            for (const mode of AUTH_MODES) {
                expect(client.getTabs(createContext(mode)).length, `${client.title} ${mode}`).toBeGreaterThan(0);
            }
        }
    });

    it('never mentions an API key in URL_ONLY mode', () => {
        for (const client of MCP_CONNECT_CLIENTS) {
            const text = client.getTabs(createContext('URL_ONLY')).map(collectTabText).join('\n');

            expect(text, client.title).not.toContain('YOUR_API_KEY');
            expect(text, client.title).not.toContain('Authorization');
        }
    });

    it('includes the URL in every client and mode', () => {
        for (const client of MCP_CONNECT_CLIENTS) {
            for (const mode of AUTH_MODES) {
                if (client.id === 'chatgpt' && mode === 'API_KEY') {
                    continue;
                }

                const text = client.getTabs(createContext(mode)).map(collectTabText).join('\n');

                expect(text, `${client.title} ${mode}`).toContain(MCP_SERVER_URL);
            }
        }
    });

    it('shows only the API key notice for ChatGPT in API_KEY mode', () => {
        const chatGpt = MCP_CONNECT_CLIENTS.find((client) => client.id === 'chatgpt')!;

        const tabs = chatGpt.getTabs(createContext('API_KEY'));

        expect(tabs).toHaveLength(1);
        expect(tabs[0].notice).toBe(CHATGPT_API_KEY_NOTICE);
        expect(tabs[0].steps).toHaveLength(0);
    });

    it('offers sign-in steps in API_KEY_OR_OAUTH mode for Claude, Claude Code, ChatGPT and Codex CLI', () => {
        for (const id of ['claude', 'claude-code', 'chatgpt', 'codex-cli']) {
            const client = MCP_CONNECT_CLIENTS.find((connectClient) => connectClient.id === id)!;

            const oneClickTab = client.getTabs(createContext('API_KEY_OR_OAUTH')).find((tab) => tab.id === 'ONE_CLICK');

            expect(oneClickTab, id).toBeDefined();
            expect(collectTabText(oneClickTab!), id).toMatch(/sign in|login|Authenticate/i);
            expect(collectTabText(oneClickTab!), id).not.toContain('YOUR_API_KEY');
        }
    });

    it('uses the given API key source in API_KEY mode', () => {
        for (const client of MCP_CONNECT_CLIENTS) {
            if (client.id === 'chatgpt') {
                continue;
            }

            const text = client
                .getTabs({...createContext('API_KEY'), apiKeySource: 'an admin API key'})
                .map(collectTabText)
                .join('\n');

            expect(text, client.title).toContain('an admin API key');
        }
    });

    it('tells the user to reload Cursor after the one-click install', () => {
        const cursor = MCP_CONNECT_CLIENTS.find((client) => client.id === 'cursor')!;

        for (const mode of AUTH_MODES) {
            const oneClickTab = cursor.getTabs(createContext(mode)).find((tab) => tab.id === 'ONE_CLICK')!;
            const lastStep = oneClickTab.steps[oneClickTab.steps.length - 1];

            expect(lastStep.title, mode).toBe('Reload Cursor');
            expect(lastStep.path, mode).toEqual(['Command Palette', 'Developer: Reload Window']);
        }
    });

    it('uses the base64 Cursor link and the VS Code input prompt for API keys', () => {
        const cursor = MCP_CONNECT_CLIENTS.find((client) => client.id === 'cursor')!;
        const vsCode = MCP_CONNECT_CLIENTS.find((client) => client.id === 'vs-code')!;

        const cursorAction = cursor.getTabs(createContext('API_KEY'))[0].steps.find((step) => step.action)!.action!;
        const vsCodeAction = vsCode.getTabs(createContext('API_KEY'))[0].steps.find((step) => step.action)!.action!;

        expect(cursorAction.label).toBe('Add to Cursor');
        expect(decodeHref(cursorAction.href)).toContain('Bearer YOUR_API_KEY');
        expect(vsCodeAction.label).toBe('Add to VS Code');
        expect(decodeHref(vsCodeAction.href)).toContain('${input:bytechef-api-key}');
    });

    it('keeps the API key header for Devin Desktop when OAuth is available', () => {
        const devinDesktop = MCP_CONNECT_CLIENTS.find((client) => client.id === 'devin-desktop')!;

        const text = devinDesktop.getTabs(createContext('API_KEY_OR_OAUTH')).map(collectTabText).join('\n');

        expect(text).toContain('"Authorization": "Bearer YOUR_API_KEY"');
        expect(text).not.toMatch(/sign in/i);
    });

    it('offers a .mcp.json snippet with the http type for Claude Code', () => {
        const claudeCode = MCP_CONNECT_CLIENTS.find((client) => client.id === 'claude-code')!;

        for (const mode of ['API_KEY', 'API_KEY_OR_OAUTH'] as McpConnectAuthModeType[]) {
            const manualTab = claudeCode.getTabs(createContext(mode)).find((tab) => tab.id === 'MANUAL')!;

            expect(collectTabText(manualTab), mode).toContain('"type": "http"');
            expect(collectTabText(manualTab), mode).toContain('.mcp.json');
        }
    });
});
