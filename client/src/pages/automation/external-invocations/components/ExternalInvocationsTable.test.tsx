import {formatDateTime} from '@/pages/automation/workflow-executions/utils/workflowExecutionsTable';
import {render as renderWithProviders} from '@/shared/util/test-utils';
import {render, screen} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

import ExternalInvocationsTable from './ExternalInvocationsTable';

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
    jobId: null,
    kind: 'COMPONENT',
    mcpServerId: null,
    operationName: 'sendMessage',
    outcome: 'SUCCESS',
    surface: 'MCP_AUTOMATION',
    toolName: 'slack_sendMessage',
    workflowName: null,
    workspaceId: null,
};

describe('ExternalInvocationsTable', () => {
    it('renders a row with the tool name, mapped surface label and outcome', () => {
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

    it('renders an em dash when the tool name is missing', () => {
        render(<ExternalInvocationsTable externalInvocationLogs={[{...baseLog, id: '2', toolName: null}]} />);

        const toolNameCell = screen.getAllByRole('cell')[1];

        expect(toolNameCell).toHaveTextContent(/^—$/);
    });

    it.each([
        ['A2A', 'A2A'],
        ['EMBEDDED_API_ACTION', 'Embedded API (Action)'],
        ['EMBEDDED_API_TOOL', 'Embedded API (Tool)'],
        ['MCP_AUTOMATION', 'MCP'],
        ['MCP_EMBEDDED', 'Embedded MCP'],
        ['FUTURE_SURFACE', 'FUTURE_SURFACE'],
    ])('labels the %s surface as %s', (surface, label) => {
        render(<ExternalInvocationsTable externalInvocationLogs={[{...baseLog, surface}]} />);

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
    ])('colours the %s outcome with %s', (outcome, className) => {
        render(<ExternalInvocationsTable externalInvocationLogs={[{...baseLog, outcome}]} />);

        expect(screen.getByText(outcome)).toHaveClass(className);
        expect(screen.getByText(outcome)).toHaveClass('border-transparent');
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
                        kind: 'WORKFLOW',
                        operationName: null,
                        toolName: 'send_message',
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
                    {...baseLog, componentName: null, kind: 'WORKFLOW', operationName: null, toolName: 'runReport'},
                ]}
            />
        );

        expect(screen.getByText('Workflow')).toBeInTheDocument();
    });

    it('shows the MCP server name and a dash when the server is missing or unknown', () => {
        render(
            <ExternalInvocationsTable
                externalInvocationLogs={[
                    {...baseLog, id: '1', mcpServerId: 5, toolName: 'named_server_tool'},
                    {...baseLog, id: '2', mcpServerId: 9, toolName: 'unknown_server_tool'},
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
                        kind: 'WORKFLOW',
                        operationName: null,
                        surface: 'A2A',
                        toolName: 'Summarize',
                    },
                ]}
                mcpServerNames={{'4': 'Sales MCP'}}
            />
        );

        expect(screen.getByText('Research agent')).toBeInTheDocument();
        expect(screen.queryByText('Sales MCP')).not.toBeInTheDocument();
    });

    it('hides the surface column when showSurface is false', () => {
        render(<ExternalInvocationsTable externalInvocationLogs={[baseLog]} showSurface={false} />);

        expect(screen.queryByText('Surface')).not.toBeInTheDocument();
        expect(screen.queryByText('MCP')).not.toBeInTheDocument();
        expect(screen.getByText('Source')).toBeInTheDocument();
        expect(screen.getByText('Server')).toBeInTheDocument();
    });

    it('formats the created date and leaves the cell empty when it is missing', () => {
        const createdDate = Date.UTC(2026, 9, 31, 12, 0, 0);

        render(
            <ExternalInvocationsTable
                externalInvocationLogs={[
                    {...baseLog, createdDate, id: '1'},
                    {...baseLog, createdDate: null, id: '2', toolName: 'undated_tool'},
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
                    {...baseLog, id: '1', toolName: 'first_tool'},
                    {...baseLog, id: '2', toolName: 'second_tool'},
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

        renderWithProviders(<ExternalInvocationsTable externalInvocationLogs={[baseLog]} onViewJob={() => {}} />);

        expect(screen.getAllByRole('columnheader').at(-1)).toHaveTextContent('Actions');
        expect(screen.getByRole('button', {name: 'External invocation actions'})).toBeInTheDocument();
    });
});
