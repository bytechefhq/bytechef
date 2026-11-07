import {describe, expect, it} from 'vitest';

import {sourceHasA2aServer, sourceHasMcpServer} from '../sourceServers';

describe('sourceServers', () => {
    it.each([
        [undefined, true, true],
        ['A2A_SERVER', true, false],
        ['MCP_SERVER', false, true],
        ['API_ACTION', false, false],
        ['API_TOOL', false, false],
    ])('for source %s offers the A2A server filter %s and the MCP server filter %s', (source, a2a, mcp) => {
        expect(sourceHasA2aServer(source)).toBe(a2a);
        expect(sourceHasMcpServer(source)).toBe(mcp);
    });
});
