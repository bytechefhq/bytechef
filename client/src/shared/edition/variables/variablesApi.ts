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
