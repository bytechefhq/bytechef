import {Button} from '@/components/ui/button';
import {PencilIcon} from 'lucide-react';
import {WorkflowExecutionModel} from 'middleware/helios/execution';
import {useNavigate} from 'react-router-dom';

const WorkflowExecutionsWorkflowView = ({
    workflowExecution,
}: {
    workflowExecution: WorkflowExecutionModel;
}) => {
    const {instance, project, workflow} = workflowExecution;

    const navigate = useNavigate();

    return (
        <div className="flex w-full justify-between bg-gray-100 p-4 align-middle">
            <h3>
                {workflow?.label
                    ? `${project?.name ? project.name + ' / ' : ''}${
                          instance?.name ? instance.name + ' / ' : ''
                      }${workflow?.label}`
                    : 'No data to show'}
            </h3>

            <div className="flex align-middle">
                <Button
                    className="mr-6"
                    onClick={() =>
                        navigate(
                            `/automation/projects/${project?.id}/workflows/${workflow?.id}`
                        )
                    }
                    size="sm"
                    variant="outline"
                >
                    <PencilIcon className="mr-1 h-4 w-4 cursor-pointer" /> Edit
                </Button>
            </div>
        </div>
    );
};

export default WorkflowExecutionsWorkflowView;
