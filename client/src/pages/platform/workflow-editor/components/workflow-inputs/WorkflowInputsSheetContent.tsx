import Button from '@/components/Button/Button';
import {SheetCloseButton} from '@/components/ui/sheet';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {WorkflowTestConfiguration} from '@/shared/middleware/platform/configuration';
import {PlusIcon, SlidersIcon} from 'lucide-react';

import WorkflowInputsDeleteDialog from './WorkflowInputsDeleteDialog';
import WorkflowInputsEditDialog from './WorkflowInputsEditDialog';
import WorkflowInputsTable from './WorkflowInputsTable';
import useWorkflowInputs from './hooks/useWorkflowInputs';

interface WorkflowInputsSheetContentProps {
    internalOnlyVisible: boolean;
    invalidateWorkflowQueries: () => void;
    workflowTestConfiguration?: WorkflowTestConfiguration;
}

const WorkflowInputsSheetContent = ({
    internalOnlyVisible,
    invalidateWorkflowQueries,
    workflowTestConfiguration,
}: WorkflowInputsSheetContentProps) => {
    const {
        closeDeleteDialog,
        closeEditDialog,
        currentInputIndex,
        deleteWorkflowInput,
        form,
        isDeleteDialogOpen,
        isEditDialogOpen,
        nameInputRef,
        openDeleteDialog,
        openEditDialog,
        saveWorkflowInput,
        workflow,
    } = useWorkflowInputs({invalidateWorkflowQueries, workflowTestConfiguration});

    const {codeWorkflow} = useWorkflowEditor();

    // A code workflow's inputs are generated from its source on every save, so this sheet cannot add or remove them
    // — it stays open because setting a test value is exactly what it is used for.
    const sourceOwnsInputs = codeWorkflow === true;

    return (
        <>
            <header className="flex w-full shrink-0 items-center justify-between gap-x-3 rounded-t-md border-b border-b-border/50 bg-surface-neutral-primary p-3">
                <span className="text-lg font-semibold">Workflow Inputs</span>

                <div className="flex items-center gap-1">
                    {!!workflow.inputs?.length && !sourceOwnsInputs && (
                        <Button icon={<PlusIcon />} label="New Input" onClick={() => openEditDialog()} size="sm" />
                    )}

                    <SheetCloseButton />
                </div>
            </header>

            <div className="flex min-h-0 flex-1 flex-col overflow-y-auto px-2">
                {isEditDialogOpen && (
                    <WorkflowInputsEditDialog
                        closeDialog={() => closeEditDialog()}
                        currentInputIndex={currentInputIndex}
                        form={form}
                        internalOnlyVisible={internalOnlyVisible}
                        isEditDialogOpen={isEditDialogOpen}
                        nameInputRef={nameInputRef}
                        openEditDialog={openEditDialog}
                        saveWorkflowInput={saveWorkflowInput}
                        workflow={workflow}
                    />
                )}

                {workflow.inputs?.length === 0 && (
                    <div className="flex h-full flex-col justify-center">
                        <div className="flex flex-col items-center space-y-2 self-center align-middle">
                            <SlidersIcon className="size-24 text-gray-300" />

                            <h2 className="text-sm font-semibold">No inputs</h2>

                            <p className="text-sm text-content-neutral-secondary">
                                {sourceOwnsInputs
                                    ? "Declare them in the workflow's source, then set their test values here."
                                    : 'Get started by creating a new input.'}
                            </p>

                            {!sourceOwnsInputs && (
                                <Button icon={<PlusIcon />} label="New Input" onClick={() => openEditDialog()} />
                            )}
                        </div>
                    </div>
                )}

                {isDeleteDialogOpen && (
                    <WorkflowInputsDeleteDialog
                        closeDeleteDialog={closeDeleteDialog}
                        currentInputIndex={currentInputIndex}
                        deleteWorkflowInput={deleteWorkflowInput}
                        isDeleteDialogOpen={isDeleteDialogOpen}
                        workflowInputs={workflow.inputs!}
                    />
                )}

                {!!workflow.inputs?.length && (
                    <WorkflowInputsTable
                        codeWorkflow={sourceOwnsInputs}
                        internalOnlyVisible={internalOnlyVisible}
                        openDeleteDialog={openDeleteDialog}
                        openEditDialog={openEditDialog}
                        workflowInputs={workflow.inputs}
                        workflowTestConfigurationInputs={workflowTestConfiguration?.inputs}
                    />
                )}
            </div>
        </>
    );
};

export default WorkflowInputsSheetContent;
