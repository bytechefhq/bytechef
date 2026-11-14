import {McpServerToolsContentProps} from '@/shared/components/mcp-server/McpServerTabs';
import {McpServer} from '@/shared/middleware/graphql';
import {render, screen} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import McpServerToolsContent from './McpServerToolsContent';

const hoisted = vi.hoisted(() => ({
    useMcpAutomationProjectConfigurationList: vi.fn(),
    useMcpComponentList: vi.fn(),
    useMcpIntegrationInstanceConfigurationList: vi.fn(),
}));

vi.mock(
    '@/ee/pages/embedded/mcp-servers/components/mcp-integration-instance-configuration-list/hooks/useMcpIntegrationInstanceConfigurationList',
    () => ({default: hoisted.useMcpIntegrationInstanceConfigurationList})
);

vi.mock(
    '@/ee/pages/embedded/mcp-servers/components/mcp-automation-project-configuration-list/hooks/useMcpAutomationProjectConfigurationList',
    () => ({default: hoisted.useMcpAutomationProjectConfigurationList})
);

vi.mock('../mcp-automation-project-configuration-list/McpAutomationProjectConfigurationDialog', () => ({
    default: ({triggerNode}: {triggerNode?: ReactNode}) => triggerNode,
}));

vi.mock('@/ee/pages/embedded/mcp-servers/components/mcp-component-list/hooks/useMcpComponentList', () => ({
    default: hoisted.useMcpComponentList,
}));

vi.mock('@/shared/components/mcp-server/McpServerToolsPanel', () => ({
    default: ({
        isComponentListEmpty,
        isWorkflowListEmpty,
        workflowEmptyStateAction,
        workflowList,
    }: {
        isComponentListEmpty: boolean;
        isWorkflowListEmpty: boolean;
        workflowEmptyStateAction?: ReactNode;
        workflowList: ReactNode;
    }) => (
        <div
            data-component-list-empty={String(isComponentListEmpty)}
            data-testid="mcp-server-tools-panel"
            data-workflow-list-empty={String(isWorkflowListEmpty)}
        >
            {workflowList}

            {workflowEmptyStateAction}
        </div>
    ),
}));

vi.mock('../mcp-component-list/McpComponentList', () => ({
    default: () => null,
}));

vi.mock('../mcp-integration-instance-configuration-list/McpIntegrationInstanceConfigurationList', () => ({
    default: () => <div data-testid="integration-configurations-section" />,
}));

vi.mock('../mcp-automation-project-configuration-list/McpAutomationProjectConfigurationList', () => ({
    default: () => <div data-testid="automations-section" />,
}));

const mcpServer = {id: '5', name: 'Server'} as McpServer;

const renderToolsContent = () =>
    render(<McpServerToolsContent {...({mcpServer} as unknown as McpServerToolsContentProps)} />);

describe('McpServerToolsContent', () => {
    beforeEach(() => {
        hoisted.useMcpComponentList.mockReset();
        hoisted.useMcpIntegrationInstanceConfigurationList.mockReset();
        hoisted.useMcpAutomationProjectConfigurationList.mockReset();
        hoisted.useMcpIntegrationInstanceConfigurationList.mockReturnValue({
            isLoading: false,
            mcpIntegrationInstanceConfigurations: [{id: '1'}],
        });
        hoisted.useMcpAutomationProjectConfigurationList.mockReturnValue({
            isLoading: false,
            mcpAutomationProjectConfigurations: [],
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

    it('rendersAutomationsSection', () => {
        hoisted.useMcpComponentList.mockReturnValue({data: undefined, isMcpComponentsLoading: true});

        renderToolsContent();

        expect(screen.getByTestId('integration-configurations-section')).toBeInTheDocument();
        expect(screen.getByTestId('automations-section')).toBeInTheDocument();
    });

    it('showsWorkflowEmptyStateOnlyWhenBothListsAreEmptyAndKeepsAutomationsAddControl', () => {
        hoisted.useMcpComponentList.mockReturnValue({data: undefined, isMcpComponentsLoading: true});
        hoisted.useMcpIntegrationInstanceConfigurationList.mockReturnValue({
            isLoading: false,
            mcpIntegrationInstanceConfigurations: [],
        });

        renderToolsContent();

        expect(screen.getByTestId('mcp-server-tools-panel')).toHaveAttribute('data-workflow-list-empty', 'true');
        expect(screen.getByRole('button', {name: 'Add Automations'})).toBeInTheDocument();
    });

    it('doesNotShowWorkflowEmptyStateWhenAutomationsExist', () => {
        hoisted.useMcpComponentList.mockReturnValue({data: undefined, isMcpComponentsLoading: true});
        hoisted.useMcpIntegrationInstanceConfigurationList.mockReturnValue({
            isLoading: false,
            mcpIntegrationInstanceConfigurations: [],
        });
        hoisted.useMcpAutomationProjectConfigurationList.mockReturnValue({
            isLoading: false,
            mcpAutomationProjectConfigurations: [{id: 'm1'}],
        });

        renderToolsContent();

        expect(screen.getByTestId('mcp-server-tools-panel')).toHaveAttribute('data-workflow-list-empty', 'false');
    });
});
