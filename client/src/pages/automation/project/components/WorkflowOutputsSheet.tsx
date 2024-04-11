import {Button} from '@/components/ui/button';
import {Sheet, SheetContent, SheetHeader, SheetTitle} from '@/components/ui/sheet';
import {WorkflowModel} from '@/middleware/automation/configuration';
import WorkflowOutputsSheetDialog from '@/pages/automation/project/components/WorkflowOutputsSheetDialog';
import WorkflowOutputsSheetTable from '@/pages/automation/project/components/WorkflowOutputsSheetTable';
import * as SheetPrimitive from '@radix-ui/react-dialog';
import {Cross2Icon} from '@radix-ui/react-icons';

interface WorkflowOutputsSheetProps {
    onClose: () => void;
    projectId: number;
    workflow: WorkflowModel;
}

const WorkflowOutputsSheet = ({onClose, projectId, workflow}: WorkflowOutputsSheetProps) => (
    <Sheet onOpenChange={onClose} open>
        <SheetContent
            className="flex flex-col p-4 sm:max-w-[700px]"
            onFocusOutside={(event) => event.preventDefault()}
            onPointerDownOutside={(event) => event.preventDefault()}
        >
            <SheetHeader>
                <div className="flex items-center justify-between">
                    <SheetTitle className="flex">Workflow Outputs</SheetTitle>

                    <div className="flex items-center gap-2">
                        {workflow.outputs && workflow.outputs?.length > 0 && (
                            <WorkflowOutputsSheetDialog
                                projectId={projectId}
                                triggerNode={<Button size="sm">New Output</Button>}
                                workflow={workflow}
                            />
                        )}

                        <SheetPrimitive.Close asChild>
                            <Cross2Icon className="size-4 cursor-pointer opacity-70" />
                        </SheetPrimitive.Close>
                    </div>
                </div>
            </SheetHeader>

            <WorkflowOutputsSheetTable projectId={projectId} workflow={workflow} />
        </SheetContent>
    </Sheet>
);

export default WorkflowOutputsSheet;
