import {Button} from '@/components/ui/button';
import {
    Sheet,
    SheetContent,
    SheetHeader,
    SheetTitle,
} from '@/components/ui/sheet';
import {WorkflowModel} from '@/middleware/helios/configuration';
import WorkflowInputsSheetDialog from '@/pages/automation/project/components/WorkflowInputsSheetDialog';
import WorkflowInputsSheetTable from '@/pages/automation/project/components/WorkflowInputsSheetTable';
import * as SheetPrimitive from '@radix-ui/react-dialog';
import {Cross2Icon} from '@radix-ui/react-icons';

interface WorkflowExecutionDetailsSheetProps {
    onClose: () => void;
    projectId: number;
    workflow: WorkflowModel;
}

const WorkflowInputsSheet = ({
    onClose,
    projectId,
    workflow,
}: WorkflowExecutionDetailsSheetProps) => {
    return (
        <Sheet modal={false} onOpenChange={onClose} open={true}>
            <SheetContent className="flex flex-col p-4 sm:max-w-[500px]">
                <SheetHeader>
                    <div className="flex items-center justify-between">
                        <SheetTitle className="flex">
                            Workflow Inputs
                        </SheetTitle>

                        <div className="flex items-center gap-1">
                            <WorkflowInputsSheetDialog
                                projectId={projectId}
                                triggerNode={<Button size="sm">Create</Button>}
                                workflow={workflow}
                            />

                            <SheetPrimitive.Close asChild>
                                <Button size="icon" variant="ghost">
                                    <Cross2Icon className="h-4 w-4 opacity-70" />
                                </Button>
                            </SheetPrimitive.Close>
                        </div>
                    </div>
                </SheetHeader>

                <WorkflowInputsSheetTable
                    inputs={workflow.inputs || []}
                    projectId={projectId}
                    workflow={workflow}
                />
            </SheetContent>
        </Sheet>
    );
};

export default WorkflowInputsSheet;
