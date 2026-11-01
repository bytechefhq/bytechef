import {MCP_API_KEY_PLACEHOLDER, MCP_SERVER_NAME} from './mcpConnectAuthMode';

const VS_CODE_API_KEY_INPUT_ID = 'bytechef-api-key';

interface McpInstallLinkOptionsI {
    apiKeyHeader: boolean;
    url: string;
}

const encodeBase64 = (value: string): string => {
    const bytes = new TextEncoder().encode(value);

    return btoa(Array.from(bytes, (byte) => String.fromCharCode(byte)).join(''));
};

export const buildCursorInstallUrl = ({apiKeyHeader, url}: McpInstallLinkOptionsI): string => {
    const config = apiKeyHeader ? {headers: {Authorization: `Bearer ${MCP_API_KEY_PLACEHOLDER}`}, url} : {url};

    return `cursor://anysphere.cursor-deeplink/mcp/install?name=${encodeURIComponent(MCP_SERVER_NAME)}&config=${encodeURIComponent(encodeBase64(JSON.stringify(config)))}`;
};

export const buildVsCodeInstallUrl = ({apiKeyHeader, url}: McpInstallLinkOptionsI): string => {
    const config = apiKeyHeader
        ? {
              headers: {Authorization: `Bearer \${input:${VS_CODE_API_KEY_INPUT_ID}}`},
              inputs: [
                  {
                      description: `${MCP_SERVER_NAME} API key`,
                      id: VS_CODE_API_KEY_INPUT_ID,
                      password: true,
                      type: 'promptString',
                  },
              ],
              name: MCP_SERVER_NAME,
              type: 'http',
              url,
          }
        : {name: MCP_SERVER_NAME, type: 'http', url};

    return `vscode:mcp/install?${encodeURIComponent(JSON.stringify(config))}`;
};
