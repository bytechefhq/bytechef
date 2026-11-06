import useWorkflowExecutionSheetStore from '@/ee/pages/embedded/workflow-executions/stores/useWorkflowExecutionSheetStore';
import {useExternalInvocations} from '@/pages/automation/external-invocations/hooks/useExternalInvocations';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {act} from '@testing-library/react';
import {ReactNode} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {EmbeddedExternalInvocations} from './EmbeddedExternalInvocations';

const hoisted = vi.hoisted(() => ({
    useEmbeddedMcpServersQueryMock: vi.fn(),
    useExternalInvocationLogsQueryMock: vi.fn(),
    useWorkspaceExternalInvocationLogsQueryMock: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    PlatformType: {Automation: 'AUTOMATION', Embedded: 'EMBEDDED'},
    useEmbeddedMcpServersQuery: hoisted.useEmbeddedMcpServersQueryMock,
    useEnvironmentsQuery: () => ({data: undefined}),
    useExternalInvocationLogsQuery: hoisted.useExternalInvocationLogsQueryMock,
    useWorkspaceExternalInvocationLogsQuery: hoisted.useWorkspaceExternalInvocationLogsQueryMock,
}));

vi.mock('@/ee/pages/embedded/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionSheet', () => ({
    default: () => <div data-testid="workflow-execution-sheet" />,
}));

vi.mock('@/pages/automation/external-invocations/ExternalInvocations', () => ({
    ExternalInvocations: ({
        environmentId,
        firstRunEmptyState,
        onViewJob,
        type,
        workflowExecutionSheet,
        workspaceScope,
    }: {
        firstRunEmptyState: ReactNode;
        onViewJob: (jobId: number) => void;
        workflowExecutionSheet: ReactNode;
    } & Parameters<typeof useExternalInvocations>[0]) => {
        const {filterOutcome, filterPageNumber, handleOutcomeChange, handlePaginationClick} = useExternalInvocations({
            environmentId,
            type,
            workspaceScope,
        });

        return (
            <div>
                <span data-testid="filters">{`outcome=${filterOutcome ?? ''};page=${filterPageNumber}`}</span>

                <span data-testid="workspace-scope">{workspaceScope === undefined ? 'none' : 'scoped'}</span>

                <button onClick={() => handleOutcomeChange({label: 'Error', value: 'ERROR'})}>pick outcome</button>

                <button onClick={() => handlePaginationClick(3)}>next pages</button>

                {firstRunEmptyState}

                <button onClick={() => onViewJob(7)}>view job</button>

                {workflowExecutionSheet}
            </div>
        );
    },
}));

const emptyQueryResult = () => ({
    data: undefined,
    error: null,
    isFetching: false,
    isLoading: false,
    refetch: vi.fn(),
});

const renderPage = () =>
    render(
        <MemoryRouter>
            <EmbeddedExternalInvocations />
        </MemoryRouter>
    );

describe('EmbeddedExternalInvocations', () => {
    beforeEach(() => {
        hoisted.useEmbeddedMcpServersQueryMock.mockReset();
        hoisted.useEmbeddedMcpServersQueryMock.mockReturnValue({data: undefined});
        hoisted.useExternalInvocationLogsQueryMock.mockReset();
        hoisted.useExternalInvocationLogsQueryMock.mockReturnValue(emptyQueryResult());
        hoisted.useWorkspaceExternalInvocationLogsQueryMock.mockReset();
        hoisted.useWorkspaceExternalInvocationLogsQueryMock.mockReturnValue(emptyQueryResult());

        environmentStore.setState({currentEnvironmentId: 2});
    });

    it('reads the tenant-wide embedded log of the current environment without a workspace scope', () => {
        renderPage();

        expect(screen.getByTestId('workspace-scope')).toHaveTextContent('none');
        expect(hoisted.useExternalInvocationLogsQueryMock).toHaveBeenLastCalledWith(
            expect.objectContaining({environmentId: '2', page: 0, type: 'EMBEDDED'}),
            {enabled: true}
        );
    });

    it('resets the filters and the page and queries the new environment when the environment changes', async () => {
        renderPage();

        await userEvent.click(screen.getByRole('button', {name: 'pick outcome'}));
        await userEvent.click(screen.getByRole('button', {name: 'next pages'}));

        expect(screen.getByTestId('filters')).toHaveTextContent('outcome=ERROR;page=3');
        expect(hoisted.useExternalInvocationLogsQueryMock).toHaveBeenLastCalledWith(
            expect.objectContaining({environmentId: '2', outcome: 'ERROR', page: 3, type: 'EMBEDDED'}),
            {enabled: true}
        );

        act(() => {
            environmentStore.setState({currentEnvironmentId: 1});
        });

        expect(screen.getByTestId('filters')).toHaveTextContent('outcome=;page=0');
        expect(hoisted.useExternalInvocationLogsQueryMock).toHaveBeenLastCalledWith(
            expect.objectContaining({environmentId: '1', outcome: undefined, page: 0, type: 'EMBEDDED'}),
            {enabled: true}
        );
    });

    it('guides a new user to create an MCP server and mentions the Embedded API when the environment has none', () => {
        hoisted.useEmbeddedMcpServersQueryMock.mockReturnValue({
            data: {embeddedMcpServers: [{environmentId: '1', id: '11', name: 'Customer tools'}]},
        });

        renderPage();

        expect(screen.getByText(/through the Embedded API/)).toBeInTheDocument();
        expect(screen.getByText('Create an MCP server').closest('li')).toHaveAttribute('aria-current', 'step');
        expect(screen.getByText('Connect an AI agent').closest('li')).not.toHaveAttribute('aria-current');
    });

    it('moves the guide on to connecting an AI agent once the environment has an MCP server', () => {
        hoisted.useEmbeddedMcpServersQueryMock.mockReturnValue({
            data: {embeddedMcpServers: [{environmentId: '2', id: '11', name: 'Customer tools'}]},
        });

        renderPage();

        expect(screen.getByText('Completed:')).toBeInTheDocument();
        expect(screen.getByText('Connect an AI agent').closest('li')).toHaveAttribute('aria-current', 'step');
    });
    it('opens the workflow execution sheet of the job a call ran when View is clicked', async () => {
        renderPage();

        expect(screen.getByTestId('workflow-execution-sheet')).toBeInTheDocument();

        await userEvent.click(screen.getByText('view job'));

        expect(useWorkflowExecutionSheetStore.getState()).toMatchObject({
            workflowExecutionId: 7,
            workflowExecutionSheetOpen: true,
        });
    });
});
