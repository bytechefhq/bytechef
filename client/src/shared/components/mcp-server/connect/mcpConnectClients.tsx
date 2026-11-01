import {BotIcon, BoxIcon, CodeXmlIcon, PlugIcon} from 'lucide-react';
import {ComponentType} from 'react';

import AnthropicLogo from './client-logos/AnthropicLogo';
import OpenAiLogo from './client-logos/OpenAiLogo';
import {MCP_API_KEY_PLACEHOLDER, McpConnectAuthModeType} from './mcpConnectAuthMode';
import {buildCursorInstallUrl, buildVsCodeInstallUrl} from './mcpConnectDeepLinks';

export const CHATGPT_API_KEY_NOTICE =
    "ChatGPT can't send API keys. Ask your ByteChef administrator to enable the OAuth authorization server so ChatGPT can sign in.";

const BEARER_HEADER_VALUE = `Bearer ${MCP_API_KEY_PLACEHOLDER}`;

export interface McpConnectStepI {
    action?: {href: string; label: string};
    codeSnippet?: string;
    description?: string;
    path?: string[];
    title: string;
}

export interface McpConnectTabI {
    id: 'MANUAL' | 'ONE_CLICK';
    label: 'Manual' | 'One click';
    notice?: string;
    steps: McpConnectStepI[];
}

export interface McpConnectContextI {
    apiKeySource: string;
    mode: McpConnectAuthModeType;
    url: string;
}

export interface McpConnectClientI {
    docsUrl: string;
    getTabs: (context: McpConnectContextI) => McpConnectTabI[];
    id: string;
    logo: ComponentType<{className?: string}>;
    subtitle?: string;
    title: string;
}

const oneClickTab = (steps: McpConnectStepI[]): McpConnectTabI => ({id: 'ONE_CLICK', label: 'One click', steps});

const manualTab = (steps: McpConnectStepI[]): McpConnectTabI => ({id: 'MANUAL', label: 'Manual', steps});

const replaceApiKeyText = (apiKeySource: string) => `Replace ${MCP_API_KEY_PLACEHOLDER} with ${apiKeySource}.`;

const toJson = (value: unknown) => JSON.stringify(value, null, 2);

const getClaudeTabs = ({apiKeySource, mode, url}: McpConnectContextI): McpConnectTabI[] => {
    const addConnectorStep: McpConnectStepI = {
        codeSnippet: url,
        description:
            'Name it ByteChef and paste the server URL. On Team and Enterprise plans, an Owner adds it under Organization settings → Connectors.',
        path: ['Customize', 'Connectors', 'Add custom connector'],
        title: 'Add a custom connector',
    };

    if (mode === 'URL_ONLY') {
        return [
            oneClickTab([
                addConnectorStep,
                {
                    description: 'Choose No sign-in and save. ByteChef tools are now available in your chats.',
                    title: 'Connect',
                },
            ]),
        ];
    }

    const manual = manualTab([
        addConnectorStep,
        {
            codeSnippet: BEARER_HEADER_VALUE,
            description: `Choose No sign-in, then under Request headers add the name authorization with the value below. ${replaceApiKeyText(apiKeySource)} Request headers are in beta and may not be available to your organization yet.`,
            title: 'Add the API key header',
        },
        {
            codeSnippet: toJson({
                mcpServers: {
                    bytechef: {
                        args: ['-y', 'mcp-remote', url, '--header', `Authorization: ${BEARER_HEADER_VALUE}`],
                        command: 'npx',
                    },
                },
            }),
            description: `If request headers are not available, paste this into claude_desktop_config.json, then quit and restart Claude. ${replaceApiKeyText(apiKeySource)}`,
            path: ['Settings', 'Developer', 'Edit Config'],
            title: 'Or use Claude Desktop',
        },
    ]);

    if (mode === 'API_KEY') {
        return [manual];
    }

    return [
        oneClickTab([
            addConnectorStep,
            {
                description: 'Choose Sign in now, then approve access when ByteChef opens in your browser.',
                title: 'Sign in',
            },
        ]),
        manual,
    ];
};

const getClaudeCodeTabs = ({apiKeySource, mode, url}: McpConnectContextI): McpConnectTabI[] => {
    const addCommand = `claude mcp add --transport http bytechef ${url}`;

    if (mode === 'URL_ONLY') {
        return [
            oneClickTab([
                {codeSnippet: addCommand, description: 'Run this in your terminal.', title: 'Add the server'},
            ]),
        ];
    }

    const manual = manualTab([
        {
            codeSnippet: `${addCommand} --header "Authorization: ${BEARER_HEADER_VALUE}"`,
            description: `${replaceApiKeyText(apiKeySource)} Then run this in your terminal.`,
            title: 'Add the server with your API key',
        },
        {
            codeSnippet: toJson({
                mcpServers: {bytechef: {headers: {Authorization: BEARER_HEADER_VALUE}, type: 'http', url}},
            }),
            description: `Or share it with your team: add this to .mcp.json in your project. ${replaceApiKeyText(apiKeySource)}`,
            title: 'Or add it to .mcp.json',
        },
    ]);

    if (mode === 'API_KEY') {
        return [manual];
    }

    return [
        oneClickTab([
            {codeSnippet: addCommand, description: 'Run this in your terminal.', title: 'Add the server'},
            {
                description: 'In Claude Code, run /mcp, select bytechef and choose Authenticate to sign in.',
                title: 'Sign in',
            },
        ]),
        manual,
    ];
};

const getCursorTabs = ({apiKeySource, mode, url}: McpConnectContextI): McpConnectTabI[] => {
    const apiKeyHeader = mode === 'API_KEY';

    const oneClickDescriptions: Record<McpConnectAuthModeType, string> = {
        API_KEY: `Opens Cursor and adds the ByteChef server. Then open ~/.cursor/mcp.json and replace ${MCP_API_KEY_PLACEHOLDER} with ${apiKeySource}.`,
        API_KEY_OR_OAUTH:
            'Opens Cursor and adds the ByteChef server. Cursor asks you to sign in to ByteChef when it connects.',
        URL_ONLY: 'Opens Cursor and adds the ByteChef server.',
    };

    const withHeaders = mode !== 'URL_ONLY';

    return [
        oneClickTab([
            {
                action: {href: buildCursorInstallUrl({apiKeyHeader, url}), label: 'Add to Cursor'},
                description: oneClickDescriptions[mode],
                title: 'Install in Cursor',
            },
        ]),
        manualTab([
            {
                codeSnippet: toJson({
                    mcpServers: {
                        bytechef: withHeaders ? {headers: {Authorization: BEARER_HEADER_VALUE}, url} : {url},
                    },
                }),
                description: withHeaders
                    ? `Paste this into ~/.cursor/mcp.json (or .cursor/mcp.json in a project) and save. ${replaceApiKeyText(apiKeySource)}`
                    : 'Paste this into ~/.cursor/mcp.json (or .cursor/mcp.json in a project) and save.',
                title: 'Add to mcp.json',
            },
        ]),
    ];
};

const getVsCodeTabs = ({apiKeySource, mode, url}: McpConnectContextI): McpConnectTabI[] => {
    const oneClickDescriptions: Record<McpConnectAuthModeType, string> = {
        API_KEY: `Opens VS Code and adds the ByteChef server. VS Code asks for ${apiKeySource} the first time the server starts.`,
        API_KEY_OR_OAUTH:
            'Opens VS Code and adds the ByteChef server. VS Code asks you to sign in to ByteChef when the server starts.',
        URL_ONLY: 'Opens VS Code and adds the ByteChef server.',
    };

    const withHeaders = mode !== 'URL_ONLY';

    return [
        oneClickTab([
            {
                action: {href: buildVsCodeInstallUrl({apiKeyHeader: mode === 'API_KEY', url}), label: 'Add to VS Code'},
                description: oneClickDescriptions[mode],
                title: 'Install in VS Code',
            },
        ]),
        manualTab([
            {
                codeSnippet: toJson(
                    withHeaders
                        ? {
                              inputs: [
                                  {
                                      description: 'ByteChef API key',
                                      id: 'bytechef-api-key',
                                      password: true,
                                      type: 'promptString',
                                  },
                              ],
                              servers: {
                                  bytechef: {
                                      headers: {Authorization: 'Bearer ${input:bytechef-api-key}'},
                                      type: 'http',
                                      url,
                                  },
                              },
                          }
                        : {servers: {bytechef: {type: 'http', url}}}
                ),
                description: withHeaders
                    ? `Paste this into .vscode/mcp.json in your workspace, or run MCP: Open User Configuration to add it everywhere. VS Code asks for ${apiKeySource} when the server starts.`
                    : 'Paste this into .vscode/mcp.json in your workspace, or run MCP: Open User Configuration to add it everywhere.',
                title: 'Add to mcp.json',
            },
        ]),
    ];
};

const getChatGptTabs = ({mode, url}: McpConnectContextI): McpConnectTabI[] => {
    if (mode === 'API_KEY') {
        return [{...oneClickTab([]), notice: CHATGPT_API_KEY_NOTICE}];
    }

    const addServerStep: McpConnectStepI = {
        codeSnippet: url,
        description:
            mode === 'URL_ONLY'
                ? 'Name it ByteChef, paste the server URL and choose No authentication.'
                : 'Name it ByteChef, paste the server URL and choose OAuth.',
        path: ['Plugins', '+', 'Add custom MCP server'],
        title: 'Add a custom MCP server',
    };

    if (mode === 'URL_ONLY') {
        return [oneClickTab([addServerStep])];
    }

    return [
        oneClickTab([
            addServerStep,
            {description: 'Approve access when ByteChef opens in your browser to sign in.', title: 'Sign in'},
        ]),
    ];
};

const getCodexCliTabs = ({apiKeySource, mode, url}: McpConnectContextI): McpConnectTabI[] => {
    const addStep: McpConnectStepI = {
        codeSnippet: `codex mcp add bytechef --url ${url}`,
        description: 'Run this in your terminal.',
        title: 'Add the server',
    };

    if (mode === 'URL_ONLY') {
        return [oneClickTab([addStep])];
    }

    const manual = manualTab([
        {
            codeSnippet: `[mcp_servers.bytechef]\nurl = "${url}"\nbearer_token_env_var = "BYTECHEF_API_KEY"`,
            description: 'Paste this into ~/.codex/config.toml.',
            title: 'Add to config.toml',
        },
        {
            codeSnippet: `export BYTECHEF_API_KEY=${MCP_API_KEY_PLACEHOLDER}`,
            description: `Set BYTECHEF_API_KEY before starting Codex. ${replaceApiKeyText(apiKeySource)}`,
            title: 'Set your API key',
        },
    ]);

    if (mode === 'API_KEY') {
        return [manual];
    }

    return [
        oneClickTab([
            addStep,
            {
                codeSnippet: 'codex mcp login bytechef',
                description: 'Run this to sign in to ByteChef.',
                title: 'Sign in',
            },
        ]),
        manual,
    ];
};

const getDevinDesktopTabs = ({apiKeySource, mode, url}: McpConnectContextI): McpConnectTabI[] => {
    const apiKeyHeader = mode !== 'URL_ONLY';

    const descriptions: Record<McpConnectAuthModeType, string> = {
        API_KEY: `Add a custom server, paste this configuration and save. ${replaceApiKeyText(apiKeySource)}`,
        API_KEY_OR_OAUTH: `Add a custom server, paste this configuration and save. ${replaceApiKeyText(apiKeySource)}`,
        URL_ONLY: 'Add a custom server, paste this configuration and save.',
    };

    return [
        manualTab([
            {
                codeSnippet: toJson({
                    mcpServers: {
                        bytechef: apiKeyHeader
                            ? {headers: {Authorization: BEARER_HEADER_VALUE}, serverUrl: url}
                            : {serverUrl: url},
                    },
                }),
                description: descriptions[mode],
                path: ['Agents', 'Customize', 'MCPs'],
                title: 'Add a custom server',
            },
        ]),
    ];
};

const getAnyMcpClientTabs = ({apiKeySource, mode, url}: McpConnectContextI): McpConnectTabI[] => {
    const descriptions: Record<McpConnectAuthModeType, string> = {
        API_KEY: `This URL requires ${apiKeySource}, sent with every request as an Authorization: Bearer header.`,
        API_KEY_OR_OAUTH: `Sign in with a ByteChef account if your client supports MCP OAuth, or send ${apiKeySource} with every request as an Authorization: Bearer header.`,
        URL_ONLY: 'This server does not require authentication, so the URL can be used as is.',
    };

    return [manualTab([{codeSnippet: url, description: descriptions[mode], title: 'Server URL'}])];
};

export const MCP_CONNECT_CLIENTS: McpConnectClientI[] = [
    {
        docsUrl: 'https://claude.com/docs/connectors/custom/add-unlisted',
        getTabs: getClaudeTabs,
        id: 'claude',
        logo: AnthropicLogo,
        title: 'Claude',
    },
    {
        docsUrl: 'https://code.claude.com/docs/en/mcp',
        getTabs: getClaudeCodeTabs,
        id: 'claude-code',
        logo: AnthropicLogo,
        title: 'Claude Code',
    },
    {
        docsUrl: 'https://cursor.com/docs/context/mcp',
        getTabs: getCursorTabs,
        id: 'cursor',
        logo: BoxIcon,
        title: 'Cursor',
    },
    {
        docsUrl: 'https://code.visualstudio.com/docs/copilot/reference/mcp-configuration',
        getTabs: getVsCodeTabs,
        id: 'vs-code',
        logo: CodeXmlIcon,
        title: 'VS Code',
    },
    {
        docsUrl: 'https://developers.openai.com/apps-sdk/deploy/connect-chatgpt',
        getTabs: getChatGptTabs,
        id: 'chatgpt',
        logo: OpenAiLogo,
        title: 'ChatGPT',
    },
    {
        docsUrl: 'https://developers.openai.com/codex/mcp',
        getTabs: getCodexCliTabs,
        id: 'codex-cli',
        logo: OpenAiLogo,
        title: 'Codex CLI',
    },
    {
        docsUrl: 'https://docs.devin.ai/desktop/cascade/mcp',
        getTabs: getDevinDesktopTabs,
        id: 'devin-desktop',
        logo: BotIcon,
        subtitle: 'Formerly Windsurf',
        title: 'Devin Desktop',
    },
    {
        docsUrl: 'https://modelcontextprotocol.io/clients',
        getTabs: getAnyMcpClientTabs,
        id: 'any-mcp-client',
        logo: PlugIcon,
        title: 'Any MCP client',
    },
];
