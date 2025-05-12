import {Badge} from '@/components/ui/badge';
import {SheetCloseButton, SheetHeader, SheetTitle} from '@/components/ui/sheet';
import {WorkflowExecution} from '@/shared/middleware/automation/workflow/execution';

const WorkflowExecutionSheetWorkflowPanel = ({workflowExecution}: {workflowExecution: WorkflowExecution}) => {
    const {project, projectDeployment, workflow} = workflowExecution;

    return (
        <div className="flex size-full flex-col bg-muted/50">
            <SheetHeader className="flex flex-row items-center justify-between space-y-0 p-3">
                <SheetTitle>
                    <span>
                        {project?.name}/{workflow?.label}/
                    </span>

                    <Badge variant="secondary">{projectDeployment?.environment}</Badge>
                </SheetTitle>

                <SheetCloseButton />
            </SheetHeader>
        </div>
    );
};

export default WorkflowExecutionSheetWorkflowPanel;
