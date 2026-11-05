import {NodeDataType} from '@/shared/types';
import {fireEvent, render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import WorkflowNodeDetailsPanel from './WorkflowNodeDetailsPanel';
import {WorkflowNodeDetailsErrorI} from './hooks/getMissingRequiredConnectionErrors';

const {panelState} = vi.hoisted(() => ({
    panelState: {errors: [] as Array<WorkflowNodeDetailsErrorI>, errorsLoading: false},
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
        errorsLoading: panelState.errorsLoading,
        getNodeVersion: () => '1',
        nodeTabs: [],
        propertiesLoading: false,
        workflow: {id: 'workflow-1'},
        workflowNodeDetailsPanelOpen: true,
    }),
}));

vi.mock('@/shared/components/copilot/hooks/useCopilotLayoutShifted', () => ({default: () => false}));

function renderPanel(errors: Array<WorkflowNodeDetailsErrorI>) {
    panelState.errors = errors;

    render(
        <WorkflowNodeDetailsPanel
            previousComponentDefinitions={[]}
            updateWorkflowMutation={{isPending: false, mutate: vi.fn()} as never}
            workflowNodeOutputs={[]}
        />
    );

    const chip = screen.queryByRole('button', {name: /Errors|Warnings/});

    if (chip) {
        fireEvent.click(chip);
    }

    return chip;
}

describe('WorkflowNodeDetailsPanel errors chip', () => {
    beforeEach(() => {
        panelState.errors = [];
        panelState.errorsLoading = false;
    });

    it('shows no chip when the node has no errors or warnings', () => {
        expect(renderPanel([])).toBeNull();
    });

    it('titles a node with only errors as errors and marks no row as a warning', () => {
        const chip = renderPanel([{kind: 'PROPERTY', name: 'model', severity: 'ERROR'}]);

        expect(chip).toHaveAccessibleName('Errors (1)');
        expect(chip).toHaveTextContent('1');
        expect(screen.getByRole('region', {name: 'Errors (1)'})).toHaveTextContent('Missing required property:');
        expect(screen.queryByRole('region', {name: /^Warnings/})).not.toBeInTheDocument();
    });

    it('titles a node with only warnings as warnings and styles the chip as a warning', () => {
        const chip = renderPanel([{kind: 'ISSUE', name: 'Missing recommended field: label', severity: 'WARNING'}]);

        expect(chip).toHaveAccessibleName('Warnings (1)');
        expect(chip).toHaveClass('border-stroke-warning-primary');
        expect(screen.getByRole('region', {name: 'Warnings (1)'})).toHaveTextContent(
            'Missing recommended field: label'
        );
        expect(screen.queryByRole('region', {name: /^Errors/})).not.toBeInTheDocument();
    });

    it('counts errors and warnings separately and keeps the error styling when both are present', () => {
        const chip = renderPanel([
            {kind: 'CONNECTION', name: 'OpenAI', severity: 'ERROR'},
            {kind: 'ISSUE', name: 'Missing recommended field: label', severity: 'WARNING'},
        ]);

        expect(chip).toHaveAccessibleName('Errors (1), Warnings (1)');
        expect(chip).toHaveTextContent('2');
        expect(chip).toHaveClass('border-stroke-destructive-primary');
        expect(screen.getByRole('region', {name: 'Errors (1)'})).toHaveTextContent('Missing required connection:');
        expect(screen.getByRole('region', {name: 'Warnings (1)'})).toHaveTextContent(
            'Missing recommended field: label'
        );
    });

    it('keeps the heading as the chip name while errors are rechecked', () => {
        panelState.errorsLoading = true;

        const chip = renderPanel([
            {kind: 'CONNECTION', name: 'OpenAI', severity: 'ERROR'},
            {kind: 'ISSUE', name: 'Missing recommended field: label', severity: 'WARNING'},
        ]);

        expect(chip).toHaveAccessibleName('Errors (1), Warnings (1)');
        expect(chip).toHaveAttribute('aria-busy', 'true');
        expect(chip).toBeDisabled();
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
