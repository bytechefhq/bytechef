import {CONDITION_CASE_FALSE, CONDITION_CASE_TRUE} from '@/shared/constants';

type ConditionCaseType = typeof CONDITION_CASE_TRUE | typeof CONDITION_CASE_FALSE;

/**
 * The fields of a task execution this reads. Structural rather than a generated model because loop
 * `iterations` are left as raw JSON by the generated converters.
 */
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

/**
 * The empty condition cases the run took, as `toConditionCaseKey` keys.
 *
 * An empty case leaves no child execution behind, so it cannot be found the way a case with tasks is
 * (by one of its tasks having run). What gives it away is a completed condition visit with no
 * children: the case that visit took had nothing to run. When exactly one of its two cases is empty,
 * that is the one it took. When both are empty the visit cannot say which, and nothing is recorded.
 *
 * Every visit counts, so a condition in a loop can have taken its empty case in one iteration and its
 * other case in the next.
 */
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
