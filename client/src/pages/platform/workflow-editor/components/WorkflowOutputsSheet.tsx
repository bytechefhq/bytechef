import {Button} from '@/components/ui/button';
import {Sheet, SheetContent, SheetHeader, SheetTitle} from '@/components/ui/sheet';
import WorkflowOutputsSheetDialog from '@/pages/platform/workflow-editor/components/WorkflowOutputsSheetDialog';
import WorkflowOutputsSheetTable from '@/pages/platform/workflow-editor/components/WorkflowOutputsSheetTable';
import {WorkflowModel} from '@/shared/middleware/platform/configuration';

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

                    <div className="mx-8 flex items-center">
                        {workflow.outputs && workflow.outputs?.length > 0 && (
                            <WorkflowOutputsSheetDialog
                                triggerNode={<Button size="sm">New Output</Button>}
                                workflow={workflow}
                            />
                        )}
                    </div>
                </div>
            </SheetHeader>

            <WorkflowOutputsSheetTable workflow={workflow} />
        </SheetContent>
    </Sheet>
);

export default WorkflowOutputsSheet;
