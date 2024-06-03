import {WorkflowExecutionModel} from '@/shared/middleware/automation/workflow/execution';

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
                </div>
            </div>

            <div className="flex-1"></div>
        </div>
    );
};

export default WorkflowExecutionSheetPanel;
