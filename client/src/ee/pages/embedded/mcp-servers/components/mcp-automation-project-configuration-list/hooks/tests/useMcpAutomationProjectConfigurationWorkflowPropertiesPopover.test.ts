import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {McpAutomationProjectConfigurationWorkflowItemType} from '../useMcpAutomationProjectConfigurationList';
import useMcpAutomationProjectConfigurationWorkflowPropertiesPopover from '../useMcpAutomationProjectConfigurationWorkflowPropertiesPopover';

const hoisted = vi.hoisted(() => ({
    invalidateQueries: vi.fn(),
    propertiesQueryResult: {data: undefined, isLoading: false} as unknown,
    updateMutate: vi.fn(),
    updateOptions: {} as {onSuccess?: () => void},
    usePropertiesQuery: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useMcpAutomationProjectConfigurationWorkflowPropertiesQuery: (variables: unknown) => {
        hoisted.usePropertiesQuery(variables);

        return hoisted.propertiesQueryResult;
    },
    useUpdateMcpAutomationProjectConfigurationWorkflowParametersMutation: (options: {onSuccess?: () => void}) => {
        hoisted.updateOptions = options;

        return {isPending: false, mutate: hoisted.updateMutate};
    },
}));

vi.mock('@tanstack/react-query', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@tanstack/react-query')>()),
    useQueryClient: () => ({invalidateQueries: hoisted.invalidateQueries}),
}));

const properties = [
    {name: 'toolName', type: 'STRING'},
    {name: 'toolDescription', type: 'STRING'},
    {name: 'customerId', type: 'STRING'},
    null,
];

const createWorkflow = (parameters?: unknown) =>
    ({id: 'link-workflow-1', parameters}) as unknown as McpAutomationProjectConfigurationWorkflowItemType;

describe('useMcpAutomationProjectConfigurationWorkflowPropertiesPopover', () => {
    beforeEach(() => {
        hoisted.invalidateQueries.mockReset();
        hoisted.propertiesQueryResult = {
            data: {mcpAutomationProjectConfigurationWorkflowProperties: properties},
            isLoading: false,
        };
        hoisted.updateMutate.mockReset();
        hoisted.usePropertiesQuery.mockReset();
    });

    it('listsThePropertiesOfTheLinkWorkflow', () => {
        const workflow = createWorkflow();

        const {result} = renderHook(() =>
            useMcpAutomationProjectConfigurationWorkflowPropertiesPopover(workflow, vi.fn())
        );

        expect(hoisted.usePropertiesQuery).toHaveBeenCalledWith({
            mcpAutomationProjectConfigurationWorkflowId: 'link-workflow-1',
        });
        expect(result.current.properties).toEqual(properties.slice(0, 3));
    });

    it('savingSubmitsFromAiMappingAndPreservedToolNameAndDescription', () => {
        const onClose = vi.fn();

        const workflow = createWorkflow({toolDescription: 'Does a thing', toolName: 'my_tool'});

        const {result} = renderHook(() =>
            useMcpAutomationProjectConfigurationWorkflowPropertiesPopover(workflow, onClose)
        );

        expect(result.current.form.getValues()).toEqual({toolDescription: 'Does a thing', toolName: 'my_tool'});

        act(() => {
            result.current.handleFormSubmit({
                customerId: 'fromAi(customerId, "The customer")',
                toolDescription: 'Does a thing',
                toolName: 'my_tool',
            });
        });

        expect(hoisted.updateMutate).toHaveBeenCalledWith({
            id: 'link-workflow-1',
            parameters: {
                customerId: 'fromAi(customerId, "The customer")',
                toolDescription: 'Does a thing',
                toolName: 'my_tool',
            },
        });

        act(() => {
            hoisted.updateOptions.onSuccess?.();
        });

        expect(hoisted.invalidateQueries).toHaveBeenCalledWith({
            queryKey: ['mcpAutomationProjectConfigurationsByServerId'],
        });
        expect(onClose).toHaveBeenCalled();
    });

    it('sanitizesEmptyStringsToNull', () => {
        const workflow = createWorkflow();

        const {result} = renderHook(() =>
            useMcpAutomationProjectConfigurationWorkflowPropertiesPopover(workflow, vi.fn())
        );

        act(() => {
            result.current.handleFormSubmit({toolDescription: '', toolName: 'x'});
        });

        expect(hoisted.updateMutate).toHaveBeenCalledWith({
            id: 'link-workflow-1',
            parameters: {toolDescription: null, toolName: 'x'},
        });
    });
});
