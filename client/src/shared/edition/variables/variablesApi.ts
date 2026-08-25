/**
 * Edition seam for workflow variables (EE-only). The CE workflow editor reads variables through this registry rather
 * than importing the EE generated hooks; the default reports nothing, so the data-pill panel renders no Variables
 * section on CE. The EE bundle registers the real hooks from registerEditionModules before the first render, so hook
 * identity never changes between renders.
 */

export interface VariableI {
    id: string;
    name: string;
    value: string;
}

export type VariableScopeType = {type: 'WORKSPACE'; workspaceId: number} | {type: 'EMBEDDED'};

export interface VariablesApiI {
    useWorkflowVariablesQuery: (
        scope: VariableScopeType | undefined,
        environmentId: number
    ) => {data: VariableI[] | undefined};
}

const noopVariablesApi: VariablesApiI = {
    useWorkflowVariablesQuery: () => ({data: undefined}),
};

let variablesApi: VariablesApiI = noopVariablesApi;

export function registerVariablesApi(api: VariablesApiI) {
    variablesApi = api;
}

export function getVariablesApi(): VariablesApiI {
    return variablesApi;
}
