import {Button} from '@/components/ui/button';
import {Sheet, SheetCloseButton, SheetContent, SheetHeader, SheetTitle} from '@/components/ui/sheet';
import {WorkflowTestConfiguration} from '@/shared/middleware/platform/configuration';
import {PlusIcon, SlidersIcon} from 'lucide-react';

import WorkflowInputsDeleteDialog from './WorkflowInputsDeleteDialog';
import WorkflowInputsEditDialog from './WorkflowInputsEditDialog';
import WorkflowInputsTable from './WorkflowInputsTable';
import useWorkflowInputs from './hooks/useWorkflowInputs';

interface WorkflowInputsSheetProps {
    invalidateWorkflowQueries: () => void;
    onSheetOpenChange: (open: boolean) => void;
    sheetOpen: boolean;
    workflowTestConfiguration?: WorkflowTestConfiguration;
}

const WorkflowInputsSheet = ({
    invalidateWorkflowQueries,
    onSheetOpenChange,
    sheetOpen,
    workflowTestConfiguration,
}: WorkflowInputsSheetProps) => {
    const {
        closeDeleteDialog,
        closeEditDialog,
        currentInputIndex,
        deleteWorkflowInput,
        form,
        isDeleteDialogOpen,
        isEditDialogOpen,
        openDeleteDialog,
        openEditDialog,
        saveWorkflowInput,
        workflow,
    } = useWorkflowInputs({invalidateWorkflowQueries, workflowTestConfiguration});

    return (
        <Sheet onOpenChange={onSheetOpenChange} open={sheetOpen}>
            <SheetContent
                className="flex flex-col p-4 sm:max-w-workflow-inputs-sheet-width"
                onFocusOutside={(event) => event.preventDefault()}
                onPointerDownOutside={(event) => event.preventDefault()}
            >
                <SheetHeader className="flex flex-row items-center justify-between space-y-0">
                    <SheetTitle>Workflow Inputs</SheetTitle>

                    <div className="flex items-center space-x-2">
                        {!!workflow.inputs?.length && (
                            <Button className="bg-content-brand-primary" onClick={() => openEditDialog()} size="sm">
                                <PlusIcon /> New Input
                            </Button>
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

                            <Button className="bg-content-brand-primary" onClick={() => openEditDialog()}>
                                <PlusIcon className="-ml-0.5 mr-1.5 size-5" />
                                New Input
                            </Button>
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
            </SheetContent>
        </Sheet>
    );
};

export default WorkflowInputsSheet;
