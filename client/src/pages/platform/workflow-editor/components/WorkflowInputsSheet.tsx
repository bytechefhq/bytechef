import {Button} from '@/components/ui/button';
import {Sheet, SheetContent, SheetHeader, SheetTitle} from '@/components/ui/sheet';
import WorkflowInputsSheetDialog from '@/pages/platform/workflow-editor/components/WorkflowInputsSheetDialog';
import WorkflowInputsSheetTable from '@/pages/platform/workflow-editor/components/WorkflowInputsSheetTable';
import {WorkflowModel} from '@/shared/middleware/automation/configuration';
import {WorkflowTestConfigurationModel} from '@/shared/middleware/platform/configuration';
import * as SheetPrimitive from '@radix-ui/react-dialog';
import {Cross2Icon} from '@radix-ui/react-icons';

interface WorkflowInputsSheetProps {
    onClose: () => void;
    workflow: WorkflowModel;
    workflowTestConfiguration?: WorkflowTestConfigurationModel;
}

const WorkflowInputsSheet = ({onClose, workflow, workflowTestConfiguration}: WorkflowInputsSheetProps) => (
    <Sheet onOpenChange={onClose} open>
        <SheetContent
            className="flex flex-col p-4 sm:max-w-[700px]"
            onFocusOutside={(event) => event.preventDefault()}
            onPointerDownOutside={(event) => event.preventDefault()}
        >
            <SheetHeader>
                <div className="flex items-center justify-between">
                    <SheetTitle className="flex">Workflow Inputs</SheetTitle>

                    <div className="flex items-center gap-2">
                        {workflow.inputs && workflow.inputs.length > 0 && (
                            <WorkflowInputsSheetDialog
                                triggerNode={<Button size="sm">New Input</Button>}
                                workflow={workflow}
                                workflowTestConfiguration={workflowTestConfiguration}
                            />
                        )}

                        <SheetPrimitive.Close asChild>
                            <Cross2Icon className="size-4 cursor-pointer opacity-70" />
                        </SheetPrimitive.Close>
                    </div>
                </div>
            </SheetHeader>

            <WorkflowInputsSheetTable workflow={workflow} workflowTestConfiguration={workflowTestConfiguration} />
        </SheetContent>
    </Sheet>
);

export default WorkflowInputsSheet;
