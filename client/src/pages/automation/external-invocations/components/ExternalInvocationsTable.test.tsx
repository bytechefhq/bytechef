import {TooltipProvider} from '@/components/ui/tooltip';
import {formatDateTime} from '@/pages/automation/workflow-executions/utils/workflowExecutionsTable';
import {render as renderWithQueryClient} from '@/shared/util/test-utils';
import {fireEvent, render as renderWithoutProviders, screen, within} from '@testing-library/react';
import {ReactElement} from 'react';
import {describe, expect, it, vi} from 'vitest';

import ExternalInvocationsTable from './ExternalInvocationsTable';

const render = (element: ReactElement) => renderWithoutProviders(<TooltipProvider>{element}</TooltipProvider>);

const renderWithProviders = (element: ReactElement) =>
    renderWithQueryClient(<TooltipProvider>{element}</TooltipProvider>);

const baseLog = {
    a2aServerId: null,
    componentName: 'slack',
    componentVersion: 1,
    connectedUserId: null,
    connectionId: null,
    createdDate: 1_000,
    durationMs: 42,
    environment: null,
    errorMessage: null,
    errorType: null,
    externalUserId: null,
    id: '1',
    integrationInstanceId: null,
    invocationName: 'slack_sendMessage',
    jobId: null,
    kind: 'COMPONENT',
    mcpServerId: null,
    operationName: 'sendMessage',
    source: 'MCP_SERVER',
    status: 'SUCCESS',
    workflowName: null,
    workspaceId: null,
};

describe('ExternalInvocationsTable', () => {
    it('renders a row with the tool name, mapped source label and status', () => {
        render(<ExternalInvocationsTable externalInvocationLogs={[baseLog]} />);

        expect(screen.getByText('slack_sendMessage')).toBeInTheDocument();
        expect(screen.getByText('MCP')).toBeInTheDocument();
        expect(screen.getByText('SUCCESS')).toBeInTheDocument();
        expect(screen.getByText('42ms')).toBeInTheDocument();
        expect(screen.getByText('slack / sendMessage')).toBeInTheDocument();
    });

    it('renders the status as the first column', () => {
        render(<ExternalInvocationsTable externalInvocationLogs={[baseLog]} />);

        expect(screen.getAllByRole('columnheader')[0]).toHaveTextContent('Status');
        expect(screen.getAllByRole('cell')[0]).toHaveTextContent('SUCCESS');
    });

    it.each([
        ['MCP_SERVER', 'COMPONENT', 'Component tool'],
        ['MCP_SERVER', 'WORKFLOW', 'Workflow tool'],
        ['API_TOOL', 'COMPONENT', 'Component tool'],
        ['API_ACTION', 'COMPONENT', 'Action'],
        ['A2A_SERVER', 'WORKFLOW', 'Skill'],
        ['MCP_SERVER', 'UNKNOWN_KIND', 'UNKNOWN_KIND'],
    ])('renders a %s %s call as %s in the Type column', (source, kind, expectedLabel) => {
        render(<ExternalInvocationsTable externalInvocationLogs={[{...baseLog, kind, source}]} />);

        expect(screen.getAllByRole('columnheader')[1]).toHaveTextContent('Name');
        expect(screen.getAllByRole('columnheader')[2]).toHaveTextContent('Type');

        const typeCell = screen.getAllByRole('cell')[2];

        if (kind === 'UNKNOWN_KIND') {
            expect(typeCell).toHaveTextContent(/^UNKNOWN_KIND$/);
        } else {
            expect(within(typeCell).getByRole('img', {name: expectedLabel})).toBeInTheDocument();
        }
    });

    it('renders an em dash when the invocation name is missing', () => {
        render(<ExternalInvocationsTable externalInvocationLogs={[{...baseLog, id: '2', invocationName: null}]} />);

        const invocationNameCell = screen.getAllByRole('cell')[1];

        expect(invocationNameCell).toHaveTextContent(/^—$/);
    });

    it.each([
        ['A2A_SERVER', 'A2A'],
        ['API_ACTION', 'API (Action)'],
        ['API_TOOL', 'API (Tool)'],
        ['MCP_SERVER', 'MCP'],
        ['FUTURE_SOURCE', 'FUTURE_SOURCE'],
    ])('labels the %s source as %s', (source, label) => {
        render(<ExternalInvocationsTable externalInvocationLogs={[{...baseLog, source}]} />);

        expect(screen.getByText(label)).toBeInTheDocument();
    });

    it.each([
        ['SUCCESS', 'text-content-success-primary'],
        ['ERROR', 'text-content-destructive-primary'],
        ['TIMEOUT', 'text-content-destructive-primary'],
        ['APPROVAL_DENIED', 'text-content-warning-primary'],
        ['RULE_BLOCKED', 'text-content-warning-primary'],
        ['CONNECTION_REQUIRED', 'bg-surface-neutral-secondary'],
        ['APPROVAL_REQUIRED', 'bg-surface-neutral-secondary'],
        ['SUSPENDED', 'bg-surface-neutral-secondary'],
    ])('colours the %s status with %s', (status, className) => {
        render(<ExternalInvocationsTable externalInvocationLogs={[{...baseLog, status}]} />);

        expect(screen.getByText(status)).toHaveClass(className);
        expect(screen.getByText(status)).toHaveClass('border-transparent');
    });

    it('shows only the component name when the call has no operation', () => {
        render(<ExternalInvocationsTable externalInvocationLogs={[{...baseLog, operationName: null}]} />);

        expect(screen.getByText('slack')).toBeInTheDocument();
        expect(screen.queryByText('slack / sendMessage')).not.toBeInTheDocument();
    });

    it('shows the workflow name in the source column for a workflow tool', () => {
        render(
            <ExternalInvocationsTable
                externalInvocationLogs={[
                    {
                        ...baseLog,
                        componentName: null,
                        invocationName: 'send_message',
                        kind: 'WORKFLOW',
                        operationName: null,
                        workflowName: 'Send Message',
                    },
                ]}
            />
        );

        expect(screen.getByText('send_message')).toBeInTheDocument();
        expect(screen.getByText('Send Message')).toBeInTheDocument();
    });

    it('falls back to Workflow in the source column when the workflow name cannot be resolved', () => {
        render(
            <ExternalInvocationsTable
                externalInvocationLogs={[
                    {
                        ...baseLog,
                        componentName: null,
                        invocationName: 'runReport',
                        kind: 'WORKFLOW',
                        operationName: null,
                    },
                ]}
            />
        );

        expect(screen.getAllByRole('cell')[5]).toHaveTextContent(/^Workflow$/);
    });

    it('shows the MCP server name and a dash when the server is missing or unknown', () => {
        render(
            <ExternalInvocationsTable
                externalInvocationLogs={[
                    {...baseLog, id: '1', invocationName: 'named_server_tool', mcpServerId: 5},
                    {...baseLog, id: '2', invocationName: 'unknown_server_tool', mcpServerId: 9},
                ]}
                mcpServerNames={{'5': 'Sales MCP'}}
            />
        );

        expect(screen.getByText('Sales MCP')).toBeInTheDocument();

        const unknownServerRow = screen.getByText('unknown_server_tool').closest('tr');

        expect(unknownServerRow).toHaveTextContent('—');
    });

    it('shows the A2A server name of an A2A skill call', () => {
        render(
            <ExternalInvocationsTable
                a2aServerNames={{'4': 'Research agent'}}
                externalInvocationLogs={[
                    {
                        ...baseLog,
                        a2aServerId: 4,
                        componentName: null,
                        invocationName: 'Summarize',
                        kind: 'WORKFLOW',
                        operationName: null,
                        source: 'A2A_SERVER',
                    },
                ]}
                mcpServerNames={{'4': 'Sales MCP'}}
            />
        );

        expect(screen.getByText('Research agent')).toBeInTheDocument();
        expect(screen.queryByText('Sales MCP')).not.toBeInTheDocument();
    });

    it('hands every clicked row to onSelect, including rows without a job', () => {
        const onSelect = vi.fn();

        render(<ExternalInvocationsTable externalInvocationLogs={[baseLog]} onSelect={onSelect} />);

        fireEvent.click(screen.getByText('slack_sendMessage'));

        expect(onSelect).toHaveBeenCalledWith(baseLog);
    });

    it('opens a focused row with the Enter and Space keys', () => {
        const onSelect = vi.fn();

        render(<ExternalInvocationsTable externalInvocationLogs={[baseLog]} onSelect={onSelect} />);

        const row = screen.getByText('slack_sendMessage').closest('tr')!;

        expect(row).toHaveAttribute('tabindex', '0');

        fireEvent.keyDown(row, {key: 'Enter'});
        fireEvent.keyDown(row, {key: ' '});
        fireEvent.keyDown(row, {key: 'a'});

        expect(onSelect).toHaveBeenCalledTimes(2);
    });

    it('does not make a row without an action focusable', () => {
        render(<ExternalInvocationsTable externalInvocationLogs={[baseLog]} />);

        expect(screen.getByText('slack_sendMessage').closest('tr')).not.toHaveAttribute('tabindex');
    });

    it('hides the source column when showSource is false', () => {
        render(<ExternalInvocationsTable externalInvocationLogs={[baseLog]} showSource={false} />);

        expect(screen.queryByText('Source')).not.toBeInTheDocument();
        expect(screen.queryByText('MCP')).not.toBeInTheDocument();
        expect(screen.getByText('Target')).toBeInTheDocument();
        expect(screen.getByText('Server')).toBeInTheDocument();
    });

    it('hides the connected user column unless the page asks for it', () => {
        render(<ExternalInvocationsTable externalInvocationLogs={[{...baseLog, externalUserId: '1234567890'}]} />);

        expect(screen.queryByRole('columnheader', {name: 'Connected User'})).not.toBeInTheDocument();
        expect(screen.queryByText('1234567890')).not.toBeInTheDocument();
    });

    it.each([
        ['the external id', {connectedUserId: '1050', externalUserId: '1234567890'}, '1234567890'],
        ['the connected user id without an external id', {connectedUserId: '1050', externalUserId: null}, '1050'],
        ['an em dash without a connected user', {connectedUserId: null, externalUserId: null}, '—'],
    ])('shows %s in the connected user column after the server', (_description, connectedUser, expectedText) => {
        render(
            <ExternalInvocationsTable externalInvocationLogs={[{...baseLog, ...connectedUser}]} showConnectedUser />
        );

        const headers = screen.getAllByRole('columnheader').map((header) => header.textContent);
        const connectedUserColumnIndex = headers.indexOf('Connected User');

        expect(connectedUserColumnIndex).toBe(headers.indexOf('Server') + 1);
        expect(screen.getAllByRole('cell')[connectedUserColumnIndex]).toHaveTextContent(expectedText);
    });

    it('formats the created date and leaves the cell empty when it is missing', () => {
        const createdDate = Date.UTC(2026, 9, 31, 12, 0, 0);

        render(
            <ExternalInvocationsTable
                externalInvocationLogs={[
                    {...baseLog, createdDate, id: '1'},
                    {...baseLog, createdDate: null, id: '2', invocationName: 'undated_tool'},
                ]}
            />
        );

        expect(screen.getByText(formatDateTime(new Date(createdDate)))).toBeInTheDocument();

        const undatedRow = screen.getByText('undated_tool').closest('tr');

        expect(undatedRow?.lastElementChild).toHaveTextContent(/^$/);
    });

    it('renders one row per invocation', () => {
        render(
            <ExternalInvocationsTable
                externalInvocationLogs={[
                    {...baseLog, id: '1', invocationName: 'first_tool'},
                    {...baseLog, id: '2', invocationName: 'second_tool'},
                ]}
            />
        );

        expect(screen.getAllByRole('row')).toHaveLength(3);
        expect(screen.getByText('first_tool')).toBeInTheDocument();
        expect(screen.getByText('second_tool')).toBeInTheDocument();
    });
    it('adds an Actions column only when the page can open a workflow run', () => {
        const {unmount} = render(<ExternalInvocationsTable externalInvocationLogs={[baseLog]} />);

        expect(screen.queryByText('Actions')).not.toBeInTheDocument();

        unmount();

        renderWithProviders(
            <ExternalInvocationsTable externalInvocationLogs={[{...baseLog, jobId: '7'}]} onViewJob={() => {}} />
        );

        expect(screen.getAllByRole('columnheader').at(-1)).toHaveTextContent('Actions');
        expect(screen.getByRole('button', {name: 'External invocation actions'})).toBeInTheDocument();
    });

    it('leaves the Actions cell empty for an invocation without a workflow run', () => {
        renderWithProviders(
            <ExternalInvocationsTable
                externalInvocationLogs={[baseLog, {...baseLog, id: '2', invocationName: 'run_workflow', jobId: '7'}]}
                onViewJob={() => {}}
            />
        );

        const rows = screen.getAllByRole('row');

        expect(rows[1].querySelectorAll('td')).toHaveLength(9);
        expect(screen.getAllByRole('button', {name: 'External invocation actions'})).toHaveLength(1);
        expect(rows[2]).toContainElement(screen.getByRole('button', {name: 'External invocation actions'}));
    });

    it('opens the workflow run when a row with a workflow run is clicked', () => {
        const onViewJob = vi.fn();

        renderWithProviders(
            <ExternalInvocationsTable
                externalInvocationLogs={[
                    {...baseLog, jobId: '10464', kind: 'WORKFLOW', operationName: null, workflowName: 'Send Message'},
                ]}
                onViewJob={onViewJob}
            />
        );

        fireEvent.click(screen.getByText('Send Message'));

        expect(onViewJob).toHaveBeenCalledWith(10464);
        expect(screen.getAllByRole('row')[1]).toHaveClass('cursor-pointer');
    });

    it('does not open anything when a row without a workflow run is clicked', () => {
        const onViewJob = vi.fn();

        renderWithProviders(<ExternalInvocationsTable externalInvocationLogs={[baseLog]} onViewJob={onViewJob} />);

        fireEvent.click(screen.getByText('slack / sendMessage'));

        expect(onViewJob).not.toHaveBeenCalled();
        expect(screen.getAllByRole('row')[1]).not.toHaveClass('cursor-pointer');
        expect(screen.queryByRole('button', {name: 'Send Message'})).not.toBeInTheDocument();
    });

    it('does not open the workflow run when the actions menu is clicked', () => {
        const onViewJob = vi.fn();

        renderWithProviders(
            <ExternalInvocationsTable externalInvocationLogs={[{...baseLog, jobId: '7'}]} onViewJob={onViewJob} />
        );

        fireEvent.click(screen.getByRole('button', {name: 'External invocation actions'}));

        expect(onViewJob).not.toHaveBeenCalled();
    });
});
