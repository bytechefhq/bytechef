import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {McpProjectWorkflowItemType} from '../useMcpProjectList';
import useMcpProjectWorkflowPropertiesPopover from '../useMcpProjectWorkflowPropertiesPopover';

const hoisted = vi.hoisted(() => ({
    invalidateQueries: vi.fn(),
    propertiesQueryResult: {data: undefined, isLoading: false} as unknown,
    updateMutate: vi.fn(),
    updateOptions: {} as {onSuccess?: () => void},
    useMcpProjectWorkflowPropertiesQuery: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useMcpProjectWorkflowPropertiesQuery: (variables: unknown) => {
        hoisted.useMcpProjectWorkflowPropertiesQuery(variables);

        return hoisted.propertiesQueryResult;
    },
    useUpdateMcpProjectWorkflowMutation: (options: {onSuccess?: () => void}) => {
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
    ({id: 'mcp-workflow-1', parameters}) as unknown as McpProjectWorkflowItemType;

const renderPopover = (workflow: McpProjectWorkflowItemType, onClose: () => void) =>
    renderHook(() => useMcpProjectWorkflowPropertiesPopover(workflow, onClose));

describe('useMcpProjectWorkflowPropertiesPopover', () => {
    beforeEach(() => {
        hoisted.invalidateQueries.mockReset();
        hoisted.propertiesQueryResult = {
            data: {mcpProjectWorkflowProperties: properties},
            isLoading: false,
        };
        hoisted.updateMutate.mockReset();
        hoisted.useMcpProjectWorkflowPropertiesQuery.mockReset();
    });

    it('queries the properties of the MCP project workflow', () => {
        renderPopover(createWorkflow(), vi.fn());

        expect(hoisted.useMcpProjectWorkflowPropertiesQuery).toHaveBeenCalledWith({
            mcpProjectWorkflowId: 'mcp-workflow-1',
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
        const {result} = renderPopover(createWorkflow(), vi.fn());

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

    it.each([['not an object'], [['channel']], [null]])(
        'ignores saved parameters of the wrong shape %j',
        (parameters) => {
            const {result} = renderPopover(createWorkflow(parameters), vi.fn());

            expect(Object.keys(result.current.form.getValues()).sort()).toEqual([
                'channel',
                'dryRun',
                'limit',
                'options',
                'ratio',
                'tags',
            ]);
        }
    );

    it('resets the form once the properties arrive', () => {
        hoisted.propertiesQueryResult = {data: undefined, isLoading: true};

        const {rerender, result} = renderPopover(createWorkflow({limit: 3}), vi.fn());

        expect(result.current.form.getValues()).toEqual({limit: 3});

        hoisted.propertiesQueryResult = {data: {mcpProjectWorkflowProperties: properties}, isLoading: false};

        rerender();

        expect(result.current.form.getValues()).toEqual(expect.objectContaining({channel: 'general', limit: 3}));
    });

    it('submits the values with blank strings converted to null at every depth', () => {
        const {result} = renderPopover(createWorkflow(), vi.fn());

        act(() =>
            result.current.handleFormSubmit({
                channel: '',
                dryRun: false,
                items: [{name: ''}, '', 'kept'],
                limit: 0,
                nested: {deeper: {value: ''}, list: ['']},
                unset: null,
            })
        );

        expect(hoisted.updateMutate).toHaveBeenCalledWith({
            id: 'mcp-workflow-1',
            input: {
                parameters: {
                    channel: null,
                    dryRun: false,
                    items: [{name: null}, null, 'kept'],
                    limit: 0,
                    nested: {deeper: {value: null}, list: [null]},
                    unset: null,
                },
            },
        });
    });

    it('refreshes the MCP project list and closes after saving', () => {
        const onClose = vi.fn();

        renderPopover(createWorkflow(), onClose);

        hoisted.updateOptions.onSuccess?.();

        expect(hoisted.invalidateQueries).toHaveBeenCalledWith({queryKey: ['mcpProjectsByServerId']});
        expect(onClose).toHaveBeenCalledTimes(1);
    });
});
