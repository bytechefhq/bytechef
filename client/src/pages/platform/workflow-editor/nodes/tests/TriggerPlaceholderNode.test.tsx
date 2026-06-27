import {TRIGGER_PLACEHOLDER_NODE_ID} from '@/shared/constants';
import {NodeDataType} from '@/shared/types';
import {render, screen} from '@testing-library/react';
import {ReactFlowProvider} from '@xyflow/react';
import {describe, expect, it, vi} from 'vitest';

import TriggerPlaceholderNode from '../TriggerPlaceholderNode';

vi.mock('../../components/WorkflowNodesPopoverMenu', () => ({
    default: ({children}: {children: React.ReactNode}) => <div data-testid="trigger-picker">{children}</div>,
}));

vi.mock('../../providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({updateWorkflowMutation: undefined}),
}));

describe('TriggerPlaceholderNode', () => {
    it('renders a + slot wrapped in the trigger picker', () => {
        render(
            <ReactFlowProvider>
                <TriggerPlaceholderNode data={{label: '+'} as NodeDataType} id={TRIGGER_PLACEHOLDER_NODE_ID} />
            </ReactFlowProvider>
        );

        expect(screen.getByTestId('trigger-picker')).toBeInTheDocument();
        expect(screen.getByTitle('Click to add a trigger')).toBeInTheDocument();
    });
});
