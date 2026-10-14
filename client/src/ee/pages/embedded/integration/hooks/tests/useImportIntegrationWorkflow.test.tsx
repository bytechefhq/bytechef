import {IntegrationWorkflowKeys} from '@/ee/shared/queries/embedded/integrationWorkflows.queries';
import {IntegrationKeys} from '@/ee/shared/queries/embedded/integrations.queries';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, renderHook} from '@testing-library/react';
import {ChangeEvent, ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useImportIntegrationWorkflow} from '../useImportIntegrationWorkflow';

const hoisted = vi.hoisted(() => ({
    captureIntegrationWorkflowImported: vi.fn(),
    mutate: vi.fn(),
    mutationOptions: {current: undefined as {onSuccess?: () => void} | undefined},
    toast: vi.fn(),
}));

vi.mock('@/shared/hooks/useAnalytics', () => ({
    useAnalytics: () => ({captureIntegrationWorkflowImported: hoisted.captureIntegrationWorkflowImported}),
}));

vi.mock('@/ee/shared/mutations/embedded/workflows.mutations', () => ({
    useCreateIntegrationWorkflowMutation: (options: typeof hoisted.mutationOptions.current) => {
        hoisted.mutationOptions.current = options;

        return {mutate: hoisted.mutate};
    },
}));

vi.mock('sonner', () => ({toast: hoisted.toast}));

const renderUseImportIntegrationWorkflow = (integrationId = 5) => {
    const queryClient = new QueryClient();
    const invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries');

    const wrapper = ({children}: {children: ReactNode}) => (
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    const {result} = renderHook(() => useImportIntegrationWorkflow(integrationId), {wrapper});

    return {invalidateQueriesSpy, result};
};

const createFileChangeEvent = (files: File[]) => ({target: {files}}) as unknown as ChangeEvent<HTMLInputElement>;

describe('useImportIntegrationWorkflow', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        hoisted.mutationOptions.current = undefined;
    });

    it('imports the selected file as a workflow of the integration and clears the file input', async () => {
        const {result} = renderUseImportIntegrationWorkflow(7);

        const fileInput = document.createElement('input');

        Object.defineProperty(fileInput, 'value', {value: 'workflow.json', writable: true});

        (result.current.workflowFileInputRef as {current: HTMLInputElement | null}).current = fileInput;

        await act(async () => {
            await result.current.handleWorkflowFileChange(
                createFileChangeEvent([new File(['{"label": "Imported"}'], 'workflow.json')])
            );
        });

        expect(hoisted.mutate).toHaveBeenCalledWith({id: 7, workflow: {definition: '{"label": "Imported"}'}});
        expect(fileInput.value).toBe('');
    });

    it('does nothing when no file is selected', async () => {
        const {result} = renderUseImportIntegrationWorkflow();

        await act(async () => {
            await result.current.handleWorkflowFileChange(createFileChangeEvent([]));
        });

        expect(hoisted.mutate).not.toHaveBeenCalled();
    });

    it('refreshes the integration caches and confirms the import on success', () => {
        const {invalidateQueriesSpy} = renderUseImportIntegrationWorkflow(7);

        act(() => {
            hoisted.mutationOptions.current?.onSuccess?.();
        });

        expect(hoisted.captureIntegrationWorkflowImported).toHaveBeenCalled();
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: IntegrationKeys.integration(7)});
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({
            queryKey: IntegrationWorkflowKeys.integrationWorkflows(7),
        });
        expect(hoisted.toast).toHaveBeenCalledWith('Workflow is imported.');
    });
});
