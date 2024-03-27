import {Button} from '@/components/ui/button';
import * as SheetPrimitive from '@radix-ui/react-dialog';
import {Cross2Icon} from '@radix-ui/react-icons';
import {PencilIcon} from 'lucide-react';
import {WorkflowExecutionModel} from 'middleware/automation/workflow/execution';
import {useNavigate} from 'react-router-dom';

const WorkflowExecutionDetailsSheetWorkflowView = ({
    workflowExecution,
}: {
    workflowExecution: WorkflowExecutionModel;
}) => {
    const {project, projectInstance, workflow} = workflowExecution;

    const navigate = useNavigate();

    return (
        <div className="flex size-full flex-col bg-gray-100">
            <div className="flex w-full items-center justify-between p-4">
                <h3>
                    {workflow?.label
                        ? `${project?.name}/${projectInstance?.environment}/${workflow?.label}`
                        : 'No data to show'}
                </h3>

                <div className="flex items-center">
                    <Button
                        className="mr-1"
                        onClick={() => navigate(`/automation/projects/${project?.id}/workflows/${workflow?.id}`)}
                        size="sm"
                        variant="outline"
                    >
                        <PencilIcon className="mr-1 size-4 cursor-pointer" /> Edit
                    </Button>

                    <SheetPrimitive.Close asChild>
                        <Cross2Icon className="size-4 cursor-pointer opacity-70" />
                    </SheetPrimitive.Close>
                </div>
            </div>

            <div className="flex-1"></div>
        </div>
    );
};

export default WorkflowExecutionDetailsSheetWorkflowView;
