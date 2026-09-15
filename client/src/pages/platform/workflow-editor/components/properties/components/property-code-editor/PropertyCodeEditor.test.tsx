import PropertyCodeEditor from '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/PropertyCodeEditor';
import {WorkflowEditorReadOnlyContext} from '@/pages/platform/workflow-editor/providers/workflowEditorReadOnlyContext';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {fireEvent, render, resetAll, screen, userEvent} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

vi.mock(
    '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/property-code-editor-dialog/PropertyCodeEditorDialog',
    () => ({
        default: () => <div data-testid="property-code-editor-dialog" />,
    })
);

const renderCodeEditor = (readOnly: boolean) =>
    render(
        <WorkflowEditorReadOnlyContext.Provider value={readOnly}>
            <fieldset disabled={readOnly}>
                <PropertyCodeEditor
                    language="javascript"
                    name="script"
                    onChange={vi.fn()}
                    value="return 1;"
                    workflow={{id: 'workflow-1'}}
                    workflowNodeName="script_1"
                />
            </fieldset>
        </WorkflowEditorReadOnlyContext.Provider>
    );

describe('PropertyCodeEditor', () => {
    beforeEach(() => {
        useWorkflowEditorStore.getState().setShowPropertyCodeEditorSheet(false);
    });

    afterEach(() => {
        resetAll();
    });

    it('opens the code editor from Open Code Editor when the editor is editable', async () => {
        renderCodeEditor(false);

        await userEvent.click(screen.getByRole('button', {name: 'Open Code Editor'}));

        expect(screen.getByTestId('property-code-editor-dialog')).toBeInTheDocument();
    });

    it('still lets a viewer open the code read-only inside a disabled property fieldset', () => {
        renderCodeEditor(true);

        expect(screen.queryByRole('button', {name: 'Open Code Editor'})).not.toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', {name: 'View Code'}));

        expect(screen.getByTestId('property-code-editor-dialog')).toBeInTheDocument();
    });
});
