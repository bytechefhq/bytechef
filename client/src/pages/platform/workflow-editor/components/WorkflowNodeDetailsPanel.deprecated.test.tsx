import {TooltipProvider} from '@/components/ui/tooltip';
import {ActionDefinition, TaskDispatcherDefinition} from '@/shared/middleware/platform/configuration';
import {NodeDataType} from '@/shared/types';
import {render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import WorkflowNodeDetailsPanel from './WorkflowNodeDetailsPanel';

const {panelState} = vi.hoisted(() => ({
    panelState: {
        currentActionDefinition: undefined as ActionDefinition | undefined,
        currentTaskDispatcherDefinition: undefined as TaskDispatcherDefinition | undefined,
    },
}));

const WAIT_FOR_APPROVAL_NODE = {
    label: 'Wait for manager',
    name: 'waitForApproval_1',
    type: 'waitForApproval/v1',
    workflowNodeName: 'waitForApproval_1',
} as NodeDataType;

vi.mock('./hooks/useWorkflowNodeDetailsPanel', () => ({
    default: () => ({
        activeTab: 'description',
        awaitingFirstSave: false,
        currentActionDefinition: panelState.currentActionDefinition,
        currentNode: WAIT_FOR_APPROVAL_NODE,
        currentOperationProperties: [],
        currentTaskDispatcherDefinition: panelState.currentTaskDispatcherDefinition,
        currentWorkflowNode: {name: WAIT_FOR_APPROVAL_NODE.workflowNodeName},
        currentWorkflowNodeConnections: [],
        errors: [],
        errorsLoading: false,
        getNodeVersion: () => '1',
        nodeTabs: [],
        propertiesLoading: false,
        workflow: {id: 'workflow-1'},
        workflowNodeDetailsPanelOpen: true,
    }),
}));

vi.mock('@/shared/components/copilot/hooks/useCopilotLayoutShifted', () => ({default: () => false}));

function renderPanel() {
    render(
        <TooltipProvider>
            <WorkflowNodeDetailsPanel
                previousComponentDefinitions={[]}
                updateWorkflowMutation={{isPending: false, mutate: vi.fn()} as never}
                workflowNodeOutputs={[]}
            />
        </TooltipProvider>
    );
}

describe('WorkflowNodeDetailsPanel deprecated badge', () => {
    beforeEach(() => {
        panelState.currentActionDefinition = undefined;
        panelState.currentTaskDispatcherDefinition = undefined;
    });

    it('marks a node whose task dispatcher is deprecated', () => {
        panelState.currentTaskDispatcherDefinition = {
            deprecated: true,
            name: 'waitForApproval',
            outputDefined: true,
            version: 1,
        } as TaskDispatcherDefinition;

        renderPanel();

        expect(screen.getByText('Deprecated')).toBeInTheDocument();
    });

    it('marks a node whose action is deprecated', () => {
        panelState.currentActionDefinition = {
            componentName: 'approvalLink',
            componentVersion: 1,
            deprecated: true,
            name: 'createApprovalLinks',
            outputDefined: true,
            outputFunctionDefined: false,
        } as ActionDefinition;

        renderPanel();

        expect(screen.getByText('Deprecated')).toBeInTheDocument();
    });

    it('does not mark a node whose definition is current', () => {
        panelState.currentTaskDispatcherDefinition = {
            name: 'loop',
            outputDefined: true,
            version: 1,
        } as TaskDispatcherDefinition;

        renderPanel();

        expect(screen.queryByText('Deprecated')).not.toBeInTheDocument();
    });
});
