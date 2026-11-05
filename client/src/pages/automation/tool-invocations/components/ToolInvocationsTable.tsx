import Badge, {type BadgeStyleType} from '@/components/Badge/Badge';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import {ToolInvocationLogsQuery} from '@/shared/middleware/graphql';

type ToolInvocationLogItemType = ToolInvocationLogsQuery['toolInvocationLogs']['content'][number];

interface ToolInvocationsTableProps {
    a2aServerNames?: Record<string, string>;
    mcpServerNames?: Record<string, string>;
    showSurface?: boolean;
    toolInvocationLogs: Array<ToolInvocationLogItemType>;
}

const SURFACE_LABELS: Record<string, string> = {
    A2A: 'A2A',
    EMBEDDED_API_ACTION: 'Embedded API (Action)',
    EMBEDDED_API_TOOL: 'Embedded API (Tool)',
    MCP_AUTOMATION: 'Automation MCP',
    MCP_EMBEDDED: 'Embedded MCP',
};

const OUTCOME_STYLE_TYPES: Record<string, BadgeStyleType> = {
    APPROVAL_DENIED: 'warning-outline',
    ERROR: 'destructive-outline',
    RULE_BLOCKED: 'warning-outline',
    SUCCESS: 'success-outline',
    TIMEOUT: 'destructive-outline',
};

const serverLabel = (
    toolInvocationLog: ToolInvocationLogItemType,
    mcpServerNames: Record<string, string> | undefined,
    a2aServerNames: Record<string, string> | undefined
): string => {
    if (toolInvocationLog.mcpServerId != null) {
        return mcpServerNames?.[String(toolInvocationLog.mcpServerId)] || '—';
    }

    if (toolInvocationLog.a2aServerId != null) {
        return a2aServerNames?.[String(toolInvocationLog.a2aServerId)] || '—';
    }

    return '—';
};

const sourceLabel = (toolInvocationLog: ToolInvocationLogItemType): string => {
    if (toolInvocationLog.kind === 'WORKFLOW') {
        return toolInvocationLog.workflowName || toolInvocationLog.componentName || 'Workflow';
    }

    const componentName = toolInvocationLog.componentName || '—';

    return toolInvocationLog.operationName ? `${componentName} / ${toolInvocationLog.operationName}` : componentName;
};

const formatCreatedDate = (createdDate: unknown): string => {
    if (createdDate == null) {
        return '';
    }

    return new Date(Number(createdDate)).toLocaleString();
};

const ToolInvocationsTable = ({
    a2aServerNames,
    mcpServerNames,
    showSurface = true,
    toolInvocationLogs,
}: ToolInvocationsTableProps) => (
    <Table>
        <TableHeader>
            <TableRow>
                <TableHead>Tool</TableHead>

                {showSurface && <TableHead>Surface</TableHead>}

                <TableHead>Server</TableHead>

                <TableHead>Source</TableHead>

                <TableHead>Outcome</TableHead>

                <TableHead>Duration</TableHead>

                <TableHead>Date</TableHead>
            </TableRow>
        </TableHeader>

        <TableBody>
            {toolInvocationLogs.map((toolInvocationLog) => (
                <TableRow key={toolInvocationLog.id}>
                    <TableCell className="font-semibold">{toolInvocationLog.toolName || '—'}</TableCell>

                    {showSurface && (
                        <TableCell>{SURFACE_LABELS[toolInvocationLog.surface] || toolInvocationLog.surface}</TableCell>
                    )}

                    <TableCell>{serverLabel(toolInvocationLog, mcpServerNames, a2aServerNames)}</TableCell>

                    <TableCell>{sourceLabel(toolInvocationLog)}</TableCell>

                    <TableCell>
                        <Badge
                            className="border-transparent"
                            label={toolInvocationLog.outcome}
                            styleType={OUTCOME_STYLE_TYPES[toolInvocationLog.outcome] || 'secondary-filled'}
                            weight="semibold"
                        />
                    </TableCell>

                    <TableCell>{toolInvocationLog.durationMs} ms</TableCell>

                    <TableCell>{formatCreatedDate(toolInvocationLog.createdDate)}</TableCell>
                </TableRow>
            ))}
        </TableBody>
    </Table>
);

export default ToolInvocationsTable;
