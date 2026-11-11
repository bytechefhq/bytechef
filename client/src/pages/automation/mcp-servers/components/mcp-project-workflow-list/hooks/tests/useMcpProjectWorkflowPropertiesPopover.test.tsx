import {TooltipProvider} from '@/components/ui/tooltip';
import McpWorkflowToolForm from '@/pages/platform/mcp-servers/components/McpWorkflowToolForm';
import {WorkflowReadOnlyProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {act, render, renderHook, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

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

    afterEach(() => {
        resetAll();
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

    it('does not save a nested built-in default that a field wrote while registering', async () => {
        windowResizeObserver();

        hoisted.propertiesQueryResult = {
            data: {
                mcpProjectWorkflowProperties: [
                    {
                        controlType: 'OBJECT_BUILDER',
                        name: 'body',
                        properties: [
                            {controlType: 'TEXT', defaultValue: 'JSON', name: 'format', type: 'STRING'},
                            {controlType: 'TEXT', name: 'content', type: 'STRING'},
                        ],
                        type: 'OBJECT',
                    },
                ],
            },
            isLoading: false,
        };

        let popoverResult: ReturnType<typeof useMcpProjectWorkflowPropertiesPopover> | undefined;

        const workflow = createWorkflow(null);

        const Harness = () => {
            const popover = useMcpProjectWorkflowPropertiesPopover(workflow, vi.fn());

            popoverResult = popover;

            return <McpWorkflowToolForm form={popover.form} properties={popover.properties} />;
        };

        render(
            <TooltipProvider>
                <WorkflowReadOnlyProvider value={{useGetComponentDefinitionsQuery: () => ({data: []})} as never}>
                    <Harness />
                </WorkflowReadOnlyProvider>
            </TooltipProvider>
        );

        expect(await screen.findByDisplayValue('JSON')).toBeInTheDocument();

        act(() => {
            popoverResult?.form.setValue('body.format', 'JSON' as never);
        });

        act(() => popoverResult?.handleFormSubmit(popoverResult.form.getValues()));

        expect(hoisted.updateMutate).toHaveBeenCalledWith({id: 'mcp-workflow-1', input: {parameters: {}}});
    });

    it('refreshes the MCP project list and closes after saving', () => {
        const onClose = vi.fn();

        renderPopover(createWorkflow(), onClose);

        hoisted.updateOptions.onSuccess?.();

        expect(hoisted.invalidateQueries).toHaveBeenCalledWith({queryKey: ['mcpProjectsByServerId']});
        expect(onClose).toHaveBeenCalledTimes(1);
    });
});
