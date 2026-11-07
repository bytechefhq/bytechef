import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {WorkflowExecution, WorkflowExecutionSourceEnum} from '@/shared/middleware/automation/workflow/execution';
import {Layers3Icon, LayoutTemplateIcon, type LucideIcon, NetworkIcon, ServerIcon} from 'lucide-react';

interface WorkflowExecutionSourceTypeI {
    icon: LucideIcon;
    label: string;
}

const WORKFLOW_EXECUTION_SOURCE_TYPES: Record<WorkflowExecutionSourceEnum, WorkflowExecutionSourceTypeI> = {
    [WorkflowExecutionSourceEnum.A2AServer]: {icon: NetworkIcon, label: 'A2A server'},
    [WorkflowExecutionSourceEnum.ApiCollection]: {icon: LayoutTemplateIcon, label: 'API collection'},
    [WorkflowExecutionSourceEnum.McpServer]: {icon: ServerIcon, label: 'MCP server'},
    [WorkflowExecutionSourceEnum.ProjectDeployment]: {icon: Layers3Icon, label: 'Project deployment'},
};

interface WorkflowExecutionSourceCellProps {
    execution: WorkflowExecution;
}

const WorkflowExecutionSourceCell = ({execution}: WorkflowExecutionSourceCellProps) => {
    const sourceType = execution.source ? WORKFLOW_EXECUTION_SOURCE_TYPES[execution.source] : undefined;

    if (!sourceType) {
        return null;
    }

    const Icon = sourceType.icon;

    const name = execution.sourceName || execution.projectDeployment?.name;

    return (
        <span className="flex min-w-0 items-center gap-2">
            <Tooltip>
                <TooltipTrigger asChild>
                    <span aria-label={sourceType.label} className="inline-flex flex-none" role="img">
                        <Icon className="size-4 text-content-neutral-secondary" />
                    </span>
                </TooltipTrigger>

                <TooltipContent>{sourceType.label}</TooltipContent>
            </Tooltip>

            {name && <span className="truncate">{name}</span>}
        </span>
    );
};

export default WorkflowExecutionSourceCell;
