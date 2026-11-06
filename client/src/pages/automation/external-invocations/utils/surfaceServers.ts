export const surfaceHasA2aServer = (surface: string | undefined): boolean => !surface || surface === 'A2A';

export const surfaceHasMcpServer = (surface: string | undefined): boolean => !surface || surface.startsWith('MCP_');
