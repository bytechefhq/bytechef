import LoadingIcon from '@/components/LoadingIcon';
import ReadOnlyWorkflowEditor from '@/shared/components/ReadOnlyWorkflow';
import {WorkflowExecutionModel} from '@/shared/middleware/automation/workflow/execution';
import {useGetWorkflowQuery} from '@/shared/queries/automation/workflows.queries';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/platform/componentDefinitions.queries';

const WorkflowExecutionWorkflowPanel = ({workflowExecution}: {workflowExecution: WorkflowExecutionModel}) => {
    const {project, projectInstance, workflow} = workflowExecution;

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
        <div className="flex size-full flex-col bg-muted">
            {workflow?.label && (
                <h1 className="p-4 text-lg font-semibold">
                    {`${project?.name}/${projectInstance?.environment}/${workflow?.label}`}

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

export default WorkflowExecutionWorkflowPanel;
