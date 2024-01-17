import {Button} from '@/components/ui/button';
import {Sheet, SheetContent, SheetHeader, SheetTitle} from '@/components/ui/sheet';
import {WorkflowModel} from '@/middleware/automation/configuration';
import WorkflowInputsSheetDialog from '@/pages/automation/project/components/WorkflowInputsSheetDialog';
import WorkflowInputsSheetTable from '@/pages/automation/project/components/WorkflowInputsSheetTable';
import * as SheetPrimitive from '@radix-ui/react-dialog';
import {Cross2Icon} from '@radix-ui/react-icons';

interface WorkflowInputsSheetProps {
    onClose: () => void;
    projectId: number;
    workflow: WorkflowModel;
}

const WorkflowInputsSheet = ({onClose, projectId, workflow}: WorkflowInputsSheetProps) => (
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
                            triggerNode={<Button size="sm">Create</Button>}
                            workflow={workflow}
                        />

                        <SheetPrimitive.Close asChild>
                            <Cross2Icon className="size-4 cursor-pointer opacity-70" />
                        </SheetPrimitive.Close>
                    </div>
                </div>
            </SheetHeader>

            <WorkflowInputsSheetTable inputs={workflow.inputs || []} projectId={projectId} workflow={workflow} />
        </SheetContent>
    </Sheet>
);

export default WorkflowInputsSheet;
