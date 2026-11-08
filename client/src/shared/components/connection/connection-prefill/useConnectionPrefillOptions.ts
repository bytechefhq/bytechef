import {MCP_CLIENT_COMPONENT_NAME} from './mcp-client/mcpServerCatalogPrefill';
import useMcpServerCatalogPrefillOptions from './mcp-client/useMcpServerCatalogPrefillOptions';
import {ConnectionPrefillOptionI} from './types';

export default function useConnectionPrefillOptions(
    componentName: string | undefined,
    enabled: boolean
): ConnectionPrefillOptionI[] {
    return useMcpServerCatalogPrefillOptions(enabled && componentName === MCP_CLIENT_COMPONENT_NAME);
}
