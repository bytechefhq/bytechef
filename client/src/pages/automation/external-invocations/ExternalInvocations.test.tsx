import {ComboBoxItemType} from '@/components/ComboBox/ComboBox';
import {TooltipProvider} from '@/components/ui/tooltip';
import {PlatformType} from '@/shared/middleware/graphql';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {ExternalInvocations} from './ExternalInvocations';

const hoisted = vi.hoisted(() => ({
    useExternalInvocationsMock: vi.fn(),
}));

vi.mock('@/pages/automation/external-invocations/hooks/useExternalInvocations', () => ({
    useExternalInvocations: hoisted.useExternalInvocationsMock,
}));

vi.mock('@/components/ComboBox/ComboBox', () => ({
    default: ({'aria-label': ariaLabel, items}: {'aria-label'?: string; items: Array<ComboBoxItemType>}) => (
        <ul aria-label={ariaLabel}>
            {items.map((item) => (
                <li data-testid="combo-box-item" key={item.value}>
                    {item.value}
                </li>
            ))}
        </ul>
    ),
}));

const externalInvocationsState = () => ({
    externalInvocationLogPage: undefined,
    externalInvocationLogs: [],
    externalInvocationLogsError: null,
    externalInvocationLogsIsFetching: false,
    externalInvocationLogsIsLoading: false,
    filterPageNumber: 0,
    handleEndDateChange: vi.fn(),
    handleMcpServerChange: vi.fn(),
    handleOutcomeChange: vi.fn(),
    handlePaginationClick: vi.fn(),
    handleStartDateChange: vi.fn(),
    handleSurfaceChange: vi.fn(),
    refetchExternalInvocations: vi.fn(),
});

const renderExternalInvocations = (props: Partial<Parameters<typeof ExternalInvocations>[0]> = {}) =>
    render(
        <MemoryRouter>
            <TooltipProvider>
                <ExternalInvocations
                    firstRunEmptyState={<div>First-run guide</div>}
                    type={PlatformType.Automation}
                    {...props}
                />
            </TooltipProvider>
        </MemoryRouter>
    );

const externalInvocationLog = {
    a2aServerId: null,
    componentName: 'slack',
    componentVersion: 1,
    connectedUserId: null,
    connectionId: null,
    createdDate: 1_000,
    durationMs: 42,
    environment: 2,
    errorMessage: null,
    errorType: null,
    externalUserId: null,
    id: '1',
    integrationInstanceId: null,
    jobId: null,
    kind: 'COMPONENT',
    mcpServerId: 11,
    operationName: 'sendMessage',
    outcome: 'SUCCESS',
    surface: 'MCP_AUTOMATION',
    toolName: 'slack_sendMessage',
    workflowName: null,
};

const recordedExternalInvocationsState = () => ({
    ...externalInvocationsState(),
    externalInvocationLogs: [externalInvocationLog],
});

const renderedItemValues = () => screen.getAllByTestId('combo-box-item').map((item) => item.textContent);

describe('ExternalInvocations', () => {
    beforeEach(() => {
        hoisted.useExternalInvocationsMock.mockReset();
        hoisted.useExternalInvocationsMock.mockReturnValue(externalInvocationsState());
    });

    it('offers the automation MCP and A2A surfaces on the automation page', () => {
        hoisted.useExternalInvocationsMock.mockReturnValue(recordedExternalInvocationsState());

        renderExternalInvocations({workspaceScope: {environmentId: 2, workspaceId: 7}});

        const itemValues = renderedItemValues();

        expect(itemValues).toEqual(expect.arrayContaining(['A2A', 'MCP_AUTOMATION']));
        expect(itemValues).not.toContain('MCP_EMBEDDED');
        expect(screen.getByText('Surface', {selector: 'label'})).toBeInTheDocument();
        expect(screen.getByRole('cell', {name: 'MCP'})).toBeInTheDocument();
    });

    it('shows the name of the A2A server an A2A skill call came through', () => {
        hoisted.useExternalInvocationsMock.mockReturnValue({
            ...externalInvocationsState(),
            externalInvocationLogs: [
                {
                    ...externalInvocationLog,
                    a2aServerId: 4,
                    componentName: null,
                    kind: 'WORKFLOW',
                    mcpServerId: null,
                    operationName: null,
                    surface: 'A2A',
                    toolName: 'Summarize',
                },
            ],
        });

        renderExternalInvocations({a2aServerOptions: [{label: 'Research agent', value: '4'}]});

        expect(screen.getByRole('cell', {name: 'Research agent'})).toBeInTheDocument();
        expect(screen.getByRole('cell', {name: 'A2A'})).toBeInTheDocument();
    });

    it('shows the name of the MCP server each invocation came through', () => {
        hoisted.useExternalInvocationsMock.mockReturnValue(recordedExternalInvocationsState());

        renderExternalInvocations({mcpServerOptions: [{label: 'Sales MCP', value: '11'}]});

        expect(screen.getByRole('cell', {name: 'Sales MCP'})).toBeInTheDocument();
    });

    it('offers only the embedded surfaces on the embedded page', () => {
        hoisted.useExternalInvocationsMock.mockReturnValue(recordedExternalInvocationsState());

        renderExternalInvocations({environmentId: 1, type: PlatformType.Embedded});

        const itemValues = renderedItemValues();

        expect(itemValues).toEqual(
            expect.arrayContaining(['EMBEDDED_API_ACTION', 'EMBEDDED_API_TOOL', 'MCP_EMBEDDED'])
        );
        expect(itemValues).not.toContain('MCP_AUTOMATION');
    });

    it('offers a paused run as an outcome filter', () => {
        hoisted.useExternalInvocationsMock.mockReturnValue(recordedExternalInvocationsState());

        renderExternalInvocations();

        expect(renderedItemValues()).toContain('SUSPENDED');
    });

    it('reads the invocations of the platform type and environment it is given', () => {
        renderExternalInvocations({environmentId: 1, type: PlatformType.Embedded});

        expect(hoisted.useExternalInvocationsMock).toHaveBeenCalledWith({
            environmentId: 1,
            type: PlatformType.Embedded,
            workspaceScope: undefined,
        });
    });

    it('shows the first-run empty state without filters or pagination when nothing has been recorded', () => {
        renderExternalInvocations({workspaceScope: {environmentId: 2, workspaceId: 7}});

        expect(screen.getByText('First-run guide')).toBeInTheDocument();
        expect(screen.queryByText('Surface')).not.toBeInTheDocument();
        expect(screen.queryByRole('table')).not.toBeInTheDocument();
        expect(screen.queryByText(/Showing/)).not.toBeInTheDocument();
    });

    it('keeps the filters and shows a no-match state when a filter hides every record', () => {
        hoisted.useExternalInvocationsMock.mockReturnValue({...externalInvocationsState(), filterOutcome: 'ERROR'});

        renderExternalInvocations();

        expect(screen.getByText('No Matching External Invocations')).toBeInTheDocument();
        expect(screen.getByText('Status')).toBeInTheDocument();
        expect(screen.queryByText('First-run guide')).not.toBeInTheDocument();
    });

    it('lists the invocations with pagination and moves to the page that is clicked', async () => {
        const pagedExternalInvocationsState = {
            ...hoisted.useExternalInvocationsMock(),
            externalInvocationLogPage: {
                content: [externalInvocationLog],
                number: 0,
                size: 1,
                totalElements: 2,
                totalPages: 2,
            },
            externalInvocationLogs: [externalInvocationLog],
        };

        hoisted.useExternalInvocationsMock.mockReturnValue(pagedExternalInvocationsState);

        renderExternalInvocations({workspaceScope: {environmentId: 2, workspaceId: 7}});

        expect(screen.getByText('slack_sendMessage')).toBeInTheDocument();
        expect(screen.queryByText('First-run guide')).not.toBeInTheDocument();
        expect(screen.getByText(/Showing/)).toHaveTextContent('Showing 1 to 1 of 2 results');

        await userEvent.click(screen.getByText('2'));

        expect(pagedExternalInvocationsState.handlePaginationClick).toHaveBeenCalledWith(1);
    });

    it('refreshes the invocations from the header button', async () => {
        const currentExternalInvocationsState = hoisted.useExternalInvocationsMock();

        renderExternalInvocations();

        const refreshButton = screen.getByRole('button', {name: 'Refresh external invocations'});

        expect(refreshButton).toBeEnabled();

        await userEvent.click(refreshButton);

        expect(currentExternalInvocationsState.refetchExternalInvocations).toHaveBeenCalledTimes(1);
    });

    it('disables the refresh button while the invocations are being fetched', () => {
        hoisted.useExternalInvocationsMock.mockReturnValue({
            ...externalInvocationsState(),
            externalInvocationLogsIsFetching: true,
        });

        renderExternalInvocations();

        expect(screen.getByRole('button', {name: 'Refresh external invocations'})).toBeDisabled();
    });

    it('shows an error instead of the list when the query fails', () => {
        hoisted.useExternalInvocationsMock.mockReturnValue({
            ...externalInvocationsState(),
            externalInvocationLogsError: new Error('Access denied'),
        });

        renderExternalInvocations();

        expect(screen.getByText('Some error occurred.')).toBeInTheDocument();
        expect(screen.queryByText('First-run guide')).not.toBeInTheDocument();
    });

    it('offers the A2A server filter only when the workspace has A2A servers', () => {
        hoisted.useExternalInvocationsMock.mockReturnValue(recordedExternalInvocationsState());

        const {unmount} = renderExternalInvocations({a2aServerOptions: []});

        expect(screen.queryByText('A2A Server', {selector: 'label'})).not.toBeInTheDocument();

        unmount();

        renderExternalInvocations({a2aServerOptions: [{label: 'Research agent', value: '4'}]});

        expect(screen.getByText('A2A Server', {selector: 'label'})).toBeInTheDocument();
        expect(renderedItemValues()).toContain('4');
        expect(screen.getByRole('list', {name: 'A2A Server'})).toBeInTheDocument();
    });

    it('offers the MCP server filter only when the workspace has MCP servers', () => {
        hoisted.useExternalInvocationsMock.mockReturnValue(recordedExternalInvocationsState());

        const {unmount} = renderExternalInvocations({mcpServerOptions: []});

        expect(screen.queryByText('MCP Server', {selector: 'label'})).not.toBeInTheDocument();

        unmount();

        renderExternalInvocations({mcpServerOptions: [{label: 'Sales tools', value: '11'}]});

        expect(screen.getByText('MCP Server', {selector: 'label'})).toBeInTheDocument();
        expect(renderedItemValues()).toContain('11');
        expect(screen.getByRole('list', {name: 'MCP Server'})).toBeInTheDocument();
    });
});
