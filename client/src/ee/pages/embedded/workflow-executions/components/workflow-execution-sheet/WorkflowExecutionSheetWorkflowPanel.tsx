import {SheetCloseButton, SheetHeader, SheetTitle} from '@/components/ui/sheet';
import {WorkflowExecution} from '@/ee/shared/middleware/embedded/workflow/execution';
import EnvironmentBadge from '@/shared/components/EnvironmentBadge';

const WorkflowExecutionSheetWorkflowPanel = ({workflowExecution}: {workflowExecution: WorkflowExecution}) => {
    const {integration, integrationInstance, workflow} = workflowExecution;

    return (
        <div className="flex size-full flex-col">
            <SheetHeader className="flex flex-row items-center justify-between space-y-0 p-3">
                <SheetTitle>
                    <span>
                        {integration?.name} / {workflow?.label}
                    </span>
                </SheetTitle>

                <SheetCloseButton />
            </SheetHeader>
        </div>
    );
};

export default WorkflowExecutionSheetWorkflowPanel;
