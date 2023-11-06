import {Button} from '@/components/ui/button';
import {Cross1Icon} from '@radix-ui/react-icons';
import {PencilIcon} from 'lucide-react';
import {WorkflowExecutionModel} from 'middleware/helios/execution';
import {useNavigate} from 'react-router-dom';

const ReadOnlyWorkflow = ({
    execution,
    setWorkflowExecutionDetailsDialogOpen,
}: {
    execution: WorkflowExecutionModel;
    setWorkflowExecutionDetailsDialogOpen: (
        workflowExecutionDetailsDialogOpen: boolean
    ) => void;
}) => {
    const {instance, project, workflow} = execution;

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
                    size="sm"
                    className="ml-4"
                    onClick={() =>
                        navigate(
                            `/automation/projects/${project?.id}/workflow/${workflow?.id}`
                        )
                    }
                    variant="outline"
                >
                    <PencilIcon className="mr-1 h-4 w-4 cursor-pointer" /> Edit
                </Button>

                <Button
                    aria-label="Close panel"
                    className="ml-auto"
                    variant="ghost"
                    size="icon"
                    onClick={() => setWorkflowExecutionDetailsDialogOpen(false)}
                >
                    <Cross1Icon
                        className="h-3 w-3 cursor-pointer"
                        aria-hidden="true"
                    />
                </Button>
            </div>
        </div>
    );
};

export default ReadOnlyWorkflow;
