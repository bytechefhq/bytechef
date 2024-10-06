import LoadingIcon from '@/components/LoadingIcon';
import ReadOnlyWorkflowEditor from '@/shared/components/ReadOnlyWorkflow';
import {WorkflowExecution} from '@/shared/middleware/embedded/workflow/execution';
import {useGetWorkflowQuery} from '@/shared/queries/embedded/workflows.queries';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/platform/componentDefinitions.queries';

const WorkflowExecutionSheetWorkflowPanel = ({workflowExecution}: {workflowExecution: WorkflowExecution}) => {
    const {integration, integrationInstance, workflow} = workflowExecution;

    const {data: workflowDetails} = useGetWorkflowQuery(workflow!.id!, !!workflow?.id);

    const workflowComponentNames = [
        ...(workflowDetails?.workflowTriggerComponentNames ?? []),
        ...(workflowDetails?.workflowTaskComponentNames ?? []),
    ];

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery(
        {include: workflowComponentNames},
        workflowComponentNames !== undefined
    );

    return (
        <div className="flex size-full flex-col bg-muted/50">
            {workflow?.label && (
                <h1 className="p-4 text-lg font-semibold">
                    {`${integration?.componentName}/${integrationInstance?.environment}/${workflow?.label}`}

                    <span className="pl-1 text-sm font-normal text-gray-500">(read-only)</span>
                </h1>
            )}

            {componentDefinitions && workflowDetails ? (
                <ReadOnlyWorkflowEditor componentDefinitions={componentDefinitions} workflow={workflowDetails} />
            ) : (
                <LoadingIcon />
            )}
        </div>
    );
};

export default WorkflowExecutionSheetWorkflowPanel;
