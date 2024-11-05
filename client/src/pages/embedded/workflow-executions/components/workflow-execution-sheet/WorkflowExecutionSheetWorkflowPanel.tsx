import LoadingIcon from '@/components/LoadingIcon';
import {Badge} from '@/components/ui/badge';
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
            <h1 className="flex items-center gap-x-2 p-4 text-base font-semibold">
                <span>{integration?.name}</span>

                <span>/</span>

                <span>{workflow?.label}</span>

                <span>/</span>

                <Badge variant="secondary">{integrationInstance?.environment}</Badge>
            </h1>

            {componentDefinitions && workflowDetails ? (
                <ReadOnlyWorkflowEditor componentDefinitions={componentDefinitions} workflow={workflowDetails} />
            ) : (
                <LoadingIcon />
            )}
        </div>
    );
};

export default WorkflowExecutionSheetWorkflowPanel;
