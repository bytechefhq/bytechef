import {Button} from '@/components/ui/button';
import {Sheet, SheetContent, SheetHeader, SheetTitle} from '@/components/ui/sheet';
import {WorkflowModel} from '@/middleware/automation/configuration';
import {WorkflowTestConfigurationModel} from '@/middleware/platform/workflow/test';
import WorkflowInputsSheetDialog from '@/pages/automation/project/components/WorkflowInputsSheetDialog';
import WorkflowInputsSheetTable from '@/pages/automation/project/components/WorkflowInputsSheetTable';
import * as SheetPrimitive from '@radix-ui/react-dialog';
import {Cross2Icon} from '@radix-ui/react-icons';

interface WorkflowInputsSheetProps {
    onClose: () => void;
    projectId: number;
    workflow: WorkflowModel;
    workflowTestConfiguration?: WorkflowTestConfigurationModel;
}

const WorkflowInputsSheet = ({onClose, projectId, workflow, workflowTestConfiguration}: WorkflowInputsSheetProps) => (
    <Sheet modal={false} onOpenChange={onClose} open>
        <SheetContent
            className="flex flex-col p-4 sm:max-w-[700px]"
            onFocusOutside={(event) => event.preventDefault()}
            onPointerDownOutside={(event) => event.preventDefault()}
        >
            <SheetHeader>
                <div className="flex items-center justify-between">
                    <SheetTitle className="flex">Workflow Inputs</SheetTitle>

                    <div className="flex items-center gap-2">
                        <WorkflowInputsSheetDialog
                            projectId={projectId}
                            triggerNode={<Button size="sm">Add Input</Button>}
                            workflow={workflow}
                            workflowTestConfiguration={workflowTestConfiguration}
                        />

                        <SheetPrimitive.Close asChild>
                            <Cross2Icon className="size-4 cursor-pointer opacity-70" />
                        </SheetPrimitive.Close>
                    </div>
                </div>
            </SheetHeader>

            <WorkflowInputsSheetTable
                projectId={projectId}
                workflow={workflow}
                workflowTestConfiguration={workflowTestConfiguration}
            />
        </SheetContent>
    </Sheet>
);

export default WorkflowInputsSheet;
