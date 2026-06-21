import {describe, expect, it, vi} from 'vitest';
import {render, screen} from '@testing-library/react';
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
});
