import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import {ToolInvocationLogsQuery} from '@/shared/middleware/graphql';
import {twMerge} from 'tailwind-merge';

type ToolInvocationLogItemType = ToolInvocationLogsQuery['toolInvocationLogs']['content'][number];

interface ToolInvocationsTableProps {
    mcpServerNames?: Record<string, string>;
    showSurface?: boolean;
    toolInvocationLogs: Array<ToolInvocationLogItemType>;
}

const SURFACE_LABELS: Record<string, string> = {
    EMBEDDED_API_ACTION: 'Embedded API (Action)',
    EMBEDDED_API_TOOL: 'Embedded API (Tool)',
    MCP_AUTOMATION: 'Automation MCP',
    MCP_EMBEDDED: 'Embedded MCP',
};

const outcomeClassName = (outcome: string): string => {
    if (outcome === 'SUCCESS') {
        return 'bg-success text-success-foreground';
    }

    if (outcome === 'ERROR' || outcome === 'TIMEOUT') {
        return 'bg-destructive text-destructive-foreground';
    }

    return 'bg-surface-neutral-secondary text-content-neutral-secondary';
};

const mcpServerLabel = (
    toolInvocationLog: ToolInvocationLogItemType,
    mcpServerNames: Record<string, string> | undefined
): string => {
    if (toolInvocationLog.mcpServerId == null) {
        return '—';
    }

    return mcpServerNames?.[String(toolInvocationLog.mcpServerId)] || '—';
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

const ToolInvocationsTable = ({mcpServerNames, showSurface = true, toolInvocationLogs}: ToolInvocationsTableProps) => (
    <Table>
        <TableHeader>
            <TableRow>
                <TableHead>Tool</TableHead>

                {showSurface && <TableHead>Surface</TableHead>}

                <TableHead>MCP Server</TableHead>

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

                    <TableCell>{mcpServerLabel(toolInvocationLog, mcpServerNames)}</TableCell>

                    <TableCell>{sourceLabel(toolInvocationLog)}</TableCell>

                    <TableCell>
                        <span
                            className={twMerge(
                                'inline-flex rounded-full px-2 py-0.5 text-xs font-medium',
                                outcomeClassName(toolInvocationLog.outcome)
                            )}
                        >
                            {toolInvocationLog.outcome}
                        </span>
                    </TableCell>

                    <TableCell>{toolInvocationLog.durationMs} ms</TableCell>

                    <TableCell>{formatCreatedDate(toolInvocationLog.createdDate)}</TableCell>
                </TableRow>
            ))}
        </TableBody>
    </Table>
);

export default ToolInvocationsTable;
