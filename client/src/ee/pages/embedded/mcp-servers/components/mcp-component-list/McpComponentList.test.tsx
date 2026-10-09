import {McpServer} from '@/shared/middleware/graphql';
import {render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import McpComponentList from './McpComponentList';

const hoisted = vi.hoisted(() => ({
    useMcpComponentList: vi.fn(),
}));

vi.mock('@/ee/pages/embedded/mcp-servers/components/mcp-component-list/hooks/useMcpComponentList', () => ({
    default: hoisted.useMcpComponentList,
}));

vi.mock('@/ee/pages/embedded/mcp-servers/components/mcp-component-list/McpComponentListItem', () => ({
    default: ({mcpComponent}: {mcpComponent: {componentName: string}}) => (
        <div data-testid="mcp-component-list-item">{mcpComponent.componentName}</div>
    ),
}));

vi.mock('@/pages/platform/mcp-servers/components/McpToolListSkeleton', () => ({
    default: () => <div data-testid="mcp-tool-list-skeleton" />,
}));

const mcpServer = {id: '5', name: 'Server'} as McpServer;

describe('McpComponentList', () => {
    beforeEach(() => {
        hoisted.useMcpComponentList.mockReset();
    });

    it('lists the embedded components of the server sorted by name', () => {
        hoisted.useMcpComponentList.mockReturnValue({
            data: {
                embeddedMcpComponentsByServerId: [
                    {componentName: 'slack', id: '2'},
                    null,
                    {componentName: 'gmail', id: '1'},
                ],
            },
            isMcpComponentsLoading: false,
        });

        render(<McpComponentList mcpServer={mcpServer} />);

        expect(hoisted.useMcpComponentList).toHaveBeenCalledWith('5');
        expect(screen.getAllByTestId('mcp-component-list-item').map((item) => item.textContent)).toEqual([
            'gmail',
            'slack',
        ]);
    });

    it('shows the skeleton while the embedded components load', () => {
        hoisted.useMcpComponentList.mockReturnValue({data: undefined, isMcpComponentsLoading: true});

        render(<McpComponentList mcpServer={mcpServer} />);

        expect(screen.getByTestId('mcp-tool-list-skeleton')).toBeInTheDocument();
        expect(screen.queryByTestId('mcp-component-list-item')).not.toBeInTheDocument();
    });

    it('renders nothing when the server has no embedded components', () => {
        hoisted.useMcpComponentList.mockReturnValue({
            data: {embeddedMcpComponentsByServerId: []},
            isMcpComponentsLoading: false,
        });

        const {container} = render(<McpComponentList mcpServer={mcpServer} />);

        expect(container).toBeEmptyDOMElement();
    });
});
