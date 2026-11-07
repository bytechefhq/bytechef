import {CONDITION_CASE_FALSE, CONDITION_CASE_TRUE} from '@/shared/constants';

import {type ConditionCaseTaskExecutionI, toConditionCaseKey} from './collectTakenEmptyConditionCases';

type ConditionCaseType = typeof CONDITION_CASE_TRUE | typeof CONDITION_CASE_FALSE;

const CONDITION_CASES: ConditionCaseType[] = [CONDITION_CASE_TRUE, CONDITION_CASE_FALSE];

function getCaseTaskNames(parameters: {[key: string]: unknown}, conditionCase: ConditionCaseType): string[] {
    const caseTasks = parameters[conditionCase];

    if (!Array.isArray(caseTasks)) {
        return [];
    }

    return caseTasks
        .map((caseTask) => (caseTask as {name?: unknown} | null)?.name)
        .filter((name): name is string => typeof name === 'string');
}

function resolveTakenCase(taskExecution: ConditionCaseTaskExecutionI): ConditionCaseType | undefined {
    const parameters = taskExecution.workflowTask?.parameters;

    if (!parameters) {
        return undefined;
    }

    const firstChildName = taskExecution.children?.[0]?.workflowTask?.name;

    if (firstChildName) {
        return CONDITION_CASES.find((conditionCase) =>
            getCaseTaskNames(parameters, conditionCase).includes(firstChildName)
        );
    }

    if (taskExecution.status !== 'COMPLETED') {
        return undefined;
    }

    const emptyCases = CONDITION_CASES.filter(
        (conditionCase) => getCaseTaskNames(parameters, conditionCase).length === 0
    );

    return emptyCases.length === 1 ? emptyCases[0] : undefined;
}

export default function collectConditionCaseCounts(
    taskExecutions: ConditionCaseTaskExecutionI[] | undefined
): Map<string, number> {
    const conditionCaseCounts = new Map<string, number>();

    const visit = (currentTaskExecutions: ConditionCaseTaskExecutionI[]) => {
        for (const taskExecution of currentTaskExecutions) {
            const {name, type} = taskExecution.workflowTask ?? {};

            if (name && type?.startsWith('condition/')) {
                const takenCase = resolveTakenCase(taskExecution);

                if (takenCase) {
                    const conditionCaseKey = toConditionCaseKey(name, takenCase);

                    conditionCaseCounts.set(conditionCaseKey, (conditionCaseCounts.get(conditionCaseKey) ?? 0) + 1);
                }
            }

            visit([...(taskExecution.children ?? []), ...(taskExecution.iterations ?? []).flat()]);
        }
    };

    visit(taskExecutions ?? []);

    return conditionCaseCounts;
}
