import {GetWorkflowExecutionsPageSourceEnum} from '@/shared/middleware/automation/workflow/execution';

interface WorkflowExecutionsSourceI {
    deploymentHeader: string;
    eeOnly: boolean;
    label: string;
    value: GetWorkflowExecutionsPageSourceEnum;
}

export const DEFAULT_WORKFLOW_EXECUTIONS_SOURCE = GetWorkflowExecutionsPageSourceEnum.Any;

export const WORKFLOW_EXECUTIONS_SOURCES: WorkflowExecutionsSourceI[] = [
    {
        deploymentHeader: 'Deployment',
        eeOnly: false,
        label: 'Any',
        value: GetWorkflowExecutionsPageSourceEnum.Any,
    },
    {
        deploymentHeader: 'Deployment',
        eeOnly: false,
        label: 'Project Deployments',
        value: GetWorkflowExecutionsPageSourceEnum.ProjectDeployment,
    },
    {
        deploymentHeader: 'MCP server',
        eeOnly: false,
        label: 'MCP Servers',
        value: GetWorkflowExecutionsPageSourceEnum.McpServer,
    },
    {
        deploymentHeader: 'A2A server',
        eeOnly: false,
        label: 'A2A Servers',
        value: GetWorkflowExecutionsPageSourceEnum.A2AServer,
    },
    {
        deploymentHeader: 'API collection',
        eeOnly: true,
        label: 'API Collections',
        value: GetWorkflowExecutionsPageSourceEnum.ApiCollection,
    },
];

export const getWorkflowExecutionsSources = (eeEdition: boolean): WorkflowExecutionsSourceI[] =>
    WORKFLOW_EXECUTIONS_SOURCES.filter((source) => eeEdition || !source.eeOnly);

export const getWorkflowExecutionsSourceDeploymentHeader = (source: GetWorkflowExecutionsPageSourceEnum): string =>
    WORKFLOW_EXECUTIONS_SOURCES.find((workflowExecutionsSource) => workflowExecutionsSource.value === source)
        ?.deploymentHeader || 'Deployment';
