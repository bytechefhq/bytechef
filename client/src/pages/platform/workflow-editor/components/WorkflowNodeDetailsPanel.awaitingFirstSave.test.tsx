import {NodeDataType, PropertyAllType} from '@/shared/types';
import {render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import WorkflowNodeDetailsPanel from './WorkflowNodeDetailsPanel';

const {panelState} = vi.hoisted(() => ({
    panelState: {awaitingFirstSave: false},
}));

const LOGGER_PROPERTIES = [{name: 'text', type: 'STRING'}] as Array<PropertyAllType>;

const LOGGER_NODE = {
    componentName: 'logger',
    name: 'logger_4',
    operationName: 'debug',
    workflowNodeName: 'logger_4',
} as NodeDataType;

vi.mock('./hooks/useWorkflowNodeDetailsPanel', () => ({
    default: () => ({
        activeTab: 'properties',
        awaitingFirstSave: panelState.awaitingFirstSave,
        currentNode: LOGGER_NODE,
        currentOperationProperties: LOGGER_PROPERTIES,
        currentWorkflowNode: {name: LOGGER_NODE.workflowNodeName},
        currentWorkflowNodeConnections: [],
        errors: [],
        getNodeVersion: () => '1',
        nodeTabs: [],
        workflow: {id: 'workflow-1'},
        workflowNodeDetailsPanelOpen: true,
    }),
}));

vi.mock('@/pages/platform/workflow-editor/components/properties/Properties', () => ({
    default: () => <div data-testid="generic-properties" />,
}));

vi.mock('@/shared/components/copilot/hooks/useCopilotLayoutShifted', () => ({default: () => false}));

function renderPanel(awaitingFirstSave: boolean) {
    panelState.awaitingFirstSave = awaitingFirstSave;

    return render(
        <WorkflowNodeDetailsPanel
            previousComponentDefinitions={[]}
            updateWorkflowMutation={{isPending: false, mutate: vi.fn()} as never}
            workflowNodeOutputs={[]}
        />
    );
}

describe('WorkflowNodeDetailsPanel properties tab while a node awaits its first save', () => {
    beforeEach(() => {
        panelState.awaitingFirstSave = false;
    });

    it('renders the properties once the node is persisted', () => {
        renderPanel(false);

        expect(screen.getByTestId('generic-properties')).toBeInTheDocument();
    });

    it('holds the properties back behind the skeleton until the first save lands', () => {
        // Every control on the properties tab talks to the server about the node by name: the
        // display-conditions query on mount, hidden-property default saves 200ms later, and the
        // parameter save behind each input. Before the add-node save lands they all 400 with
        // "Workflow node with name: logger_4 does not exist".
        renderPanel(true);

        expect(screen.queryByTestId('generic-properties')).not.toBeInTheDocument();
    });
});
