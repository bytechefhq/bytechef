import {ComboBoxItemType} from '@/components/ComboBox/ComboBox';
import {TooltipProvider} from '@/components/ui/tooltip';
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

const renderToolInvocations = (props: Parameters<typeof ToolInvocations>[0] = {}) =>
    render(
        <MemoryRouter>
            <TooltipProvider>
                <ToolInvocations {...props} />
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

const renderedItemValues = () => screen.getAllByTestId('combo-box-item').map((item) => item.textContent);

describe('ToolInvocations', () => {
    beforeEach(() => {
        hoisted.useToolInvocationsMock.mockReset();
        hoisted.useToolInvocationsMock.mockReturnValue(toolInvocationsState());
    });

    it('offers every surface on the tenant-wide page', () => {
        render(
            <MemoryRouter>
                <TooltipProvider>
                    <ToolInvocations />
                </TooltipProvider>
            </MemoryRouter>
        );

        expect(renderedItemValues()).toEqual(
            expect.arrayContaining([
                'EMBEDDED_API_ACTION',
                'EMBEDDED_API_TOOL',
                'MCP_AUTOMATION',
                'MCP_EMBEDDED',
                'MCP_MANAGEMENT',
            ])
        );
    });

    it('offers only the surfaces that can occur in a workspace on the workspace-scoped page', () => {
        render(
            <MemoryRouter>
                <TooltipProvider>
                    <ToolInvocations workspaceScope={{environmentId: 2, workspaceId: 7}} />
                </TooltipProvider>
            </MemoryRouter>
        );

        const itemValues = renderedItemValues();

        expect(itemValues).toContain('MCP_AUTOMATION');
        expect(itemValues).not.toContain('EMBEDDED_API_ACTION');
        expect(itemValues).not.toContain('EMBEDDED_API_TOOL');
        expect(itemValues).not.toContain('MCP_EMBEDDED');
        expect(itemValues).not.toContain('MCP_MANAGEMENT');
    });

    it('shows the empty state and no pagination when nothing has been recorded', () => {
        renderToolInvocations({workspaceScope: {environmentId: 2, workspaceId: 7}});

        expect(screen.getByText('No Tool Invocations')).toBeInTheDocument();
        expect(screen.queryByRole('table')).not.toBeInTheDocument();
        expect(screen.queryByText(/Showing/)).not.toBeInTheDocument();
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
        expect(screen.queryByText('No Tool Invocations')).not.toBeInTheDocument();
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
        expect(screen.queryByText('No Tool Invocations')).not.toBeInTheDocument();
    });

    it('offers the MCP server filter only when the workspace has MCP servers', () => {
        const {unmount} = renderToolInvocations({mcpServerOptions: []});

        expect(screen.queryByText('MCP Server')).not.toBeInTheDocument();

        unmount();

        renderToolInvocations({mcpServerOptions: [{label: 'Sales tools', value: '11'}]});

        expect(screen.getByText('MCP Server')).toBeInTheDocument();
        expect(renderedItemValues()).toContain('11');
    });
});
