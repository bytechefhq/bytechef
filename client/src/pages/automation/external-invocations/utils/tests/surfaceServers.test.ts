import {describe, expect, it} from 'vitest';

import {surfaceHasA2aServer, surfaceHasMcpServer} from '../surfaceServers';

describe('surfaceServers', () => {
    it.each([
        [undefined, true, true],
        ['A2A', true, false],
        ['MCP_AUTOMATION', false, true],
        ['MCP_EMBEDDED', false, true],
        ['EMBEDDED_API_ACTION', false, false],
        ['EMBEDDED_API_TOOL', false, false],
    ])('for surface %s offers the A2A server filter %s and the MCP server filter %s', (surface, a2a, mcp) => {
        expect(surfaceHasA2aServer(surface)).toBe(a2a);
        expect(surfaceHasMcpServer(surface)).toBe(mcp);
    });
});
