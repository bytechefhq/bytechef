import {McpComponent, McpTool} from '@/shared/middleware/graphql';
import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import McpComponentToolList from './McpComponentToolList';

const hoisted = vi.hoisted(() => ({
    componentDefinition: undefined as Record<string, unknown> | undefined,
}));

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    useGetComponentDefinitionQuery: () => ({data: hoisted.componentDefinition}),
}));

vi.mock('../mcp-component-dialog/McpComponentDialog', () => ({
    default: () => null,
}));

vi.mock('./McpComponentToolListItem', () => ({
    default: ({connectionRequired}: {connectionRequired?: boolean}) => (
        <div data-connection-required={String(connectionRequired)} data-testid="tool-list-item" />
    ),
}));

const mcpTools = [{id: '42', name: 'createOpportunity', title: 'Create Opportunity'}] as Array<McpTool>;

const renderList = () =>
    render(
        <McpComponentToolList
            componentName="affinity"
            componentVersion={1}
            connectionId={null}
            mcpComponent={{id: '1'} as McpComponent}
            mcpServerId="1"
            mcpTools={mcpTools}
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
});
