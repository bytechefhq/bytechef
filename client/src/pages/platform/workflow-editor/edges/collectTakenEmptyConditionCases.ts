import {CONDITION_CASE_FALSE, CONDITION_CASE_TRUE} from '@/shared/constants';

type ConditionCaseType = typeof CONDITION_CASE_TRUE | typeof CONDITION_CASE_FALSE;

export interface ConditionCaseTaskExecutionI {
    children?: ConditionCaseTaskExecutionI[];
    iterations?: ConditionCaseTaskExecutionI[][];
    status?: string;
    workflowTask?: {name?: string; parameters?: {[key: string]: unknown}; type?: string};
}

export function toConditionCaseKey(conditionName: string, conditionCase: string): string {
    return `${conditionName}/${conditionCase}`;
}

function isEmptyCase(parameters: {[key: string]: unknown}, conditionCase: ConditionCaseType): boolean {
    const caseTasks = parameters[conditionCase];

    return !Array.isArray(caseTasks) || caseTasks.length === 0;
}

export default function collectTakenEmptyConditionCases(
    taskExecutions: ConditionCaseTaskExecutionI[] | undefined
): Set<string> {
    const takenEmptyConditionCases = new Set<string>();

    const visit = (currentTaskExecutions: ConditionCaseTaskExecutionI[]) => {
        for (const taskExecution of currentTaskExecutions) {
            const {name, parameters, type} = taskExecution.workflowTask ?? {};

            if (
                name &&
                parameters &&
                type?.startsWith('condition/') &&
                taskExecution.status === 'COMPLETED' &&
                !taskExecution.children?.length
            ) {
                const emptyTrueCase = isEmptyCase(parameters, CONDITION_CASE_TRUE);
                const emptyFalseCase = isEmptyCase(parameters, CONDITION_CASE_FALSE);

                if (emptyTrueCase !== emptyFalseCase) {
                    takenEmptyConditionCases.add(
                        toConditionCaseKey(name, emptyTrueCase ? CONDITION_CASE_TRUE : CONDITION_CASE_FALSE)
                    );
                }
            }

            visit([...(taskExecution.children ?? []), ...(taskExecution.iterations ?? []).flat()]);
        }
    };

    visit(taskExecutions ?? []);

    return takenEmptyConditionCases;
}
