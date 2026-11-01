export type McpConnectAuthModeType = 'API_KEY' | 'API_KEY_OR_OAUTH' | 'URL_ONLY';

export const MCP_API_KEY_PLACEHOLDER = 'YOUR_API_KEY';

export const MCP_SERVER_NAME = 'ByteChef';

export const getMcpConnectAuthMode = ({
    authenticationRequired,
    oauthAvailable,
}: {
    authenticationRequired: boolean;
    oauthAvailable: boolean;
}): McpConnectAuthModeType => {
    if (!authenticationRequired) {
        return 'URL_ONLY';
    }

    return oauthAvailable ? 'API_KEY_OR_OAUTH' : 'API_KEY';
};
