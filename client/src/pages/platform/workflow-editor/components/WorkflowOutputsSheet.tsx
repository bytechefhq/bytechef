import {Button} from '@/components/ui/button';
import {Sheet, SheetCloseButton, SheetContent, SheetHeader, SheetTitle} from '@/components/ui/sheet';
import WorkflowOutputsSheetDialog from '@/pages/platform/workflow-editor/components/WorkflowOutputsSheetDialog';
import WorkflowOutputsSheetTable from '@/pages/platform/workflow-editor/components/WorkflowOutputsSheetTable';
import {Workflow} from '@/shared/middleware/platform/configuration';

interface WorkflowOutputsSheetProps {
    onClose: () => void;
    workflow: Workflow;
}

const WorkflowOutputsSheet = ({onClose, workflow}: WorkflowOutputsSheetProps) => (
    <Sheet onOpenChange={onClose} open>
        <SheetContent
            className="flex flex-col p-4 sm:max-w-workflow-outputs-sheet-width"
            onFocusOutside={(event) => event.preventDefault()}
            onPointerDownOutside={(event) => event.preventDefault()}
        >
            <SheetHeader className="flex flex-row items-center justify-between">
                <SheetTitle>Workflow Outputs</SheetTitle>

                <div className="flex items-center gap-1">
                    {!!workflow.outputs?.length && (
                        <WorkflowOutputsSheetDialog
                            triggerNode={<Button size="sm">New Output</Button>}
                            workflow={workflow}
                        />
                    )}

                    <SheetCloseButton />
                </div>
            </SheetHeader>

            <WorkflowOutputsSheetTable workflow={workflow} />
        </SheetContent>
    </Sheet>
);

export default WorkflowOutputsSheet;
