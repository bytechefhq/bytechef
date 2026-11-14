import {describe, expect, it, vi} from 'vitest';
import {fireEvent, render, screen} from '@testing-library/react';
import ConnectDialog from './ConnectDialog';

describe('ConnectDialog — internalOnly inputs', () => {
    it('renders normal inputs and hides internalOnly inputs in the workflows view', () => {
        render(
            <ConnectDialog
                closeDialog={vi.fn()}
                handleClick={vi.fn()}
                handleWorkflowInputChange={vi.fn()}
                handleWorkflowToggle={vi.fn()}
                integration={{name: 'Test Integration'}}
                isOpen={true}
                mergedWorkflows={[
                    {
                        enabled: true,
                        inputs: [
                            {label: 'Channel', name: 'channel', type: 'string'},
                            {internalOnly: true, label: 'API Key', name: 'apiKey', type: 'string'},
                        ],
                        label: 'Test Workflow',
                        workflowUuid: 'workflow-abc',
                    },
                ]}
                workflowsView={true}
            />
        );

        expect(screen.getByLabelText('Channel')).toBeInTheDocument();
        expect(screen.queryByText('API Key')).not.toBeInTheDocument();
    });

    it('renders normal inputs and hides internalOnly inputs in the tools view', () => {
        render(
            <ConnectDialog
                closeDialog={vi.fn()}
                handleClick={vi.fn()}
                handleMcpWorkflowInputChange={vi.fn()}
                handleMcpWorkflowToggle={vi.fn()}
                handleWorkflowInputChange={vi.fn()}
                handleWorkflowToggle={vi.fn()}
                integration={{name: 'Test Integration'}}
                isOpen={true}
                mergedMcpWorkflows={[
                    {
                        enabled: true,
                        inputs: [
                            {label: 'Recipient', name: 'recipient', type: 'string'},
                            {internalOnly: true, label: 'Signing Secret', name: 'signingSecret', type: 'string'},
                        ],
                        label: 'Test Tool Workflow',
                        workflowUuid: 'mcp-workflow-abc',
                    },
                ]}
                mergedWorkflows={[{enabled: false, inputs: [], label: 'Test Workflow', workflowUuid: 'workflow-abc'}]}
                workflowsView={true}
            />
        );

        fireEvent.click(screen.getByRole('tab', {name: 'Tools'}));

        expect(screen.getByLabelText('Recipient')).toBeInTheDocument();
        expect(screen.queryByText('Signing Secret')).not.toBeInTheDocument();
    });
});
