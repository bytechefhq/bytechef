import {WorkflowInput, WorkflowNodeOutput} from '@/shared/middleware/platform/configuration';

import getDataPillPanelNodeOutputs from './getDataPillPanelNodeOutputs';

export default function hasDataPillPanelContent(
    workflowNodeOutputs: Array<WorkflowNodeOutput> | undefined,
    currentNodeName: string | undefined,
    workflowInputs: Array<WorkflowInput> | undefined
): boolean {
    const nodeOutputs = getDataPillPanelNodeOutputs(workflowNodeOutputs ?? [], currentNodeName);

    return nodeOutputs.length > 0 || !!workflowInputs?.length;
}
