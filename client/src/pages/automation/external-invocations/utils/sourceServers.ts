export const sourceHasA2aServer = (source: string | undefined): boolean => !source || source === 'A2A_SERVER';

export const sourceHasMcpServer = (source: string | undefined): boolean => !source || source === 'MCP_SERVER';
