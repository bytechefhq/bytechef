import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {act, render, screen} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowExecutionSheetStore from '../../../stores/useWorkflowExecutionSheetStore';
import WorkflowExecutionSheetContent from '../WorkflowExecutionSheetContent';

const hoisted = vi.hoisted(() => ({
    grants: {} as Record<number, string[]>,
    taskExecutionQuery: vi.fn(),
    useRealScopeHook: false,
}));

vi.mock('@/shared/hooks/useHasEmbeddedScope', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/hooks/useHasEmbeddedScope')>();
    const {environmentStore: mockedEnvironmentStore} = await import('@/shared/stores/useEnvironmentStore');

    return {
        ...actual,
        useHasEmbeddedScope: (scope: string, environmentId?: number) =>
            hoisted.useRealScopeHook
                ? actual.useHasEmbeddedScope(scope as never, environmentId)
                : (
                      hoisted.grants[environmentId ?? mockedEnvironmentStore.getState().currentEnvironmentId] ?? []
                  ).includes(scope),
    };
});

vi.mock('@/ee/shared/queries/embedded/workflowExecutions.queries', () => ({
    useGetWorkflowExecutionTaskExecutionQuery: (...args: unknown[]) => hoisted.taskExecutionQuery(...args),
}));

vi.mock('@/components/ui/resizable', () => ({
    ResizableHandle: () => <div />,
    ResizablePanel: ({children}: {children: ReactNode}) => <div>{children}</div>,
    ResizablePanelGroup: ({children}: {children: ReactNode}) => <div>{children}</div>,
}));

vi.mock('@/components/ui/scroll-area', () => ({
    ScrollArea: ({children}: {children: ReactNode}) => <div>{children}</div>,
}));

vi.mock('@/shared/components/workflow-executions/WorkflowExecutionsHeader', () => ({default: () => <div />}));

vi.mock('@/shared/components/workflow-executions/WorkflowExecutionsAccordionItem', () => ({
    default: ({children}: {children: ReactNode}) => <div>{children}</div>,
}));

vi.mock('@/shared/components/workflow-executions/WorkflowTaskExecutionItem', () => ({default: () => <div />}));

vi.mock('@/shared/components/workflow-executions/WorkflowTriggerExecutionItem', () => ({default: () => <div />}));

vi.mock('@/shared/components/workflow-executions/WorkflowExecutionsTabsPanel', () => ({
    default: ({detailsHidden, selectedItemInput}: {detailsHidden?: boolean; selectedItemInput?: unknown}) => (
        <div data-testid="tabs-panel">
            {`detailsHidden=${String(detailsHidden)};input=${selectedItemInput ? 'loaded' : 'none'}`}
        </div>
    ),
}));

const DEVELOPMENT = 0;
const PRODUCTION = 2;

const job = {id: '1', status: 'COMPLETED', taskExecutions: [{id: '11', name: 'task'}]} as never;

const renderContent = () => render(<WorkflowExecutionSheetContent job={job} />);

const lastQueryEnabledArgument = () => {
    const calls = hoisted.taskExecutionQuery.mock.calls;

    return calls[calls.length - 1][1];
};

describe('embedded WorkflowExecutionSheetContent', () => {
    beforeEach(() => {
        hoisted.grants = {};
        hoisted.useRealScopeHook = false;

        hoisted.taskExecutionQuery.mockReset();
        hoisted.taskExecutionQuery.mockImplementation(() => ({
            data: {input: {a: 1}, output: {b: 2}},
            isLoading: false,
        }));

        act(() => {
            environmentStore.setState({currentEnvironmentId: DEVELOPMENT});
        });

        useWorkflowExecutionSheetStore.setState({workflowExecutionId: 1, workflowExecutionSheetOpen: true});
    });

    afterEach(() => {
        act(() => {
            authenticationStore.setState({account: undefined, authenticated: false});
            environmentStore.setState({currentEnvironmentId: DEVELOPMENT});
        });
    });

    it('testAViewerRunSheetHidesDetailsAndFetchesNoTaskData', () => {
        hoisted.grants = {
            [DEVELOPMENT]: ['EXECUTION_DETAILS_VIEW', 'EXECUTION_VIEW'],
            [PRODUCTION]: ['EXECUTION_VIEW'],
        };

        act(() => {
            environmentStore.setState({currentEnvironmentId: PRODUCTION});
        });

        renderContent();

        expect(screen.getByTestId('tabs-panel')).toHaveTextContent('detailsHidden=true');
        expect(lastQueryEnabledArgument()).toBe(false);
    });

    it('testAnEditorRunSheetShowsDetailsAndFetchesTaskData', () => {
        hoisted.grants = {[PRODUCTION]: ['EXECUTION_DETAILS_VIEW', 'EXECUTION_VIEW']};

        act(() => {
            environmentStore.setState({currentEnvironmentId: PRODUCTION});
        });

        renderContent();

        expect(screen.getByTestId('tabs-panel')).toHaveTextContent('detailsHidden=false;input=loaded');
        expect(lastQueryEnabledArgument()).toBe(true);
    });

    it('testATenantAdminSeesEveryControl', () => {
        hoisted.useRealScopeHook = true;

        act(() => {
            authenticationStore.setState({
                account: {authorities: ['ROLE_ADMIN'], login: 'admin'} as never,
                authenticated: true,
            });
        });

        renderContent();

        expect(screen.getByTestId('tabs-panel')).toHaveTextContent('detailsHidden=false');
        expect(lastQueryEnabledArgument()).toBe(true);
    });
});
