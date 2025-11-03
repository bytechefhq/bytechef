import Button from '@/components/Button/Button';
import {SheetCloseButton, SheetHeader, SheetTitle} from '@/components/ui/sheet';
import WorkflowOutputsSheetDialog from '@/pages/platform/workflow-editor/components/WorkflowOutputsSheetDialog';
import WorkflowOutputsSheetTable from '@/pages/platform/workflow-editor/components/WorkflowOutputsSheetTable';
import {Workflow} from '@/shared/middleware/platform/configuration';

interface WorkflowOutputsSheetContentProps {
    workflow: Workflow;
}

const WorkflowOutputsSheetContent = ({workflow}: WorkflowOutputsSheetContentProps) => (
    <>
        <SheetHeader className="flex flex-row items-center justify-between space-y-0">
            <SheetTitle>Workflow Outputs</SheetTitle>

            <div className="flex items-center space-x-2">
                {!!workflow.outputs?.length && (
                    <WorkflowOutputsSheetDialog
                        triggerNode={<Button label="New Output" size="sm" />}
                        workflow={workflow}
                    />
                )}

                <SheetCloseButton />
            </div>
        </SheetHeader>

        <WorkflowOutputsSheetTable workflow={workflow} />
    </>
);

export default WorkflowOutputsSheetContent;
