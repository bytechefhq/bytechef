import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {McpIntegrationInstanceConfigurationWorkflowItemType} from '../useMcpIntegrationInstanceConfigurationList';
import useMcpIntegrationInstanceConfigurationWorkflowPropertiesPopover from '../useMcpIntegrationInstanceConfigurationWorkflowPropertiesPopover';

const hoisted = vi.hoisted(() => ({
    invalidateQueries: vi.fn(),
    propertiesQueryResult: {data: undefined, isLoading: false} as unknown,
    updateMutate: vi.fn(),
    updateOptions: {} as {onSuccess?: () => void},
    usePropertiesQuery: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useMcpIntegrationInstanceConfigurationWorkflowPropertiesQuery: (variables: unknown) => {
        hoisted.usePropertiesQuery(variables);

        return hoisted.propertiesQueryResult;
    },
    useUpdateMcpIntegrationInstanceConfigurationWorkflowMutation: (options: {onSuccess?: () => void}) => {
        hoisted.updateOptions = options;

        return {isPending: false, mutate: hoisted.updateMutate};
    },
}));

vi.mock('@tanstack/react-query', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@tanstack/react-query')>()),
    useQueryClient: () => ({invalidateQueries: hoisted.invalidateQueries}),
}));

const properties = [
    {defaultValue: 'general', name: 'channel', type: 'STRING'},
    {integerDefaultValue: 10, name: 'limit', type: 'INTEGER'},
    {name: 'ratio', numberDefaultValue: 0.5, type: 'NUMBER'},
    {booleanDefaultValue: false, name: 'dryRun', type: 'BOOLEAN'},
    {arrayDefaultValue: ['a'], name: 'tags', type: 'ARRAY'},
    {name: 'options', objectDefaultValue: {retry: true}, type: 'OBJECT'},
    {defaultValue: null, name: 'noDefault', type: 'STRING'},
    {defaultValue: 'ignored', type: 'STRING'},
    null,
];

const createWorkflow = (parameters?: unknown) =>
    ({id: 'mcp-workflow-1', parameters}) as unknown as McpIntegrationInstanceConfigurationWorkflowItemType;

const renderPopover = (workflow: McpIntegrationInstanceConfigurationWorkflowItemType, onClose: () => void) =>
    renderHook(() => useMcpIntegrationInstanceConfigurationWorkflowPropertiesPopover(workflow, onClose));

describe('useMcpIntegrationInstanceConfigurationWorkflowPropertiesPopover', () => {
    beforeEach(() => {
        hoisted.invalidateQueries.mockReset();
        hoisted.propertiesQueryResult = {
            data: {mcpIntegrationInstanceConfigurationWorkflowProperties: properties},
            isLoading: false,
        };
        hoisted.updateMutate.mockReset();
        hoisted.usePropertiesQuery.mockReset();
    });

    it('queries the properties of the MCP workflow', () => {
        renderPopover(createWorkflow(), vi.fn());

        expect(hoisted.usePropertiesQuery).toHaveBeenCalledWith({
            mcpIntegrationInstanceConfigurationWorkflowId: 'mcp-workflow-1',
        });
    });

    it('drops empty property entries', () => {
        const {result} = renderPopover(createWorkflow(), vi.fn());

        expect(result.current.properties).toEqual(properties.slice(0, 8));
    });

    it('returns no properties while they are loading', () => {
        hoisted.propertiesQueryResult = {data: undefined, isLoading: true};

        const {result} = renderPopover(createWorkflow(), vi.fn());

        expect(result.current.properties).toEqual([]);
        expect(result.current.isLoading).toBe(true);
        expect(result.current.form.getValues()).toEqual({});
    });

    it('initializes the form with the typed default of each named property', () => {
        const {result} = renderPopover(createWorkflow(null), vi.fn());

        expect(result.current.form.getValues()).toEqual({
            channel: 'general',
            dryRun: false,
            limit: 10,
            options: {retry: true},
            ratio: 0.5,
            tags: ['a'],
        });
    });

    it('lets saved parameters override the property defaults', () => {
        const {result} = renderPopover(createWorkflow({channel: 'random', extra: 1}), vi.fn());

        expect(result.current.form.getValues()).toEqual(
            expect.objectContaining({channel: 'random', extra: 1, limit: 10})
        );
    });

    it('resets the form once the properties arrive', () => {
        hoisted.propertiesQueryResult = {data: undefined, isLoading: true};

        const {rerender, result} = renderPopover(createWorkflow({limit: 3}), vi.fn());

        expect(result.current.form.getValues()).toEqual({limit: 3});

        hoisted.propertiesQueryResult = {
            data: {mcpIntegrationInstanceConfigurationWorkflowProperties: properties},
            isLoading: false,
        };

        rerender();

        expect(result.current.form.getValues()).toEqual(expect.objectContaining({channel: 'general', limit: 3}));
    });

    it('saves only the fields the user changed on top of the saved parameters', () => {
        const {result} = renderPopover(createWorkflow({extra: 1}), vi.fn());

        act(() => result.current.form.setValue('channel', '', {shouldDirty: true}));
        act(() => result.current.form.setValue('nested', {deeper: {value: ''}}, {shouldDirty: true}));
        act(() => result.current.handleFormSubmit(result.current.form.getValues()));

        expect(hoisted.updateMutate).toHaveBeenCalledWith({
            id: 'mcp-workflow-1',
            input: {parameters: {channel: null, extra: 1, nested: {deeper: {value: null}}}},
        });
    });

    it('shows a saved null as an empty string and does not report it as changed', () => {
        const {result} = renderPopover(createWorkflow({channel: null, extra: 1}), vi.fn());

        expect(result.current.form.getValues('channel')).toBe('');

        act(() => result.current.form.setValue('channel', '', {shouldDirty: true}));

        expect(result.current.form.getFieldState('channel').isDirty).toBe(false);

        act(() => result.current.handleFormSubmit(result.current.form.getValues()));

        expect(hoisted.updateMutate).toHaveBeenCalledWith(
            expect.objectContaining({input: {parameters: {channel: null, extra: 1}}})
        );
    });

    it('does not save untouched built-in defaults', () => {
        const {result} = renderPopover(createWorkflow(), vi.fn());

        act(() => result.current.handleFormSubmit(result.current.form.getValues()));

        expect(hoisted.updateMutate).toHaveBeenCalledWith({id: 'mcp-workflow-1', input: {parameters: {}}});
    });

    it('saves the ai filled values', () => {
        const {result} = renderPopover(createWorkflow(), vi.fn());

        act(() => result.current.form.setValue('channel', '=fromAi(channel, "", "STRING")', {shouldDirty: true}));
        act(() => result.current.handleFormSubmit(result.current.form.getValues()));

        expect(hoisted.updateMutate).toHaveBeenCalledWith({
            id: 'mcp-workflow-1',
            input: {parameters: {channel: '=fromAi(channel, "", "STRING")'}},
        });
    });

    it('converts blank strings inside dirty array values to null', () => {
        const {result} = renderPopover(createWorkflow(), vi.fn());

        act(() => result.current.form.setValue('items', [{name: ''}, '', 'kept', ['']], {shouldDirty: true}));
        act(() => result.current.form.setValue('nested', {list: ['', 1]}, {shouldDirty: true}));
        act(() => result.current.handleFormSubmit(result.current.form.getValues()));

        expect(hoisted.updateMutate).toHaveBeenCalledWith({
            id: 'mcp-workflow-1',
            input: {
                parameters: {
                    items: [{name: null}, null, 'kept', [null]],
                    nested: {list: [null, 1]},
                },
            },
        });
    });

    it('refreshes the MCP server list and closes after saving', () => {
        const onClose = vi.fn();

        renderPopover(createWorkflow(), onClose);

        hoisted.updateOptions.onSuccess?.();

        expect(hoisted.invalidateQueries).toHaveBeenCalledWith({
            queryKey: ['mcpIntegrationInstanceConfigurationsByServerId'],
        });
        expect(onClose).toHaveBeenCalledTimes(1);
    });
});
