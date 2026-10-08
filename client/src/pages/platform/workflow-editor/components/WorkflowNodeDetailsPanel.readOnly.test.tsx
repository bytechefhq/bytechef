import {NodeDataType, PropertyAllType} from '@/shared/types';
import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import {WorkflowEditorReadOnlyContext} from '../providers/workflowEditorReadOnlyContext';
import WorkflowNodeDetailsPanel from './WorkflowNodeDetailsPanel';

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
        awaitingFirstSave: false,
        currentComponentDefinition: {
            actions: [
                {name: 'debug', title: 'Debug'},
                {name: 'info', title: 'Info'},
            ],
        },
        currentNode: LOGGER_NODE,
        currentOperationName: 'debug',
        currentOperationProperties: LOGGER_PROPERTIES,
        currentWorkflowNode: {name: LOGGER_NODE.workflowNodeName},
        currentWorkflowNodeConnections: [],
        currentWorkflowNodeOperations: [{name: 'debug'}, {name: 'info'}],
        errors: [],
        getNodeVersion: () => '1',
        nodeTabs: [],
        workflow: {id: 'workflow-1'},
        workflowNodeDetailsPanelOpen: true,
    }),
}));

vi.mock('@/pages/platform/workflow-editor/components/properties/Properties', () => ({
    default: () => <input aria-label="text property" defaultValue="hello" />,
}));

vi.mock('@/shared/components/copilot/hooks/useCopilotLayoutShifted', () => ({default: () => false}));

const renderPanel = (readOnly: boolean) =>
    render(
        <WorkflowEditorReadOnlyContext.Provider value={readOnly}>
            <WorkflowNodeDetailsPanel
                previousComponentDefinitions={[]}
                updateWorkflowMutation={{isPending: false, mutate: vi.fn()} as never}
                workflowNodeOutputs={[]}
            />
        </WorkflowEditorReadOnlyContext.Provider>
    );

describe('WorkflowNodeDetailsPanel in read-only mode', () => {
    it('keeps property inputs, the operation select and the version select enabled when editable', () => {
        renderPanel(false);

        expect(screen.getByRole('textbox', {name: 'text property'})).toBeEnabled();
        expect(screen.getByRole('combobox', {name: 'Component version'})).toBeEnabled();

        for (const combobox of screen.getAllByRole('combobox')) {
            expect(combobox).toBeEnabled();
        }
    });

    it('disables property inputs while still showing their values', () => {
        renderPanel(true);

        const propertyInput = screen.getByRole('textbox', {name: 'text property'});

        expect(propertyInput).toBeDisabled();
        expect(propertyInput).toHaveValue('hello');
    });

    it('disables the operation and version selects', () => {
        renderPanel(true);

        expect(screen.getByRole('combobox', {name: 'Component version'})).toBeDisabled();

        for (const combobox of screen.getAllByRole('combobox')) {
            expect(combobox).toBeDisabled();
        }
    });
});
