import Badge, {type BadgeStyleType} from '@/components/Badge/Badge';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import {ExternalInvocationLogsQuery} from '@/shared/middleware/graphql';

type ExternalInvocationLogItemType = ExternalInvocationLogsQuery['externalInvocationLogs']['content'][number];

interface ExternalInvocationsTableProps {
    a2aServerNames?: Record<string, string>;
    mcpServerNames?: Record<string, string>;
    showSurface?: boolean;
    externalInvocationLogs: Array<ExternalInvocationLogItemType>;
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
    externalInvocationLog: ExternalInvocationLogItemType,
    mcpServerNames: Record<string, string> | undefined,
    a2aServerNames: Record<string, string> | undefined
): string => {
    if (externalInvocationLog.mcpServerId != null) {
        return mcpServerNames?.[String(externalInvocationLog.mcpServerId)] || '—';
    }

    if (externalInvocationLog.a2aServerId != null) {
        return a2aServerNames?.[String(externalInvocationLog.a2aServerId)] || '—';
    }

    return '—';
};

const sourceLabel = (externalInvocationLog: ExternalInvocationLogItemType): string => {
    if (externalInvocationLog.kind === 'WORKFLOW') {
        return externalInvocationLog.workflowName || externalInvocationLog.componentName || 'Workflow';
    }

    const componentName = externalInvocationLog.componentName || '—';

    return externalInvocationLog.operationName
        ? `${componentName} / ${externalInvocationLog.operationName}`
        : componentName;
};

const formatCreatedDate = (createdDate: unknown): string => {
    if (createdDate == null) {
        return '';
    }

    return new Date(Number(createdDate)).toLocaleString();
};

const ExternalInvocationsTable = ({
    a2aServerNames,
    externalInvocationLogs,
    mcpServerNames,
    showSurface = true,
}: ExternalInvocationsTableProps) => (
    <Table>
        <TableHeader>
            <TableRow>
                <TableHead>Status</TableHead>

                <TableHead>Tool</TableHead>

                {showSurface && <TableHead>Surface</TableHead>}

                <TableHead>Server</TableHead>

                <TableHead>Source</TableHead>

                <TableHead>Duration</TableHead>

                <TableHead>Date</TableHead>
            </TableRow>
        </TableHeader>

        <TableBody>
            {externalInvocationLogs.map((externalInvocationLog) => (
                <TableRow key={externalInvocationLog.id}>
                    <TableCell>
                        <Badge
                            className="border-transparent"
                            label={externalInvocationLog.outcome}
                            styleType={OUTCOME_STYLE_TYPES[externalInvocationLog.outcome] || 'secondary-filled'}
                            weight="semibold"
                        />
                    </TableCell>

                    <TableCell className="font-semibold">{externalInvocationLog.toolName || '—'}</TableCell>

                    {showSurface && (
                        <TableCell>
                            {SURFACE_LABELS[externalInvocationLog.surface] || externalInvocationLog.surface}
                        </TableCell>
                    )}

                    <TableCell>{serverLabel(externalInvocationLog, mcpServerNames, a2aServerNames)}</TableCell>

                    <TableCell>{sourceLabel(externalInvocationLog)}</TableCell>

                    <TableCell>{externalInvocationLog.durationMs} ms</TableCell>

                    <TableCell>{formatCreatedDate(externalInvocationLog.createdDate)}</TableCell>
                </TableRow>
            ))}
        </TableBody>
    </Table>
);

export default ExternalInvocationsTable;
