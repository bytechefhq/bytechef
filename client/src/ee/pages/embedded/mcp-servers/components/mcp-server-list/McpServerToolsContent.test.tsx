import {McpServerToolsContentProps} from '@/shared/components/mcp-server/McpServerTabs';
import {McpServer} from '@/shared/middleware/graphql';
import {render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import McpServerToolsContent from './McpServerToolsContent';

const hoisted = vi.hoisted(() => ({
    useMcpComponentList: vi.fn(),
    useMcpIntegrationInstanceConfigurationList: vi.fn(),
}));

vi.mock('@/ee/pages/embedded/mcp-servers/components/mcp-component-list/hooks/useMcpComponentList', () => ({
    default: hoisted.useMcpComponentList,
}));

vi.mock(
    '@/ee/pages/embedded/mcp-servers/components/mcp-integration-instance-configuration-list/hooks/useMcpIntegrationInstanceConfigurationList',
    () => ({
        default: hoisted.useMcpIntegrationInstanceConfigurationList,
    })
);

vi.mock('@/shared/components/mcp-server/McpServerToolsPanel', () => ({
    default: ({
        isComponentListEmpty,
        isWorkflowListEmpty,
    }: {
        isComponentListEmpty: boolean;
        isWorkflowListEmpty: boolean;
    }) => (
        <div
            data-component-list-empty={String(isComponentListEmpty)}
            data-testid="mcp-server-tools-panel"
            data-workflow-list-empty={String(isWorkflowListEmpty)}
        />
    ),
}));

vi.mock('../mcp-component-list/McpComponentList', () => ({
    default: () => null,
}));

vi.mock('../mcp-integration-instance-configuration-list/McpIntegrationInstanceConfigurationList', () => ({
    default: () => null,
}));

const mcpServer = {id: '5', name: 'Server'} as McpServer;

const renderToolsContent = () =>
    render(<McpServerToolsContent {...({mcpServer} as unknown as McpServerToolsContentProps)} />);

describe('McpServerToolsContent', () => {
    beforeEach(() => {
        hoisted.useMcpComponentList.mockReset();
        hoisted.useMcpIntegrationInstanceConfigurationList.mockReset();
        hoisted.useMcpIntegrationInstanceConfigurationList.mockReturnValue({
            isLoading: false,
            mcpIntegrationInstanceConfigurations: [{id: '1'}],
        });
    });

    it('reports an empty component list once the server has no embedded components', () => {
        hoisted.useMcpComponentList.mockReturnValue({
            data: {embeddedMcpComponentsByServerId: []},
            isMcpComponentsLoading: false,
        });

        renderToolsContent();

        expect(hoisted.useMcpComponentList).toHaveBeenCalledWith('5');
        expect(screen.getByTestId('mcp-server-tools-panel')).toHaveAttribute('data-component-list-empty', 'true');
        expect(screen.getByTestId('mcp-server-tools-panel')).toHaveAttribute('data-workflow-list-empty', 'false');
    });

    it('does not report an empty component list when the server has embedded components', () => {
        hoisted.useMcpComponentList.mockReturnValue({
            data: {embeddedMcpComponentsByServerId: [{componentName: 'gmail', id: '1'}]},
            isMcpComponentsLoading: false,
        });

        renderToolsContent();

        expect(screen.getByTestId('mcp-server-tools-panel')).toHaveAttribute('data-component-list-empty', 'false');
    });

    it('does not report an empty component list while the embedded components load', () => {
        hoisted.useMcpComponentList.mockReturnValue({data: undefined, isMcpComponentsLoading: true});

        renderToolsContent();

        expect(screen.getByTestId('mcp-server-tools-panel')).toHaveAttribute('data-component-list-empty', 'false');
    });
});
