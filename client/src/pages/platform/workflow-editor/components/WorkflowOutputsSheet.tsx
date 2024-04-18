import {Button} from '@/components/ui/button';
import {Sheet, SheetContent, SheetHeader, SheetTitle} from '@/components/ui/sheet';
import {WorkflowModel} from '@/middleware/platform/configuration';
import WorkflowOutputsSheetDialog from '@/pages/platform/workflow-editor/components/WorkflowOutputsSheetDialog';
import WorkflowOutputsSheetTable from '@/pages/platform/workflow-editor/components/WorkflowOutputsSheetTable';
import * as SheetPrimitive from '@radix-ui/react-dialog';
import {Cross2Icon} from '@radix-ui/react-icons';

interface WorkflowOutputsSheetProps {
    onClose: () => void;
    workflow: WorkflowModel;
}

const WorkflowOutputsSheet = ({onClose, workflow}: WorkflowOutputsSheetProps) => (
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

            <WorkflowOutputsSheetTable workflow={workflow} />
        </SheetContent>
    </Sheet>
);

export default WorkflowOutputsSheet;
