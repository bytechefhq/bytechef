import {TooltipProvider} from '@/components/ui/tooltip';
import {NodeDataType} from '@/shared/types';
import {fireEvent, render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import WorkflowNodeDetailsPanel from './WorkflowNodeDetailsPanel';

const {panelState} = vi.hoisted(() => ({
    panelState: {
        currentComponentDefinition: undefined as
            | {actions?: Array<{name: string; title: string}>; triggers?: Array<{name: string; title: string}>}
            | undefined,
        currentNode: undefined as NodeDataType | undefined,
        handleOperationSelectChange: vi.fn(),
        handlePanelClose: vi.fn(),
        missingDefinitionMessage: undefined as string | undefined,
    },
}));

vi.mock('./hooks/useWorkflowNodeDetailsPanel', () => ({
    default: () => ({
        activeTab: 'properties',
        awaitingFirstSave: false,
        currentComponentDefinition: panelState.currentComponentDefinition,
        currentNode: panelState.currentNode,
        currentOperationName: panelState.currentNode?.operationName,
        currentOperationProperties: [],
        currentWorkflowNodeConnections: [],
        errors: [],
        errorsLoading: false,
        getNodeVersion: () => '1',
        handleOperationSelectChange: panelState.handleOperationSelectChange,
        handlePanelClose: panelState.handlePanelClose,
        missingDefinitionMessage: panelState.missingDefinitionMessage,
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

describe('WorkflowNodeDetailsPanel missing definition', () => {
    beforeEach(() => {
        Element.prototype.hasPointerCapture = Element.prototype.hasPointerCapture || vi.fn();
        Element.prototype.releasePointerCapture = Element.prototype.releasePointerCapture || vi.fn();
        Element.prototype.scrollIntoView = Element.prototype.scrollIntoView || vi.fn();

        panelState.currentComponentDefinition = undefined;
        panelState.handleOperationSelectChange = vi.fn();
        panelState.handlePanelClose = vi.fn();
    });

    it('shows that the component of the node is not available', () => {
        panelState.currentNode = {
            componentName: 'viber',
            label: 'Viber',
            name: 'viber_1',
            operationName: 'sendTextMessage',
            type: 'viber/v1/sendTextMessage',
            workflowNodeName: 'viber_1',
        } as NodeDataType;
        panelState.missingDefinitionMessage = "Component 'viber' (version 1) is not available";

        renderPanel();

        expect(screen.getByText("Component 'viber' (version 1) is not available")).toBeInTheDocument();
        expect(screen.getByText('Viber')).toBeInTheDocument();
        expect(screen.getByText('(viber_1)')).toBeInTheDocument();
    });

    it('shows that the trigger of the node does not exist', () => {
        panelState.currentNode = {
            componentName: 'infobip',
            label: 'Infobip',
            name: 'trigger_3',
            operationName: 'newViberBusinessMessage',
            trigger: true,
            type: 'infobip/v1/newViberBusinessMessage',
            workflowNodeName: 'trigger_3',
        } as NodeDataType;
        panelState.missingDefinitionMessage =
            "Trigger 'newViberBusinessMessage' does not exist in component 'infobip' (version 1)";

        renderPanel();

        expect(
            screen.getByText("Trigger 'newViberBusinessMessage' does not exist in component 'infobip' (version 1)")
        ).toBeInTheDocument();
        expect(screen.queryByText('Properties')).not.toBeInTheDocument();
    });

    it('closes the panel', () => {
        panelState.currentNode = {
            componentName: 'viber',
            label: 'Viber',
            name: 'viber_1',
            type: 'viber/v1/sendTextMessage',
            workflowNodeName: 'viber_1',
        } as NodeDataType;
        panelState.missingDefinitionMessage = "Component 'viber' (version 1) is not available";

        renderPanel();

        fireEvent.click(screen.getByRole('button', {name: 'Close the node details dialog'}));

        expect(panelState.handlePanelClose).toHaveBeenCalled();
    });

    it('lets another trigger of the component be selected when the trigger of the node does not exist', async () => {
        panelState.currentComponentDefinition = {
            actions: [{name: 'sendSms', title: 'Send SMS'}],
            triggers: [{name: 'newSmsMessage', title: 'New SMS Message'}],
        };
        panelState.currentNode = {
            componentName: 'infobip',
            label: 'Infobip',
            name: 'trigger_3',
            operationName: 'newViberBusinessMessage',
            trigger: true,
            type: 'infobip/v1/newViberBusinessMessage',
            workflowNodeName: 'trigger_3',
        } as NodeDataType;
        panelState.missingDefinitionMessage =
            "Trigger 'newViberBusinessMessage' does not exist in component 'infobip' (version 1)";

        renderPanel();

        expect(screen.getByText('Triggers')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('combobox'));

        expect(screen.queryByRole('option', {name: /Send SMS/})).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('option', {name: /New SMS Message/}));

        expect(panelState.handleOperationSelectChange).toHaveBeenCalledWith('newSmsMessage');
    });

    it('lets another action of the component be selected when the action of the node does not exist', async () => {
        panelState.currentComponentDefinition = {
            actions: [{name: 'sendSms', title: 'Send SMS'}],
            triggers: [{name: 'newSmsMessage', title: 'New SMS Message'}],
        };
        panelState.currentNode = {
            componentName: 'infobip',
            label: 'Infobip',
            name: 'infobip_1',
            operationName: 'sendViberBotTextMessage',
            type: 'infobip/v1/sendViberBotTextMessage',
            workflowNodeName: 'infobip_1',
        } as NodeDataType;
        panelState.missingDefinitionMessage =
            "Action 'sendViberBotTextMessage' does not exist in component 'infobip' (version 1)";

        renderPanel();

        expect(screen.getByText('Actions')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('combobox'));
        await userEvent.click(screen.getByRole('option', {name: /Send SMS/}));

        expect(panelState.handleOperationSelectChange).toHaveBeenCalledWith('sendSms');
    });

    it('offers no operation select when the component of the node is not available', () => {
        panelState.currentNode = {
            componentName: 'viber',
            label: 'Viber',
            name: 'viber_1',
            operationName: 'sendTextMessage',
            type: 'viber/v1/sendTextMessage',
            workflowNodeName: 'viber_1',
        } as NodeDataType;
        panelState.missingDefinitionMessage = "Component 'viber' (version 1) is not available";

        renderPanel();

        expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
    });
});
