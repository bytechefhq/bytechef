import {ProjectKeys} from '@/shared/queries/automation/projects.queries';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, renderHook, waitFor} from '@testing-library/react';
import {ChangeEvent, ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useImportProjectWorkflow} from '../useImportProjectWorkflow';

const hoisted = vi.hoisted(() => ({
    captureProjectWorkflowImported: vi.fn(),
    convertN8nWorkflow: vi.fn(),
    hasEnabledAiProvider: {hasEnabledAiProvider: true, isPending: false},
    mutate: vi.fn(),
    mutationOptions: {current: undefined as {onSuccess?: () => void} | undefined},
    toast: vi.fn(),
}));

vi.mock('@/shared/hooks/useAnalytics', () => ({
    useAnalytics: () => ({captureProjectWorkflowImported: hoisted.captureProjectWorkflowImported}),
}));

vi.mock('@/shared/hooks/useHasEnabledAiProvider', () => ({
    useHasEnabledAiProvider: () => hoisted.hasEnabledAiProvider,
}));

vi.mock('@/pages/automation/project/hooks/useConverterN8nToWorkflow', () => ({
    useConvertN8nToWorkflow: () => ({convertN8nWorkflow: hoisted.convertN8nWorkflow}),
}));

vi.mock('@/shared/mutations/automation/workflows.mutations', () => ({
    useCreateProjectWorkflowMutation: (options: typeof hoisted.mutationOptions.current) => {
        hoisted.mutationOptions.current = options;

        return {mutate: hoisted.mutate};
    },
}));

vi.mock('sonner', () => ({toast: hoisted.toast}));

const createFileChangeEvent = (files: File[]) => ({target: {files}}) as unknown as ChangeEvent<HTMLInputElement>;

const renderUseImportProjectWorkflow = () => {
    const queryClient = new QueryClient();
    const invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries');

    const wrapper = ({children}: {children: ReactNode}) => (
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    const {result} = renderHook(() => useImportProjectWorkflow(5), {wrapper});

    return {invalidateQueriesSpy, result};
};

describe('useImportProjectWorkflow', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        hoisted.hasEnabledAiProvider = {hasEnabledAiProvider: true, isPending: false};
        hoisted.mutationOptions.current = undefined;
    });

    it('disables the n8n import only once the AI provider check finds none enabled', () => {
        expect(renderUseImportProjectWorkflow().result.current.importN8nWorkflowDisabled).toBe(false);

        hoisted.hasEnabledAiProvider = {hasEnabledAiProvider: false, isPending: true};

        expect(renderUseImportProjectWorkflow().result.current.importN8nWorkflowDisabled).toBe(false);

        hoisted.hasEnabledAiProvider = {hasEnabledAiProvider: false, isPending: false};

        expect(renderUseImportProjectWorkflow().result.current.importN8nWorkflowDisabled).toBe(true);
    });

    it('imports the selected workflow file into the project', async () => {
        const {result} = renderUseImportProjectWorkflow();

        await act(() =>
            result.current.handleWorkflowFileChange(
                createFileChangeEvent([new File(['{"label":"Imported"}'], 'workflow.json')])
            )
        );

        expect(hoisted.mutate).toHaveBeenCalledWith({id: 5, workflow: {definition: '{"label":"Imported"}'}});
    });

    it('converts an n8n file before importing it and resets its input', async () => {
        hoisted.convertN8nWorkflow.mockResolvedValue('converted-definition');

        const {result} = renderUseImportProjectWorkflow();

        const n8nInput = document.createElement('input');

        n8nInput.type = 'file';

        result.current.n8nWorkflowFileInputRef.current = n8nInput;

        await act(() =>
            result.current.handleN8nWorkflowFileChange(createFileChangeEvent([new File(['{"nodes":[]}'], 'n8n.json')]))
        );

        expect(hoisted.convertN8nWorkflow).toHaveBeenCalledWith('{"nodes":[]}');
        expect(hoisted.mutate).toHaveBeenCalledWith({id: 5, workflow: {definition: 'converted-definition'}});
        expect(n8nInput.value).toBe('');
        expect(result.current.isImportingN8nWorkflow).toBe(false);
    });

    it('reports the n8n import as in progress until the conversion settles', async () => {
        let resolveConversion: (definition: string) => void = () => {};

        hoisted.convertN8nWorkflow.mockReturnValue(
            new Promise<string>((resolve) => {
                resolveConversion = resolve;
            })
        );

        const {result} = renderUseImportProjectWorkflow();

        let importPromise: Promise<void> = Promise.resolve();

        act(() => {
            importPromise = result.current.handleN8nWorkflowFileChange(
                createFileChangeEvent([new File(['{}'], 'n8n.json')])
            );
        });

        await waitFor(() => expect(result.current.isImportingN8nWorkflow).toBe(true));

        await act(async () => {
            resolveConversion('converted');

            await importPromise;
        });

        expect(result.current.isImportingN8nWorkflow).toBe(false);
    });

    it('ignores an n8n file change without files', async () => {
        const {result} = renderUseImportProjectWorkflow();

        await act(() => result.current.handleN8nWorkflowFileChange(createFileChangeEvent([])));

        expect(hoisted.convertN8nWorkflow).not.toHaveBeenCalled();
        expect(hoisted.mutate).not.toHaveBeenCalled();
    });

    it('refreshes the project, resets the workflow input and confirms on a successful import', () => {
        const {invalidateQueriesSpy, result} = renderUseImportProjectWorkflow();

        const workflowInput = document.createElement('input');

        workflowInput.type = 'file';

        result.current.workflowFileInputRef.current = workflowInput;

        hoisted.mutationOptions.current?.onSuccess?.();

        expect(hoisted.captureProjectWorkflowImported).toHaveBeenCalled();
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ProjectKeys.project(5)});
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ProjectKeys.projects});
        expect(workflowInput.value).toBe('');
        expect(hoisted.toast).toHaveBeenCalledWith('Workflow is imported.');
    });
});
