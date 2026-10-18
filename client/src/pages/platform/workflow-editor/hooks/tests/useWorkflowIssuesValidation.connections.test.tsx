import {
    WorkflowTestConfigurationKeys,
    useGetWorkflowTestConfigurationQuery,
} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {renderHook, waitFor} from '@testing-library/react';
import {ReactNode} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowIssuesStore from '../../stores/useWorkflowIssuesStore';
import useWorkflowIssuesValidation from '../useWorkflowIssuesValidation';

const hoisted = vi.hoisted(() => ({
    workflowState: {workflow: {definition: '{"tasks":[{"name":"sendChannelMessage_1"}]}', id: 'wf-1'}},
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 0}),
}));

vi.mock('../../stores/useWorkflowDataStore', () => ({
    default: (selector: (state: typeof hoisted.workflowState) => unknown) => selector(hoisted.workflowState),
}));

const BOUND_CONNECTION = {connectionId: 1051, workflowConnectionKey: 'slack', workflowNodeName: 'sendChannelMessage_1'};

const MISSING_CONNECTION_ISSUE = {
    kind: 'MISSING_CONNECTION',
    message: 'Missing required connection: Slack',
    nodeName: 'sendChannelMessage_1',
    propertyPath: null,
    severity: 'ERROR',
};

function jsonResponse(body: unknown): Response {
    return new Response(JSON.stringify(body), {headers: {'Content-Type': 'application/json'}, status: 200});
}

function renderValidation(queryClient: QueryClient) {
    const wrapper = ({children}: {children: ReactNode}) => (
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    return renderHook(
        () => {
            const workflowTestConfigurationQuery = useGetWorkflowTestConfigurationQuery({
                environmentId: 0,
                workflowId: 'wf-1',
            });

            useWorkflowIssuesValidation();

            return workflowTestConfigurationQuery;
        },
        {wrapper}
    );
}

function getValidationRequestCount(fetchMock: ReturnType<typeof vi.fn>): number {
    return fetchMock.mock.calls.filter(([input]) => !String(input).includes('workflow-test-configurations')).length;
}

describe('useWorkflowIssuesValidation with workflow test configuration connections', () => {
    let connectionBound: boolean;
    let fetchMock: ReturnType<typeof vi.fn>;
    let queryClient: QueryClient;
    let testConfigurationExists: boolean;
    let testConfigurationResponseGate: Promise<void> | undefined;

    beforeEach(() => {
        connectionBound = false;
        testConfigurationExists = true;
        testConfigurationResponseGate = undefined;
        queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

        useWorkflowIssuesStore.getState().reset();

        fetchMock = vi.fn(async (input: RequestInfo | URL) => {
            const url = String(input);

            if (url.includes('workflow-test-configurations')) {
                await testConfigurationResponseGate;

                if (!testConfigurationExists && !connectionBound) {
                    return new Response('', {status: 200});
                }

                return jsonResponse({
                    connections: connectionBound ? [BOUND_CONNECTION] : [],
                    inputs: {},
                    workflowId: 'wf-1',
                });
            }

            return jsonResponse({
                data: {
                    validateWorkflow: {
                        errors: [],
                        nodeIssues: connectionBound ? [] : [MISSING_CONNECTION_ISSUE],
                        warnings: [],
                    },
                },
            });
        });

        vi.stubGlobal('fetch', fetchMock);
    });

    afterEach(() => {
        queryClient.clear();

        vi.unstubAllGlobals();
    });

    it('drops the missing connection issue once the refetched test configuration has the connection bound', async () => {
        renderValidation(queryClient);

        await waitFor(() =>
            expect(useWorkflowIssuesStore.getState().validatorIssues.map((issue) => issue.message)).toEqual([
                'Missing required connection: Slack',
            ])
        );

        await waitFor(() =>
            expect(
                queryClient.getQueryState(WorkflowTestConfigurationKeys.workflowTestConfiguration('wf-1'))?.status
            ).toBe('success')
        );

        connectionBound = true;

        await queryClient.invalidateQueries({queryKey: WorkflowTestConfigurationKeys.workflowTestConfigurations});

        await waitFor(() => expect(useWorkflowIssuesStore.getState().validatorIssues).toEqual([]));
    });

    it('validates once on load while the test configuration connections stay the same', async () => {
        renderValidation(queryClient);

        await waitFor(() =>
            expect(
                queryClient.getQueryState(WorkflowTestConfigurationKeys.workflowTestConfiguration('wf-1'))?.status
            ).toBe('success')
        );

        await queryClient.invalidateQueries({queryKey: WorkflowTestConfigurationKeys.workflowTestConfigurations});

        await waitFor(() =>
            expect(
                queryClient.getQueryState(WorkflowTestConfigurationKeys.workflowTestConfiguration('wf-1'))
                    ?.dataUpdateCount
            ).toBe(2)
        );

        const validationRequests = fetchMock.mock.calls.filter(
            ([input]) => !String(input).includes('workflow-test-configurations')
        );

        expect(validationRequests).toHaveLength(1);
    });

    it('drops the missing connection issue once the first connection is bound to a workflow without a test configuration', async () => {
        testConfigurationExists = false;

        renderValidation(queryClient);

        await waitFor(() =>
            expect(useWorkflowIssuesStore.getState().validatorIssues.map((issue) => issue.message)).toEqual([
                'Missing required connection: Slack',
            ])
        );

        await waitFor(() =>
            expect(
                queryClient.getQueryState(WorkflowTestConfigurationKeys.workflowTestConfiguration('wf-1'))?.fetchStatus
            ).toBe('idle')
        );

        connectionBound = true;

        await queryClient.invalidateQueries({queryKey: WorkflowTestConfigurationKeys.workflowTestConfigurations});

        await waitFor(() => expect(useWorkflowIssuesStore.getState().validatorIssues).toEqual([]));
    });

    it('revalidates when the first bound connection arrives through a refetch that passes through pending', async () => {
        testConfigurationExists = false;

        const {result} = renderValidation(queryClient);

        await waitFor(() =>
            expect(useWorkflowIssuesStore.getState().validatorIssues.map((issue) => issue.message)).toEqual([
                'Missing required connection: Slack',
            ])
        );

        await waitFor(() => expect(result.current.isError).toBe(true));

        const validationRequestCountBeforeBinding = getValidationRequestCount(fetchMock);

        let releaseTestConfigurationResponse: () => void = () => {};

        testConfigurationResponseGate = new Promise<void>((resolve) => {
            releaseTestConfigurationResponse = resolve;
        });

        connectionBound = true;

        const invalidation = queryClient.invalidateQueries({
            queryKey: WorkflowTestConfigurationKeys.workflowTestConfigurations,
        });

        await waitFor(() => expect(result.current.isPending).toBe(true));

        releaseTestConfigurationResponse();

        await invalidation;

        await waitFor(() => expect(result.current.data?.connections).toEqual([BOUND_CONNECTION]));

        await waitFor(() =>
            expect(getValidationRequestCount(fetchMock)).toBeGreaterThan(validationRequestCountBeforeBinding)
        );

        await waitFor(() => expect(useWorkflowIssuesStore.getState().validatorIssues).toEqual([]));
    });
});
