import {NodeDataType, PropertyAllType} from '@/shared/types';
import {render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import WorkflowNodeDetailsPanel from './WorkflowNodeDetailsPanel';

const {panelState, recordedPropertiesProps} = vi.hoisted(() => ({
    panelState: {currentNode: undefined as NodeDataType | undefined},
    recordedPropertiesProps: {value: undefined as {properties?: Array<PropertyAllType>} | undefined},
}));

const GRAPH_PROPERTIES = [
    {name: 'startNode', type: 'STRING'},
    {name: 'maxTransitions', type: 'INTEGER'},
    {name: 'transitions', type: 'ARRAY'},
] as Array<PropertyAllType>;

vi.mock('./hooks/useWorkflowNodeDetailsPanel', () => ({
    default: () => ({
        activeTab: 'properties',
        currentNode: panelState.currentNode,
        currentOperationProperties: GRAPH_PROPERTIES,
        currentWorkflowNode: {name: panelState.currentNode?.workflowNodeName},
        currentWorkflowNodeConnections: [],
        errors: [],
        getNodeVersion: () => '1',
        nodeTabs: [],
        workflow: {id: 'workflow-1'},
        workflowNodeDetailsPanelOpen: true,
    }),
}));

vi.mock('@/pages/platform/workflow-editor/components/properties/Properties', () => ({
    default: (propertiesProps: {properties?: Array<PropertyAllType>}) => {
        recordedPropertiesProps.value = propertiesProps;

        return <div data-testid="generic-properties" />;
    },
}));

vi.mock('@/pages/platform/workflow-editor/components/properties/graph/GraphTransitionsPanel', () => ({
    default: ({graphId}: {graphId: string}) => <div>{`transitions of ${graphId}`}</div>,
}));

vi.mock('@/shared/components/copilot/hooks/useCopilotLayoutShifted', () => ({default: () => false}));

function renderPanel(currentNode: NodeDataType) {
    panelState.currentNode = currentNode;

    return render(
        <WorkflowNodeDetailsPanel
            previousComponentDefinitions={[]}
            updateWorkflowMutation={{isPending: false, mutate: vi.fn()} as never}
            workflowNodeOutputs={[]}
        />
    );
}

describe('WorkflowNodeDetailsPanel graph transitions', () => {
    beforeEach(() => {
        recordedPropertiesProps.value = undefined;
    });

    it('replaces a graph node generic transitions array with the Transitions panel', () => {
        renderPanel({
            componentName: 'graph',
            name: 'graph_1',
            taskDispatcher: true,
            workflowNodeName: 'graph_1',
        } as NodeDataType);

        expect(screen.getByText('transitions of graph_1')).toBeInTheDocument();

        expect(recordedPropertiesProps.value?.properties?.map((property) => property.name)).toEqual([
            'startNode',
            'maxTransitions',
        ]);
    });

    it('leaves every property generic for a node that is not a graph', () => {
        renderPanel({
            componentName: 'loop',
            name: 'loop_1',
            taskDispatcher: true,
            workflowNodeName: 'loop_1',
        } as NodeDataType);

        expect(screen.queryByText(/^transitions of/)).not.toBeInTheDocument();

        expect(recordedPropertiesProps.value?.properties?.map((property) => property.name)).toEqual([
            'startNode',
            'maxTransitions',
            'transitions',
        ]);
    });
});
