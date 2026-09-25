import {NodeDataType} from '@/shared/types';
import {render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import WorkflowNodeDetailsPanel from './WorkflowNodeDetailsPanel';
import {WorkflowNodeDetailsErrorI} from './hooks/getMissingRequiredConnectionErrors';

const {panelState} = vi.hoisted(() => ({
    panelState: {errors: [] as Array<WorkflowNodeDetailsErrorI>},
}));

const OPEN_AI_NODE = {
    componentName: 'openAi',
    name: 'openAi_1',
    operationName: 'ask',
    type: 'openAi/v1/ask',
    workflowNodeName: 'openAi_1',
} as NodeDataType;

vi.mock('./hooks/useWorkflowNodeDetailsPanel', () => ({
    default: () => ({
        activeTab: 'description',
        awaitingFirstSave: false,
        currentNode: OPEN_AI_NODE,
        currentOperationProperties: [],
        currentWorkflowNode: {name: OPEN_AI_NODE.workflowNodeName},
        currentWorkflowNodeConnections: [],
        errors: panelState.errors,
        errorsAccordionOpen: true,
        errorsLoading: false,
        getNodeVersion: () => '1',
        nodeTabs: [],
        propertiesLoading: false,
        setErrorsAccordionOpen: vi.fn(),
        workflow: {id: 'workflow-1'},
        workflowNodeDetailsPanelOpen: true,
    }),
}));

vi.mock('@/shared/components/copilot/hooks/useCopilotLayoutShifted', () => ({default: () => false}));

function renderPanel(errors: Array<WorkflowNodeDetailsErrorI>) {
    panelState.errors = errors;

    return render(
        <WorkflowNodeDetailsPanel
            previousComponentDefinitions={[]}
            updateWorkflowMutation={{isPending: false, mutate: vi.fn()} as never}
            workflowNodeOutputs={[]}
        />
    );
}

describe('WorkflowNodeDetailsPanel errors box', () => {
    beforeEach(() => {
        panelState.errors = [];
    });

    it('shows no box when the node has no errors or warnings', () => {
        renderPanel([]);

        expect(screen.queryByRole('button', {name: /Errors|Warnings/})).not.toBeInTheDocument();
    });

    it('titles a node with only errors as errors and marks no row as a warning', () => {
        renderPanel([{kind: 'PROPERTY', name: 'model', severity: 'ERROR'}]);

        expect(screen.getByRole('button', {name: /Errors \(1\)/})).toBeInTheDocument();
        expect(screen.getByText('Missing required property:')).toBeInTheDocument();
        expect(screen.queryByLabelText('Warning')).not.toBeInTheDocument();
    });

    it('titles a node with only warnings as warnings and styles the box as a warning', () => {
        renderPanel([{kind: 'ISSUE', name: 'Missing recommended field: label', severity: 'WARNING'}]);

        const heading = screen.getByRole('button', {name: /Warnings \(1\)/});

        expect(heading.parentElement).toHaveClass('border-stroke-warning-primary');
        expect(screen.getByLabelText('Warning')).toBeInTheDocument();
    });

    it('counts errors and warnings separately and keeps the error styling when both are present', () => {
        renderPanel([
            {kind: 'CONNECTION', name: 'OpenAI', severity: 'ERROR'},
            {kind: 'ISSUE', name: 'Missing recommended field: label', severity: 'WARNING'},
        ]);

        const heading = screen.getByRole('button', {name: /Errors \(1\), Warnings \(1\)/});

        expect(heading.parentElement).toHaveClass('border-stroke-destructive-primary');
        expect(screen.getByText('Missing required connection:')).toBeInTheDocument();
        expect(screen.getAllByLabelText('Warning')).toHaveLength(1);
    });

    it('names the property an issue concerns in front of its message', () => {
        renderPanel([
            {
                kind: 'ISSUE',
                name: 'References disabled node firecrawl_5',
                propertyLabel: 'Top K',
                severity: 'WARNING',
            },
        ]);

        expect(screen.getByText('Top K:')).toBeInTheDocument();
        expect(screen.getByText('References disabled node firecrawl_5')).toBeInTheDocument();
    });
});
