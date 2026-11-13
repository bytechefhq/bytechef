import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {renderHook, waitFor} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowTestConfigurationConnections from '../useWorkflowTestConfigurationConnections';

const hoisted = vi.hoisted(() => ({
    getWorkflowTestConfiguration: vi.fn(),
}));

vi.mock('@/shared/middleware/platform/configuration', () => ({
    WorkflowTestConfigurationApi: class {
        getWorkflowTestConfiguration = hoisted.getWorkflowTestConfiguration;
    },
}));

const createWrapper = () => {
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

    const Wrapper = ({children}: {children: ReactNode}) => (
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    return Wrapper;
};

describe('useWorkflowTestConfigurationConnections', () => {
    beforeEach(() => {
        hoisted.getWorkflowTestConfiguration.mockReset();
    });

    it('finds the editor connection by workflowUuid of the latest workflow', async () => {
        hoisted.getWorkflowTestConfiguration.mockImplementation(({workflowId}: {workflowId: string}) =>
            Promise.resolve(
                workflowId === 'draft-workflow-id'
                    ? {
                          connections: [
                              {connectionId: 42, workflowConnectionKey: 'googleMail', workflowNodeName: 'googleMail_1'},
                          ],
                          environmentId: 1,
                          workflowId,
                      }
                    : null
            )
        );

        const {result} = renderHook(
            () =>
                useWorkflowTestConfigurationConnections(
                    [{id: 'draft-workflow-id', workflowUuid: 'workflow-uuid'}],
                    1,
                    true
                ),
            {wrapper: createWrapper()}
        );

        await waitFor(() => expect(result.current.isFetched).toBe(true), {interval: 5, timeout: 5000});

        expect(hoisted.getWorkflowTestConfiguration).toHaveBeenCalledWith({
            environmentId: 1,
            workflowId: 'draft-workflow-id',
        });
        expect(result.current.findConnectionId('workflow-uuid', 'googleMail_1', 'googleMail')).toBe(42);
        expect(result.current.findConnectionId('workflow-uuid', 'googleMail_2', 'googleMail')).toBeUndefined();
        expect(result.current.findConnectionId('other-uuid', 'googleMail_1', 'googleMail')).toBeUndefined();
    });

    it('treats a workflow without a test configuration as fetched with no connections', async () => {
        hoisted.getWorkflowTestConfiguration.mockResolvedValue(null);

        const {result} = renderHook(
            () =>
                useWorkflowTestConfigurationConnections([{id: 'workflow-id', workflowUuid: 'workflow-uuid'}], 1, true),
            {wrapper: createWrapper()}
        );

        await waitFor(() => expect(result.current.isFetched).toBe(true), {interval: 5, timeout: 5000});

        expect(result.current.findConnectionId('workflow-uuid', 'googleMail_1', 'googleMail')).toBeUndefined();
    });

    it('does not fetch while disabled', () => {
        const {result} = renderHook(
            () =>
                useWorkflowTestConfigurationConnections([{id: 'workflow-id', workflowUuid: 'workflow-uuid'}], 1, false),
            {wrapper: createWrapper()}
        );

        expect(result.current.isFetched).toBe(false);
        expect(hoisted.getWorkflowTestConfiguration).not.toHaveBeenCalled();
    });
});
