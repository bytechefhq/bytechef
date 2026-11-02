import {McpTool} from '@/shared/middleware/graphql';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

import McpComponentToolList from '../McpComponentToolList';

const hoisted = vi.hoisted(() => ({
    componentDefinition: undefined as Record<string, unknown> | undefined,
}));

vi.mock('@/shared/hooks/useHasWorkspaceScope', () => ({
    useHasWorkspaceScope: (_workspaceId: number | undefined, scope: string) => scope === 'MCP_EDIT',
}));

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    useGetComponentDefinitionQuery: () => ({data: hoisted.componentDefinition}),
}));

vi.mock('../McpComponentToolListItem', () => ({
    default: ({
        connectionRequired,
        embedded,
        enabledSwitchVisible,
    }: {
        connectionRequired?: boolean;
        embedded?: boolean;
        enabledSwitchVisible?: boolean;
    }) => (
        <div
            data-connection-required={String(connectionRequired)}
            data-embedded={String(embedded)}
            data-enabled-switch-visible={String(enabledSwitchVisible)}
            data-testid="tool-list-item"
        />
    ),
}));

const mcpTools = [{id: '42', name: 'createOpportunity', title: 'Create Opportunity'}] as Array<McpTool>;

const renderList = ({embedded = false, tools = mcpTools}: {embedded?: boolean; tools?: Array<McpTool>} = {}) =>
    render(
        <McpComponentToolList
            componentName="affinity"
            componentVersion={1}
            connectionId={null}
            embedded={embedded}
            mcpTools={tools}
            renderEditDialog={(open) => <div data-open={String(open)} data-testid="edit-dialog" />}
        />
    );

describe('McpComponentToolList', () => {
    it('reads connectionRequired off the component definition and hands it to each tool item', () => {
        hoisted.componentDefinition = {clusterElements: [], connectionRequired: true};

        renderList();

        expect(screen.getByTestId('tool-list-item')).toHaveAttribute('data-connection-required', 'true');
    });

    it('reports a connection as required while the component definition is still loading', () => {
        hoisted.componentDefinition = undefined;

        renderList();

        expect(screen.getByTestId('tool-list-item')).toHaveAttribute('data-connection-required', 'true');
    });

    it('reports no connection requirement once a connection-less definition has loaded', () => {
        hoisted.componentDefinition = {clusterElements: [], connectionRequired: false};

        renderList();

        expect(screen.getByTestId('tool-list-item')).toHaveAttribute('data-connection-required', 'false');
    });

    it('shows the enabled switch on each tool in automation', () => {
        hoisted.componentDefinition = {clusterElements: [], connectionRequired: true};

        renderList();

        const toolListItem = screen.getByTestId('tool-list-item');

        expect(toolListItem).toHaveAttribute('data-embedded', 'false');
        expect(toolListItem).toHaveAttribute('data-enabled-switch-visible', 'true');
    });

    it('marks each tool as embedded instead of showing the automation switch', () => {
        hoisted.componentDefinition = {clusterElements: [], connectionRequired: true};

        renderList({embedded: true});

        const toolListItem = screen.getByTestId('tool-list-item');

        expect(toolListItem).toHaveAttribute('data-embedded', 'true');
        expect(toolListItem).toHaveAttribute('data-enabled-switch-visible', 'false');
    });

    it('opens the edit dialog from the empty state', async () => {
        hoisted.componentDefinition = {clusterElements: [], connectionRequired: true};

        renderList({tools: []});

        expect(screen.getByTestId('edit-dialog')).toHaveAttribute('data-open', 'false');

        await userEvent.click(screen.getByRole('button', {name: 'Edit Tools'}));

        expect(screen.getByTestId('edit-dialog')).toHaveAttribute('data-open', 'true');
    });
});
