import {WorkflowEditorReadOnlyContext} from '@/pages/platform/workflow-editor/providers/workflowEditorReadOnlyContext';
import {render, resetAll, screen} from '@/shared/util/test-utils';
import {afterEach, describe, expect, it, vi} from 'vitest';

import OutputSchemaCreationControls from './OutputSchemaCreationControls';

const renderControls = (readOnly: boolean) =>
    render(
        <WorkflowEditorReadOnlyContext.Provider value={readOnly}>
            <OutputSchemaCreationControls
                handleTestOperationClick={vi.fn()}
                outputDefined
                saveWorkflowNodeTestOutputMutationPending={false}
                setShowUploadDialog={vi.fn()}
                showUploadSampleOutputButton
                uploadSampleOutputRequestMutationPending={false}
            />
        </WorkflowEditorReadOnlyContext.Provider>
    );

afterEach(() => {
    resetAll();
});

describe('OutputSchemaCreationControls', () => {
    it('offers testing and uploading sample output when the editor is editable', () => {
        renderControls(false);

        expect(screen.getByRole('button', {name: 'Test Action'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Upload Sample Output Data'})).toBeInTheDocument();
    });

    it('offers neither testing nor uploading sample output in read-only mode', () => {
        renderControls(true);

        expect(screen.getByText('Define Output Schema')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Test Action'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Upload Sample Output Data'})).not.toBeInTheDocument();
    });
});
