import {ComponentDefinition, EmbeddedMcpToolsByComponentIdQuery} from '@/shared/middleware/graphql';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useMcpComponentDialogToolSelectionStep, {SelectedToolI} from '../useMcpComponentDialogToolSelectionStep';

const hoisted = vi.hoisted(() => ({
    useGetComponentDefinitionQuery: vi.fn(),
}));

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    useGetComponentDefinitionQuery: hoisted.useGetComponentDefinitionQuery,
}));

const clusterElements = [
    {
        componentName: 'gmail',
        componentVersion: 1,
        description: 'Send an email',
        name: 'sendEmail',
        title: 'Send Email',
        type: 'TOOLS',
    },
    {
        componentName: 'gmail',
        componentVersion: 1,
        description: 'Read emails',
        name: 'readEmails',
        title: 'Read Emails',
        type: 'TOOLS',
    },
    {componentName: 'gmail', componentVersion: 1, name: 'model', title: 'Model', type: 'MODEL'},
];

const sendEmailTool: SelectedToolI = {
    componentName: 'gmail',
    componentVersion: 1,
    description: 'Send an email',
    name: 'sendEmail',
    title: 'Send Email',
};

const readEmailsTool: SelectedToolI = {
    componentName: 'gmail',
    componentVersion: 1,
    description: 'Read emails',
    name: 'readEmails',
    title: 'Read Emails',
};

const selectedComponent = {name: 'gmail', version: 1} as ComponentDefinition;

const renderToolSelectionStep = ({
    existingTools,
    onToolsChange = vi.fn(),
    selectedTools = [],
}: {
    existingTools?: EmbeddedMcpToolsByComponentIdQuery;
    onToolsChange?: (tools: SelectedToolI[]) => void;
    selectedTools?: SelectedToolI[];
} = {}) =>
    renderHook(() =>
        useMcpComponentDialogToolSelectionStep({existingTools, onToolsChange, selectedComponent, selectedTools})
    );

describe('useMcpComponentDialogToolSelectionStep', () => {
    beforeEach(() => {
        hoisted.useGetComponentDefinitionQuery.mockReset();
        hoisted.useGetComponentDefinitionQuery.mockReturnValue({data: {clusterElements}, isLoading: false});
    });

    it('offers only the tool cluster elements of the selected component', () => {
        const {result} = renderToolSelectionStep();

        expect(result.current.toolElements.map((tool) => tool.name)).toEqual(['sendEmail', 'readEmails']);
        expect(hoisted.useGetComponentDefinitionQuery).toHaveBeenCalledWith(
            {componentName: 'gmail', componentVersion: 1},
            true
        );
    });

    it('pre-selects the tools already saved on the embedded MCP component', () => {
        const onToolsChange = vi.fn();

        renderToolSelectionStep({
            existingTools: {
                embeddedMcpToolsByComponentId: [{name: 'readEmails'}, {name: 'removedTool'}],
            } as EmbeddedMcpToolsByComponentIdQuery,
            onToolsChange,
        });

        expect(onToolsChange).toHaveBeenCalledWith([readEmailsTool]);
    });

    it('does not pre-select anything when the embedded MCP component has no saved tools', () => {
        const onToolsChange = vi.fn();

        renderToolSelectionStep({
            existingTools: {embeddedMcpToolsByComponentId: []} as unknown as EmbeddedMcpToolsByComponentIdQuery,
            onToolsChange,
        });

        expect(onToolsChange).not.toHaveBeenCalled();
    });

    it('does not pre-select anything until the component tools are loaded', () => {
        const onToolsChange = vi.fn();

        hoisted.useGetComponentDefinitionQuery.mockReturnValue({data: undefined, isLoading: true});

        const {result} = renderToolSelectionStep({
            existingTools: {
                embeddedMcpToolsByComponentId: [{name: 'readEmails'}],
            } as EmbeddedMcpToolsByComponentIdQuery,
            onToolsChange,
        });

        expect(result.current.isLoadingComponentDefinition).toBe(true);
        expect(onToolsChange).not.toHaveBeenCalled();
    });

    it('adds and removes a single tool', () => {
        const onToolsChange = vi.fn();

        const {result} = renderToolSelectionStep({onToolsChange, selectedTools: [sendEmailTool]});

        act(() => result.current.handleToolToggle(clusterElements[1] as never, true));

        expect(onToolsChange).toHaveBeenLastCalledWith([sendEmailTool, readEmailsTool]);

        act(() => result.current.handleToolToggle(clusterElements[0] as never, false));

        expect(onToolsChange).toHaveBeenLastCalledWith([]);
    });

    it('selects and clears all tools', () => {
        const onToolsChange = vi.fn();

        const {result} = renderToolSelectionStep({onToolsChange});

        act(() => result.current.handleSelectAllTools(true));

        expect(onToolsChange).toHaveBeenLastCalledWith([sendEmailTool, readEmailsTool]);

        act(() => result.current.handleSelectAllTools(false));

        expect(onToolsChange).toHaveBeenLastCalledWith([]);
    });

    it('reports whether some or all tools are selected', () => {
        const {result: someResult} = renderToolSelectionStep({selectedTools: [sendEmailTool]});

        expect(someResult.current.someToolsSelected).toBe(true);
        expect(someResult.current.allToolsSelected).toBe(false);

        const {result: allResult} = renderToolSelectionStep({selectedTools: [sendEmailTool, readEmailsTool]});

        expect(allResult.current.someToolsSelected).toBe(false);
        expect(allResult.current.allToolsSelected).toBe(true);
    });
});
