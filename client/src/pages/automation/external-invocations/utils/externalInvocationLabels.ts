import {type BadgeStyleType} from '@/components/Badge/Badge';
import {formatDateTime} from '@/pages/automation/workflow-executions/utils/workflowExecutionsTable';
import {ExternalInvocationLogsQuery} from '@/shared/middleware/graphql';

export type ExternalInvocationLogItemType = ExternalInvocationLogsQuery['externalInvocationLogs']['content'][number];

export const SOURCE_LABELS: Record<string, string> = {
    A2A_SERVER: 'A2A',
    API_ACTION: 'API (Action)',
    API_TOOL: 'API (Tool)',
    MCP_SERVER: 'MCP',
};

export const STATUS_STYLE_TYPES: Record<string, BadgeStyleType> = {
    APPROVAL_DENIED: 'warning-outline',
    ERROR: 'destructive-outline',
    RULE_BLOCKED: 'warning-outline',
    SUCCESS: 'success-outline',
    TIMEOUT: 'destructive-outline',
};

const TOOL_SOURCES = new Set(['API_TOOL', 'MCP_SERVER']);

const CAMEL_CASE_BOUNDARY_PATTERN = /([a-z0-9])([A-Z])/g;

export const invocationNameLabel = (externalInvocationLog: ExternalInvocationLogItemType): string | null => {
    const {invocationName, source} = externalInvocationLog;

    if (!invocationName || !TOOL_SOURCES.has(source)) {
        return invocationName ?? null;
    }

    return invocationName.replace(CAMEL_CASE_BOUNDARY_PATTERN, '$1_$2').toUpperCase();
};

export const targetLabel = (externalInvocationLog: ExternalInvocationLogItemType): string => {
    if (externalInvocationLog.kind === 'WORKFLOW') {
        return externalInvocationLog.workflowName || externalInvocationLog.componentName || 'Workflow';
    }

    const componentName = externalInvocationLog.componentName || '—';

    return externalInvocationLog.operationName
        ? `${componentName} / ${externalInvocationLog.operationName}`
        : componentName;
};

export const formatCreatedDate = (createdDate: unknown): string => {
    if (createdDate == null) {
        return '';
    }

    return formatDateTime(new Date(Number(createdDate)));
};
