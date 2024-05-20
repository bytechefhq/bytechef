import * as SheetPrimitive from '@radix-ui/react-dialog';
import {Cross2Icon} from '@radix-ui/react-icons';
import {WorkflowExecutionModel} from 'middleware/automation/workflow/execution';

const WorkflowExecutionSheetPanel = ({workflowExecution}: {workflowExecution: WorkflowExecutionModel}) => {
    const {project, projectInstance, workflow} = workflowExecution;

    return (
        <div className="flex size-full flex-col bg-gray-100">
            <div className="flex w-full items-center justify-between p-4">
                <h3>
                    {workflow?.label
                        ? `${project?.name}/${projectInstance?.environment}/${workflow?.label}`
                        : 'No data to show'}
                </h3>

                <div className="flex items-center">
                    {/*<Button*/}

                    {/*    className="mr-1"*/}

                    {/*    onClick={() =>*/}

                    {/*        navigate(*/}

                    {/*            `/automation/projects/${project?.id}/project-workflows/${workflow?.projectWorkflowId}`*/}

                    {/*        )*/}

                    {/*    }*/}

                    {/*    size="sm"*/}

                    {/*    variant="outline"*/}

                    {/*>*/}

                    {/*    <PencilIcon className="mr-1 size-4 cursor-pointer" /> Edit*/}

                    {/*</Button>*/}

                    <SheetPrimitive.Close asChild>
                        <Cross2Icon className="size-4 cursor-pointer opacity-70" />
                    </SheetPrimitive.Close>
                </div>
            </div>

            <div className="flex-1"></div>
        </div>
    );
};

export default WorkflowExecutionSheetPanel;
