import {useWorkflowExecutions} from '@/ee/pages/embedded/workflow-executions/hooks/useWorkflowExecutions';
import {
    WorkflowExecutionsSource,
    WorkflowExecutionsSourceType,
} from '@/ee/pages/embedded/workflow-executions/utils/workflowExecutionsSources';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {renderHook, waitFor} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import type {GetWorkflowExecutionsPageJobStatusEnum} from '@/ee/shared/middleware/embedded/workflow/execution';

const {
    automationGetWorkflowExecutionsPageMock,
    embeddedGetAnyWorkflowExecutionsPageMock,
    embeddedGetWorkflowExecutionsPageMock,
} = vi.hoisted(() => ({
    automationGetWorkflowExecutionsPageMock: vi.fn(),
    embeddedGetAnyWorkflowExecutionsPageMock: vi.fn(),
    embeddedGetWorkflowExecutionsPageMock: vi.fn(),
}));

vi.mock('@/shared/middleware/automation/workflow/execution', () => ({
    WorkflowExecutionApi: class {
        getWorkflowExecutionsPage = automationGetWorkflowExecutionsPageMock;
    },
}));

vi.mock('@/ee/shared/middleware/embedded/workflow/execution', () => ({
    WorkflowExecutionApi: class {
        getAnyWorkflowExecutionsPage = embeddedGetAnyWorkflowExecutionsPageMock;
        getWorkflowExecutionsPage = embeddedGetWorkflowExecutionsPageMock;
    },
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useConnectedUserProjectsQuery: () => ({data: undefined}),
}));

const createWrapper = () => {
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

    const Wrapper = ({children}: {children: ReactNode}) => (
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    return Wrapper;
};

describe('useWorkflowExecutions', () => {
    beforeEach(() => {
        automationGetWorkflowExecutionsPageMock.mockReset();
        automationGetWorkflowExecutionsPageMock.mockResolvedValue({content: []});
        embeddedGetAnyWorkflowExecutionsPageMock.mockReset();
        embeddedGetAnyWorkflowExecutionsPageMock.mockResolvedValue({content: []});
        embeddedGetWorkflowExecutionsPageMock.mockReset();
        embeddedGetWorkflowExecutionsPageMock.mockResolvedValue({content: []});
    });

    it('sends the selected connected user with the automation executions request', async () => {
        const initialProps: {connectedUserId?: number} = {};

        const {rerender, result} = renderHook(
            ({connectedUserId}: {connectedUserId?: number}) =>
                useWorkflowExecutions(WorkflowExecutionsSource.Automations, {connectedUserId, environmentId: 0, id: 1}),
            {initialProps, wrapper: createWrapper()}
        );

        await waitFor(() => {
            expect(result.current.workflowExecutionsIsLoading).toBe(false);
        });

        rerender({connectedUserId: 7});

        await waitFor(() => {
            expect(automationGetWorkflowExecutionsPageMock).toHaveBeenLastCalledWith(
                expect.objectContaining({connectedUserId: 7, embedded: true})
            );
        });

        await waitFor(() => {
            expect(result.current.workflowExecutionsIsFetching).toBe(false);
        });

        expect(embeddedGetWorkflowExecutionsPageMock).not.toHaveBeenCalled();
        expect(embeddedGetAnyWorkflowExecutionsPageMock).not.toHaveBeenCalled();
    });

    it('reads the executions of every source from one page for the any source', async () => {
        const jobStartDate = new Date(2026, 10, 1);

        const {result} = renderHook(
            () =>
                useWorkflowExecutions(WorkflowExecutionsSource.Any, {
                    connectedUserId: 7,
                    environmentId: 2,
                    id: 5,
                    integrationId: 3,
                    jobStartDate,
                    jobStatus: 'FAILED' as GetWorkflowExecutionsPageJobStatusEnum,
                    pageNumber: 1,
                    workflowId: 'workflow-uuid',
                }),
            {wrapper: createWrapper()}
        );

        await waitFor(() => {
            expect(result.current.workflowExecutionsIsFetching).toBe(false);
        });

        expect(embeddedGetAnyWorkflowExecutionsPageMock).toHaveBeenCalledWith({
            connectedUserId: 7,
            environmentId: 2,
            jobEndDate: undefined,
            jobStartDate,
            jobStatus: 'FAILED',
            pageNumber: 1,
            workspaceId: 5,
        });
        expect(automationGetWorkflowExecutionsPageMock).not.toHaveBeenCalled();
        expect(embeddedGetWorkflowExecutionsPageMock).not.toHaveBeenCalled();
    });

    it('drops the previous source page and reports loading while the next source loads', async () => {
        const integrationPage = {content: [{id: 1}]};
        let resolveAutomationPage: (page: object) => void = () => {};

        embeddedGetWorkflowExecutionsPageMock.mockResolvedValue(integrationPage);
        automationGetWorkflowExecutionsPageMock.mockImplementation(
            () =>
                new Promise((resolve) => {
                    resolveAutomationPage = resolve;
                })
        );

        const initialProps: {source: WorkflowExecutionsSourceType} = {source: WorkflowExecutionsSource.Integrations};

        const {rerender, result} = renderHook(
            ({source}: {source: WorkflowExecutionsSourceType}) =>
                useWorkflowExecutions(source, {environmentId: 0, id: 1}),
            {initialProps, wrapper: createWrapper()}
        );

        await waitFor(() => {
            expect(result.current.workflowExecutionPage).toEqual(integrationPage);
        });

        rerender({source: WorkflowExecutionsSource.Automations});

        await waitFor(() => {
            expect(automationGetWorkflowExecutionsPageMock).toHaveBeenCalled();
        });

        expect(result.current.workflowExecutionsIsLoading).toBe(true);
        expect(result.current.workflowExecutionPage).toBeUndefined();

        resolveAutomationPage({content: []});

        await waitFor(() => {
            expect(result.current.workflowExecutionsIsLoading).toBe(false);
        });

        expect(result.current.workflowExecutionPage).toEqual({content: []});
    });
});
