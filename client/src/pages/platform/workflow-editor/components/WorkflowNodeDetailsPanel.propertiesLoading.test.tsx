import {NodeDataType, PropertyAllType} from '@/shared/types';
import {render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import WorkflowNodeDetailsPanel from './WorkflowNodeDetailsPanel';

const {panelState} = vi.hoisted(() => ({
    panelState: {propertiesLoading: false},
}));

const MISTRAL_PROPERTIES = [
    {name: 'model', type: 'STRING'},
    {displayCondition: "type == 'image_url'", name: 'url', type: 'STRING'},
] as Array<PropertyAllType>;

const MISTRAL_NODE = {
    componentName: 'mistral',
    name: 'mistral_1',
    operationName: 'ocr',
    type: 'mistral/v1/ocr',
    workflowNodeName: 'mistral_1',
} as NodeDataType;

vi.mock('./hooks/useWorkflowNodeDetailsPanel', () => ({
    default: () => ({
        activeTab: 'properties',
        awaitingFirstSave: false,
        currentNode: MISTRAL_NODE,
        currentOperationProperties: MISTRAL_PROPERTIES,
        currentWorkflowNode: {name: MISTRAL_NODE.workflowNodeName},
        currentWorkflowNodeConnections: [],
        errors: [],
        getNodeVersion: () => '1',
        nodeTabs: [],
        propertiesLoading: panelState.propertiesLoading,
        workflow: {id: 'workflow-1'},
        workflowNodeDetailsPanelOpen: true,
    }),
}));

vi.mock('./WorkflowEditorSkeletons', async (importOriginal) => ({
    ...(await importOriginal<typeof import('./WorkflowEditorSkeletons')>()),
    PropertiesTabSkeleton: () => <div data-testid="properties-tab-skeleton" />,
}));

vi.mock('@/pages/platform/workflow-editor/components/properties/Properties', () => ({
    default: () => <div data-testid="generic-properties" />,
}));

vi.mock('@/shared/components/copilot/hooks/useCopilotLayoutShifted', () => ({default: () => false}));

function renderPanel(propertiesLoading: boolean) {
    panelState.propertiesLoading = propertiesLoading;

    return render(
        <WorkflowNodeDetailsPanel
            previousComponentDefinitions={[]}
            updateWorkflowMutation={{isPending: false, mutate: vi.fn()} as never}
            workflowNodeOutputs={[]}
        />
    );
}

describe('WorkflowNodeDetailsPanel properties tab around an operation switch', () => {
    beforeEach(() => {
        panelState.propertiesLoading = false;
    });

    it('renders the properties once the switched operation display conditions have arrived', () => {
        renderPanel(false);

        expect(screen.getByTestId('generic-properties')).toBeInTheDocument();
        expect(screen.queryByTestId('properties-tab-skeleton')).not.toBeInTheDocument();
    });

    it('keeps the whole tab on one skeleton while the switch saves and its display conditions refetch', () => {
        renderPanel(true);

        expect(screen.getByTestId('properties-tab-skeleton')).toBeInTheDocument();
        expect(screen.queryByTestId('generic-properties')).not.toBeInTheDocument();
    });
});
