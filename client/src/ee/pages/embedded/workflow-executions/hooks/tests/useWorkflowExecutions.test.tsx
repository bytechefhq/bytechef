import {useWorkflowExecutions} from '@/ee/pages/embedded/workflow-executions/hooks/useWorkflowExecutions';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {renderHook, waitFor} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {automationGetWorkflowExecutionsPageMock, embeddedGetWorkflowExecutionsPageMock} = vi.hoisted(() => ({
    automationGetWorkflowExecutionsPageMock: vi.fn(),
    embeddedGetWorkflowExecutionsPageMock: vi.fn(),
}));

vi.mock('@/shared/middleware/automation/workflow/execution', () => ({
    WorkflowExecutionApi: class {
        getWorkflowExecutionsPage = automationGetWorkflowExecutionsPageMock;
    },
}));

vi.mock('@/ee/shared/middleware/embedded/workflow/execution', () => ({
    WorkflowExecutionApi: class {
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
        embeddedGetWorkflowExecutionsPageMock.mockReset();
        embeddedGetWorkflowExecutionsPageMock.mockResolvedValue({content: []});
    });

    it('sends the selected connected user with the automation executions request', async () => {
        const initialProps: {connectedUserId?: number} = {};

        const {rerender, result} = renderHook(
            ({connectedUserId}: {connectedUserId?: number}) =>
                useWorkflowExecutions(1, {connectedUserId, environmentId: 0, id: 1}),
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
    });
});
