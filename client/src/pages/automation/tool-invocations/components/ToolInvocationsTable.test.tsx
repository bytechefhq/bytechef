import {render, screen} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

import ToolInvocationsTable from './ToolInvocationsTable';

const baseLog = {
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
    workspaceId: null,
};

describe('ToolInvocationsTable', () => {
    it('renders a row with the tool name, mapped surface label and outcome', () => {
        render(<ToolInvocationsTable toolInvocationLogs={[baseLog]} />);

        expect(screen.getByText('slack_sendMessage')).toBeInTheDocument();
        expect(screen.getByText('Automation MCP')).toBeInTheDocument();
        expect(screen.getByText('SUCCESS')).toBeInTheDocument();
        expect(screen.getByText('42 ms')).toBeInTheDocument();
        expect(screen.getByText('slack / sendMessage')).toBeInTheDocument();
    });

    it('renders an em dash when the tool name is missing', () => {
        render(<ToolInvocationsTable toolInvocationLogs={[{...baseLog, id: '2', toolName: null}]} />);

        expect(screen.getByText('—')).toBeInTheDocument();
    });

    it.each([
        ['EMBEDDED_API_ACTION', 'Embedded API (Action)'],
        ['EMBEDDED_API_TOOL', 'Embedded API (Tool)'],
        ['MCP_AUTOMATION', 'Automation MCP'],
        ['MCP_EMBEDDED', 'Embedded MCP'],
        ['MCP_MANAGEMENT', 'Management MCP'],
        ['FUTURE_SURFACE', 'FUTURE_SURFACE'],
    ])('labels the %s surface as %s', (surface, label) => {
        render(<ToolInvocationsTable toolInvocationLogs={[{...baseLog, surface}]} />);

        expect(screen.getByText(label)).toBeInTheDocument();
    });

    it.each([
        ['SUCCESS', 'bg-success'],
        ['ERROR', 'bg-destructive'],
        ['TIMEOUT', 'bg-destructive'],
        ['CONNECTION_REQUIRED', 'bg-surface-neutral-secondary'],
        ['APPROVAL_REQUIRED', 'bg-surface-neutral-secondary'],
        ['APPROVAL_DENIED', 'bg-surface-neutral-secondary'],
        ['SUSPENDED', 'bg-surface-neutral-secondary'],
    ])('colours the %s outcome with %s', (outcome, className) => {
        render(<ToolInvocationsTable toolInvocationLogs={[{...baseLog, outcome}]} />);

        expect(screen.getByText(outcome)).toHaveClass(className);
    });

    it('shows only the component name when the call has no operation', () => {
        render(<ToolInvocationsTable toolInvocationLogs={[{...baseLog, operationName: null}]} />);

        expect(screen.getByText('slack')).toBeInTheDocument();
        expect(screen.queryByText('slack / sendMessage')).not.toBeInTheDocument();
    });

    it('shows a dash in the component column for a workflow tool', () => {
        render(
            <ToolInvocationsTable
                toolInvocationLogs={[
                    {...baseLog, componentName: null, kind: 'WORKFLOW', operationName: null, toolName: 'runReport'},
                ]}
            />
        );

        expect(screen.getByText('runReport')).toBeInTheDocument();
        expect(screen.getByText('—')).toBeInTheDocument();
    });

    it('formats the created date and leaves the cell empty when it is missing', () => {
        const createdDate = Date.UTC(2026, 9, 31, 12, 0, 0);

        render(
            <ToolInvocationsTable
                toolInvocationLogs={[
                    {...baseLog, createdDate, id: '1'},
                    {...baseLog, createdDate: null, id: '2', toolName: 'undated_tool'},
                ]}
            />
        );

        expect(screen.getByText(new Date(createdDate).toLocaleString())).toBeInTheDocument();

        const undatedRow = screen.getByText('undated_tool').closest('tr');

        expect(undatedRow?.lastElementChild).toHaveTextContent(/^$/);
    });

    it('renders one row per invocation', () => {
        render(
            <ToolInvocationsTable
                toolInvocationLogs={[
                    {...baseLog, id: '1', toolName: 'first_tool'},
                    {...baseLog, id: '2', toolName: 'second_tool'},
                ]}
            />
        );

        expect(screen.getAllByRole('row')).toHaveLength(3);
        expect(screen.getByText('first_tool')).toBeInTheDocument();
        expect(screen.getByText('second_tool')).toBeInTheDocument();
    });
});
