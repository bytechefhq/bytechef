import {McpServer} from '@/shared/middleware/graphql';
import {render} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import McpComponentList from './McpComponentList';

const hoisted = vi.hoisted(() => ({
    componentListResult: {data: {mcpComponentsByServerId: [] as unknown[]}, isMcpComponentsLoading: false},
}));

vi.mock('@/pages/automation/mcp-servers/components/mcp-component-list/hooks/useMcpComponentList', () => ({
    default: () => hoisted.componentListResult,
}));

vi.mock('@/pages/automation/mcp-servers/components/mcp-component-list/McpComponentListItem', () => ({
    default: ({mcpComponent}: {mcpComponent: {componentName: string}}) => <li>{mcpComponent.componentName}</li>,
}));

const mcpServer = {id: '1', name: 'mcpserver1'} as McpServer;

describe('McpComponentList', () => {
    beforeEach(() => {
        hoisted.componentListResult = {data: {mcpComponentsByServerId: []}, isMcpComponentsLoading: false};
    });

    // A server with workflows but no components used to show "No Components" alongside them. The combined
    // "No Tools" prompt in McpServerToolsContent covers the both-empty case, so this list stays silent.
    it('renders nothing when the server has no components', () => {
        const {container} = render(<McpComponentList mcpServer={mcpServer} />);

        expect(container).toBeEmptyDOMElement();
    });

    it('renders the components it has, sorted by name', () => {
        hoisted.componentListResult = {
            data: {
                mcpComponentsByServerId: [
                    {componentName: 'httpClient', id: '2'},
                    {componentName: 'activeCampaign', id: '1'},
                ],
            },
            isMcpComponentsLoading: false,
        };

        const {getAllByRole} = render(<McpComponentList mcpServer={mcpServer} />);

        expect(getAllByRole('listitem').map((item) => item.textContent)).toEqual(['activeCampaign', 'httpClient']);
    });

    it('still renders the loading skeleton while components are loading', () => {
        hoisted.componentListResult = {data: {mcpComponentsByServerId: []}, isMcpComponentsLoading: true};

        const {container} = render(<McpComponentList mcpServer={mcpServer} />);

        expect(container).not.toBeEmptyDOMElement();
    });
});
