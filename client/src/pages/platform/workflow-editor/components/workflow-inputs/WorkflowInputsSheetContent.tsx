import Button from '@/components/Button/Button';
import {SheetCloseButton, SheetHeader, SheetTitle} from '@/components/ui/sheet';
import {WorkflowTestConfiguration} from '@/shared/middleware/platform/configuration';
import {PlusIcon, SlidersIcon} from 'lucide-react';

import WorkflowInputsDeleteDialog from './WorkflowInputsDeleteDialog';
import WorkflowInputsEditDialog from './WorkflowInputsEditDialog';
import WorkflowInputsTable from './WorkflowInputsTable';
import useWorkflowInputs from './hooks/useWorkflowInputs';

interface WorkflowInputsSheetContentProps {
    invalidateWorkflowQueries: () => void;
    workflowTestConfiguration?: WorkflowTestConfiguration;
}

const WorkflowInputsSheetContent = ({
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

    return (
        <>
            <SheetHeader className="flex flex-row items-center justify-between space-y-0">
                <SheetTitle>Workflow Inputs</SheetTitle>

                <div className="flex items-center space-x-2">
                    {!!workflow.inputs?.length && (
                        <Button icon={<PlusIcon />} label="New Input" onClick={() => openEditDialog()} size="sm" />
                    )}

                    <SheetCloseButton />
                </div>
            </SheetHeader>

            {isEditDialogOpen && (
                <WorkflowInputsEditDialog
                    closeDialog={() => closeEditDialog()}
                    currentInputIndex={currentInputIndex}
                    form={form}
                    isEditDialogOpen={isEditDialogOpen}
                    nameInputRef={nameInputRef}
                    openEditDialog={openEditDialog}
                    saveWorkflowInput={saveWorkflowInput}
                />
            )}

            {workflow.inputs?.length === 0 && (
                <div className="flex h-full flex-col justify-center">
                    <div className="flex flex-col items-center space-y-2 self-center align-middle">
                        <SlidersIcon className="size-24 text-gray-300" />

                        <h2 className="text-sm font-semibold">No inputs</h2>

                        <p className="text-sm text-gray-500">Get started by creating a new input.</p>

                        <Button icon={<PlusIcon />} label="New Input" onClick={() => openEditDialog()} />
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
                    openDeleteDialog={openDeleteDialog}
                    openEditDialog={openEditDialog}
                    workflowInputs={workflow.inputs}
                    workflowTestConfigurationInputs={workflowTestConfiguration?.inputs}
                />
            )}
        </>
    );
};

export default WorkflowInputsSheetContent;
