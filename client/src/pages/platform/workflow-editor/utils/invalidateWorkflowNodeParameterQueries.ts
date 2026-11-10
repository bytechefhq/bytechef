import {GetWorkflowNodeParameterDisplayConditionsRequest} from '@/shared/middleware/platform/configuration';
import {WorkflowNodeDescriptionKeys} from '@/shared/queries/platform/workflowNodeDescriptions.queries';
import {WorkflowNodeOutputKeys} from '@/shared/queries/platform/workflowNodeOutputs.queries';
import {WorkflowNodeParameterKeys} from '@/shared/queries/platform/workflowNodeParameters.queries';
import {QueryClient} from '@tanstack/react-query';

export default function invalidateWorkflowNodeParameterQueries(
    queryClient: QueryClient,
    {environmentId, id, workflowNodeName}: GetWorkflowNodeParameterDisplayConditionsRequest
): void {
    const workflowNodeRequest = {environmentId, id, workflowNodeName};

    void queryClient.invalidateQueries({
        queryKey: WorkflowNodeDescriptionKeys.workflowNodeDescription(workflowNodeRequest),
    });

    void queryClient.invalidateQueries({
        queryKey: WorkflowNodeParameterKeys.propertyWorkflowNodeParameterDisplayConditions(workflowNodeRequest),
    });

    void queryClient.invalidateQueries({
        queryKey: WorkflowNodeOutputKeys.workflowNodeOutputs,
    });
}
