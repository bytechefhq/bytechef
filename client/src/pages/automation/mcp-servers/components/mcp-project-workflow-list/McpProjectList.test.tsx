import {McpServer} from '@/shared/middleware/graphql';
import {render} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import McpProjectList from './McpProjectList';

const hoisted = vi.hoisted(() => ({
    mcpProjectListResult: {isLoading: false, mcpProjects: [] as unknown[]},
}));

vi.mock('./hooks/useMcpProjectList', () => ({
    default: () => hoisted.mcpProjectListResult,
}));

const mcpServer = {id: '1', name: 'mcpserver1'} as McpServer;

describe('McpProjectList', () => {
    beforeEach(() => {
        hoisted.mcpProjectListResult = {isLoading: false, mcpProjects: []};
    });

    // A server with components but no workflows used to show "No MCP Projects" underneath its components.
    // The combined "No Tools" prompt in McpServerToolsContent covers the both-empty case, so this list stays silent.
    it('renders nothing when the server has no projects', () => {
        const {container} = render(<McpProjectList mcpServer={mcpServer} />);

        expect(container).toBeEmptyDOMElement();
    });

    it('still renders the loading skeleton while projects are loading', () => {
        hoisted.mcpProjectListResult = {isLoading: true, mcpProjects: []};

        const {container} = render(<McpProjectList mcpServer={mcpServer} />);

        expect(container).not.toBeEmptyDOMElement();
    });
});
