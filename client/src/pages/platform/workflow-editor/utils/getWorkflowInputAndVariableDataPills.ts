import {VariableI} from '@/shared/edition/variables/variablesApi';
import {WorkflowInput} from '@/shared/middleware/platform/configuration';
import {DataPillType} from '@/shared/types';

export const VARIABLES_NODE_NAME = 'vars';

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
