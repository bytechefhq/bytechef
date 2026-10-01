import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {renderHook} from '@testing-library/react';
import {ReactNode} from 'react';
import {type MockInstance, afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {WorkflowIssueI} from '../../stores/useWorkflowIssuesStore';
import useRefreshAfterLookupFailure from './useRefreshAfterLookupFailure';

interface HookPropsI {
    nodeName: string | undefined;
    workflowIssues: Array<WorkflowIssueI>;
}

const CONNECTIONS_QUERY_KEY = ['automation_connections'];

const createLookupFailure = (nodeName: string, message = 'Unable to perform oauth token refresh'): WorkflowIssueI => ({
    kind: 'LOOKUP_FAILED',
    message,
    nodeName,
    propertyPath: 'spreadsheetId',
    severity: 'ERROR',
    source: 'LIVE',
});

const INITIAL_PROPS: HookPropsI = {nodeName: 'googleSheets_1', workflowIssues: []};

describe('useRefreshAfterLookupFailure', () => {
    let invalidateQueriesSpy: MockInstance<QueryClient['invalidateQueries']>;
    let queryClient: QueryClient;

    beforeEach(() => {
        queryClient = new QueryClient();

        invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries').mockResolvedValue();
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    const renderRefreshHook = (initialProps: HookPropsI = INITIAL_PROPS) =>
        renderHook(
            (props: HookPropsI) => useRefreshAfterLookupFailure({...props, connectionsQueryKey: CONNECTIONS_QUERY_KEY}),
            {
                initialProps,
                wrapper: ({children}: {children: ReactNode}) => (
                    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
                ),
            }
        );

    const getInvalidatedQueryKeys = () => invalidateQueriesSpy.mock.calls.map(([filters]) => filters?.queryKey);

    it('should not refetch anything while the node has no failed lookups', () => {
        renderRefreshHook();

        expect(invalidateQueriesSpy).not.toHaveBeenCalled();
    });

    it('should refetch the connections and the workflow validation when a lookup of the node fails', () => {
        const {rerender} = renderRefreshHook();

        rerender({...INITIAL_PROPS, workflowIssues: [createLookupFailure('googleSheets_1')]});

        expect(getInvalidatedQueryKeys()).toEqual([CONNECTIONS_QUERY_KEY, ['ValidateWorkflow']]);
    });

    it('should not refetch again while the same lookup failure stays recorded', () => {
        const workflowIssues = [createLookupFailure('googleSheets_1')];

        const {rerender} = renderRefreshHook({...INITIAL_PROPS, workflowIssues});

        rerender({...INITIAL_PROPS, workflowIssues: [...workflowIssues]});

        expect(getInvalidatedQueryKeys()).toEqual([CONNECTIONS_QUERY_KEY, ['ValidateWorkflow']]);
    });

    it('should refetch again when the lookup fails with a different message', () => {
        const {rerender} = renderRefreshHook({
            ...INITIAL_PROPS,
            workflowIssues: [createLookupFailure('googleSheets_1')],
        });

        rerender({...INITIAL_PROPS, workflowIssues: [createLookupFailure('googleSheets_1', 'Connection is invalid')]});

        expect(invalidateQueriesSpy).toHaveBeenCalledTimes(4);
    });

    it('should ignore lookup failures of other nodes', () => {
        const {rerender} = renderRefreshHook();

        rerender({...INITIAL_PROPS, workflowIssues: [createLookupFailure('slack_1')]});

        expect(invalidateQueriesSpy).not.toHaveBeenCalled();
    });
});
