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
});
