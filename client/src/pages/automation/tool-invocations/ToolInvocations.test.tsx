import {ComboBoxItemType} from '@/components/ComboBox/ComboBox';
import {TooltipProvider} from '@/components/ui/tooltip';
import {PlatformType} from '@/shared/middleware/graphql';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {ToolInvocations} from './ToolInvocations';

const hoisted = vi.hoisted(() => ({
    useToolInvocationsMock: vi.fn(),
}));

vi.mock('@/pages/automation/tool-invocations/hooks/useToolInvocations', () => ({
    useToolInvocations: hoisted.useToolInvocationsMock,
}));

vi.mock('@/components/ComboBox/ComboBox', () => ({
    default: ({items}: {items: Array<ComboBoxItemType>}) => (
        <ul>
            {items.map((item) => (
                <li data-testid="combo-box-item" key={item.value}>
                    {item.value}
                </li>
            ))}
        </ul>
    ),
}));

const toolInvocationsState = () => ({
    filterPageNumber: 0,
    handleEndDateChange: vi.fn(),
    handleMcpServerChange: vi.fn(),
    handleOutcomeChange: vi.fn(),
    handlePaginationClick: vi.fn(),
    handleStartDateChange: vi.fn(),
    handleSurfaceChange: vi.fn(),
    refetchToolInvocations: vi.fn(),
    toolInvocationLogPage: undefined,
    toolInvocationLogs: [],
    toolInvocationLogsError: null,
    toolInvocationLogsIsFetching: false,
    toolInvocationLogsIsLoading: false,
});

const renderToolInvocations = (props: Partial<Parameters<typeof ToolInvocations>[0]> = {}) =>
    render(
        <MemoryRouter>
            <TooltipProvider>
                <ToolInvocations type={PlatformType.Automation} {...props} />
            </TooltipProvider>
        </MemoryRouter>
    );

const toolInvocationLog = {
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
};

const recordedToolInvocationsState = () => ({...toolInvocationsState(), toolInvocationLogs: [toolInvocationLog]});

const renderedItemValues = () => screen.getAllByTestId('combo-box-item').map((item) => item.textContent);

describe('ToolInvocations', () => {
    beforeEach(() => {
        hoisted.useToolInvocationsMock.mockReset();
        hoisted.useToolInvocationsMock.mockReturnValue(toolInvocationsState());
    });

    it('offers only the automation surfaces on the tenant-wide automation page', () => {
        hoisted.useToolInvocationsMock.mockReturnValue(recordedToolInvocationsState());

        renderToolInvocations();

        const itemValues = renderedItemValues();

        expect(itemValues).toEqual(expect.arrayContaining(['MCP_AUTOMATION', 'MCP_MANAGEMENT']));
        expect(itemValues).not.toContain('EMBEDDED_API_ACTION');
        expect(itemValues).not.toContain('EMBEDDED_API_TOOL');
        expect(itemValues).not.toContain('MCP_EMBEDDED');
    });

    it('offers the automation and management surfaces on the workspace-scoped page', () => {
        hoisted.useToolInvocationsMock.mockReturnValue(recordedToolInvocationsState());

        renderToolInvocations({workspaceScope: {environmentId: 2, workspaceId: 7}});

        const itemValues = renderedItemValues();

        expect(itemValues).toEqual(expect.arrayContaining(['MCP_AUTOMATION', 'MCP_MANAGEMENT']));
        expect(itemValues).not.toContain('EMBEDDED_API_ACTION');
        expect(itemValues).not.toContain('EMBEDDED_API_TOOL');
        expect(itemValues).not.toContain('MCP_EMBEDDED');
    });

    it('offers only the embedded surfaces on the embedded page', () => {
        hoisted.useToolInvocationsMock.mockReturnValue(recordedToolInvocationsState());

        renderToolInvocations({environmentId: 1, type: PlatformType.Embedded});

        const itemValues = renderedItemValues();

        expect(itemValues).toEqual(
            expect.arrayContaining(['EMBEDDED_API_ACTION', 'EMBEDDED_API_TOOL', 'MCP_EMBEDDED'])
        );
        expect(itemValues).not.toContain('MCP_AUTOMATION');
        expect(itemValues).not.toContain('MCP_MANAGEMENT');
    });

    it('offers a paused run as an outcome filter', () => {
        hoisted.useToolInvocationsMock.mockReturnValue(recordedToolInvocationsState());

        renderToolInvocations();

        expect(renderedItemValues()).toContain('SUSPENDED');
    });

    it('reads the invocations of the platform type and environment it is given', () => {
        renderToolInvocations({environmentId: 1, type: PlatformType.Embedded});

        expect(hoisted.useToolInvocationsMock).toHaveBeenCalledWith({
            environmentId: 1,
            type: PlatformType.Embedded,
            workspaceScope: undefined,
        });
    });

    it('shows the first-run empty state without filters or pagination when nothing has been recorded', () => {
        renderToolInvocations({workspaceScope: {environmentId: 2, workspaceId: 7}});

        expect(screen.getByText('No tool calls yet')).toBeInTheDocument();
        expect(screen.queryByText('Surface')).not.toBeInTheDocument();
        expect(screen.queryByRole('table')).not.toBeInTheDocument();
        expect(screen.queryByText(/Showing/)).not.toBeInTheDocument();
    });

    it('shows the first-run empty state it is given', () => {
        renderToolInvocations({firstRunEmptyState: <div>Custom guide</div>});

        expect(screen.getByText('Custom guide')).toBeInTheDocument();
        expect(screen.queryByText('No tool calls yet')).not.toBeInTheDocument();
    });

    it('keeps the filters and shows a no-match state when a filter hides every record', () => {
        hoisted.useToolInvocationsMock.mockReturnValue({...toolInvocationsState(), filterOutcome: 'ERROR'});

        renderToolInvocations({firstRunEmptyState: <div>Custom guide</div>});

        expect(screen.getByText('No Matching Tool Invocations')).toBeInTheDocument();
        expect(screen.getByText('Surface')).toBeInTheDocument();
        expect(screen.queryByText('Custom guide')).not.toBeInTheDocument();
    });

    it('lists the invocations with pagination and moves to the page that is clicked', async () => {
        const pagedToolInvocationsState = {
            ...hoisted.useToolInvocationsMock(),
            toolInvocationLogPage: {
                content: [toolInvocationLog],
                number: 0,
                size: 1,
                totalElements: 2,
                totalPages: 2,
            },
            toolInvocationLogs: [toolInvocationLog],
        };

        hoisted.useToolInvocationsMock.mockReturnValue(pagedToolInvocationsState);

        renderToolInvocations({workspaceScope: {environmentId: 2, workspaceId: 7}});

        expect(screen.getByText('slack_sendMessage')).toBeInTheDocument();
        expect(screen.queryByText('No tool calls yet')).not.toBeInTheDocument();
        expect(screen.getByText(/Showing/)).toHaveTextContent('Showing 1 to 1 of 2 results');

        await userEvent.click(screen.getByText('2'));

        expect(pagedToolInvocationsState.handlePaginationClick).toHaveBeenCalledWith(1);
    });

    it('refreshes the invocations from the header button', async () => {
        const currentToolInvocationsState = hoisted.useToolInvocationsMock();

        renderToolInvocations();

        const refreshButton = screen.getByRole('button', {name: 'Refresh tool invocations'});

        expect(refreshButton).toBeEnabled();

        await userEvent.click(refreshButton);

        expect(currentToolInvocationsState.refetchToolInvocations).toHaveBeenCalledTimes(1);
    });

    it('disables the refresh button while the invocations are being fetched', () => {
        hoisted.useToolInvocationsMock.mockReturnValue({...toolInvocationsState(), toolInvocationLogsIsFetching: true});

        renderToolInvocations();

        expect(screen.getByRole('button', {name: 'Refresh tool invocations'})).toBeDisabled();
    });

    it('shows an error instead of the list when the query fails', () => {
        hoisted.useToolInvocationsMock.mockReturnValue({
            ...toolInvocationsState(),
            toolInvocationLogsError: new Error('Access denied'),
        });

        renderToolInvocations();

        expect(screen.getByText('Some error occurred.')).toBeInTheDocument();
        expect(screen.queryByText('No tool calls yet')).not.toBeInTheDocument();
    });

    it('offers the MCP server filter only when the workspace has MCP servers', () => {
        hoisted.useToolInvocationsMock.mockReturnValue(recordedToolInvocationsState());

        const {unmount} = renderToolInvocations({mcpServerOptions: []});

        expect(screen.queryByText('MCP Server')).not.toBeInTheDocument();

        unmount();

        renderToolInvocations({mcpServerOptions: [{label: 'Sales tools', value: '11'}]});

        expect(screen.getByText('MCP Server')).toBeInTheDocument();
        expect(renderedItemValues()).toContain('11');
    });
});
