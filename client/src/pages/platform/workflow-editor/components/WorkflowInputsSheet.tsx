import {Button} from '@/components/ui/button';
import {Sheet, SheetCloseButton, SheetContent, SheetHeader, SheetTitle} from '@/components/ui/sheet';
import WorkflowInputsSheetDialog from '@/pages/platform/workflow-editor/components/WorkflowInputsSheetDialog';
import WorkflowInputsSheetTable from '@/pages/platform/workflow-editor/components/WorkflowInputsSheetTable';
import {Workflow, WorkflowTestConfiguration} from '@/shared/middleware/platform/configuration';

interface WorkflowInputsSheetProps {
    onClose: () => void;
    workflow: Workflow;
    workflowTestConfiguration?: WorkflowTestConfiguration;
}

const WorkflowInputsSheet = ({onClose, workflow, workflowTestConfiguration}: WorkflowInputsSheetProps) => (
    <Sheet onOpenChange={onClose} open>
        <SheetContent
            className="flex flex-col p-4 sm:max-w-workflow-inputs-sheet-width"
            onFocusOutside={(event) => event.preventDefault()}
            onPointerDownOutside={(event) => event.preventDefault()}
        >
            <SheetHeader className="flex flex-row items-center justify-between">
                <SheetTitle>Workflow Inputs</SheetTitle>

                <div className="flex items-center gap-1">
                    {workflow.inputs && workflow.inputs.length > 0 && (
                        <WorkflowInputsSheetDialog
                            triggerNode={<Button size="sm">New Input</Button>}
                            workflow={workflow}
                            workflowTestConfiguration={workflowTestConfiguration}
                        />
                    )}

                    <SheetCloseButton />
                </div>
            </SheetHeader>

            <WorkflowInputsSheetTable workflow={workflow} workflowTestConfiguration={workflowTestConfiguration} />
        </SheetContent>
    </Sheet>
);

export default WorkflowInputsSheet;
