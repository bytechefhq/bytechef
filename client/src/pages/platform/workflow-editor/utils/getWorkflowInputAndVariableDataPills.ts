import {VariableI} from '@/shared/edition/variables/variablesApi';
import {WorkflowInput} from '@/shared/middleware/platform/configuration';
import {DataPillType} from '@/shared/types';

export const VARIABLES_NODE_NAME = 'vars';

/**
 * Builds the flat data pills shared by every "previous data" surface (the workflow node details panel, the
 * data-stream editor, the AI agent testing panel): one pill per workflow input, plus one `vars.NAME` pill per
 * variable visible to the workflow. Extracted from the three producers, which previously each carried their own
 * copy of the input-pill mapping.
 */
export default function getWorkflowInputAndVariableDataPills(
    inputs: WorkflowInput[] | undefined,
    variables: VariableI[]
): DataPillType[] {
    const inputDataPills: DataPillType[] = (inputs ?? []).map((input) => ({
        id: input.name,
        nodeName: input.name,
        value: input.name,
    }));

    const variableDataPills: DataPillType[] = variables.map((variable) => ({
        id: `${VARIABLES_NODE_NAME}.${variable.name}`,
        nodeName: VARIABLES_NODE_NAME,
        value: `${VARIABLES_NODE_NAME}.${variable.name}`,
    }));

    return [...inputDataPills, ...variableDataPills];
}
