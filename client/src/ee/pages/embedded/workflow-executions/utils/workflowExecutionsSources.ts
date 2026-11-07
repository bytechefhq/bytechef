export const WorkflowExecutionsSource = {
    Any: 'ANY',
    Automations: 'AUTOMATIONS',
    Integrations: 'INTEGRATIONS',
} as const;

export type WorkflowExecutionsSourceType = (typeof WorkflowExecutionsSource)[keyof typeof WorkflowExecutionsSource];

export const WORKFLOW_EXECUTIONS_SOURCE_OPTIONS: Array<{label: string; value: WorkflowExecutionsSourceType}> = [
    {label: 'Any', value: WorkflowExecutionsSource.Any},
    {label: 'Integrations', value: WorkflowExecutionsSource.Integrations},
    {label: 'Automations', value: WorkflowExecutionsSource.Automations},
];

export const getWorkflowExecutionsSource = (source: string | null): WorkflowExecutionsSourceType =>
    WORKFLOW_EXECUTIONS_SOURCE_OPTIONS.find((option) => option.value === source)?.value ?? WorkflowExecutionsSource.Any;
