import {WorkflowExecutionModel} from '@/shared/middleware/embedded/workflow/execution';

const WorkflowExecutionSheetPanel = ({workflowExecution}: {workflowExecution: WorkflowExecutionModel}) => {
    const {integration, integrationInstance, workflow} = workflowExecution;

    return (
        <div className="flex w-full justify-between bg-gray-100 p-4 align-middle">
            <h3>
                {workflow?.label
                    ? `${integration?.componentName}/${integrationInstance?.environment}/${workflow?.label}`
                    : 'No data to show'}
            </h3>

            <div className="flex align-middle">
                {/*<Button*/}

                {/*    className="mr-1"*/}

                {/*    onClick={() =>*/}

                {/*        navigate(*/}

                {/*            `/embedded/integrations/${integration?.id}/integration-workflows/${workflow?.integrationWorkflowId}`*/}

                {/*        )*/}

                {/*    }*/}

                {/*    size="sm"*/}

                {/*    variant="outline"*/}

                {/*>*/}

                {/*    <PencilIcon className="mr-1 size-4 cursor-pointer" /> Edit*/}

                {/*</Button>*/}
            </div>
        </div>
    );
};

export default WorkflowExecutionSheetPanel;
