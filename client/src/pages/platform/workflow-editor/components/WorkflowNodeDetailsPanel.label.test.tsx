import {NodeDataType} from '@/shared/types';
import {render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import WorkflowNodeDetailsPanel from './WorkflowNodeDetailsPanel';

const {panelState} = vi.hoisted(() => ({
    panelState: {label: undefined as string | undefined},
}));

vi.mock('./hooks/useWorkflowNodeDetailsPanel', () => ({
    default: () => ({
        activeTab: 'properties',
        currentNode: {
            clusterElementType: 'chatMemory',
            componentName: 'chatMemory',
            label: panelState.label,
            name: 'chatMemory_1',
            workflowNodeName: 'chatMemory_1',
        } as NodeDataType,
        currentOperationProperties: [],
        currentWorkflowNode: {name: 'chatMemory', title: 'Chat Memory'},
        currentWorkflowNodeConnections: [],
        errors: [],
        getNodeVersion: () => '2',
        nodeTabs: [],
        workflow: {id: 'workflow-1'},
        workflowNodeDetailsPanelOpen: true,
    }),
}));

vi.mock('@/pages/platform/workflow-editor/components/properties/Properties', () => ({
    default: () => <div data-testid="generic-properties" />,
}));

vi.mock('@/shared/components/copilot/hooks/useCopilotLayoutShifted', () => ({default: () => false}));

function renderPanel(label?: string) {
    panelState.label = label;

    return render(
        <WorkflowNodeDetailsPanel
            previousComponentDefinitions={[]}
            updateWorkflowMutation={{isPending: false, mutate: vi.fn()} as never}
            workflowNodeOutputs={[]}
        />
    );
}

describe('WorkflowNodeDetailsPanel header label', () => {
    beforeEach(() => {
        panelState.label = undefined;
    });

    it('shows the node label when the node has one', () => {
        renderPanel('Session Memory');

        expect(screen.getByText('Session Memory')).toBeInTheDocument();
    });

    it('falls back to the definition title when the node has no label', () => {
        renderPanel();

        expect(screen.getByText('Chat Memory')).toBeInTheDocument();
        expect(screen.getByText('(chatMemory_1)')).toBeInTheDocument();
    });
});
