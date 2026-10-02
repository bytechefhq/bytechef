import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuLabel,
    DropdownMenuRadioGroup,
    DropdownMenuRadioItem,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Skeleton} from '@/components/ui/skeleton';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {Workflow} from '@/shared/middleware/automation/configuration';
import {ChevronDownIcon, WorkflowIcon} from 'lucide-react';

interface ProjectItemSelectProps {
    currentLabel?: string;
    currentProjectWorkflowId?: number;
    onWorkflowValueChange: (projectWorkflowId: number) => void;
    projectWorkflows: Workflow[];
}

const ProjectItemSelect = ({
    currentLabel,
    currentProjectWorkflowId,
    onWorkflowValueChange,
    projectWorkflows,
}: ProjectItemSelectProps) => (
    <DropdownMenu>
        <Tooltip>
            <TooltipTrigger asChild>
                <DropdownMenuTrigger
                    aria-label="Project item select"
                    className="flex max-w-64 items-center gap-1 rounded-md px-1.5 py-1 font-semibold text-content-neutral-primary outline-hidden hover:bg-surface-neutral-primary-hover data-[state=open]:bg-surface-neutral-primary-hover"
                >
                    {currentLabel ? (
                        <span className="truncate">{currentLabel}</span>
                    ) : (
                        <Skeleton className="h-3 w-44" />
                    )}

                    <ChevronDownIcon className="size-4 shrink-0 text-content-neutral-secondary" />
                </DropdownMenuTrigger>
            </TooltipTrigger>

            {currentLabel && currentLabel.length > 30 && <TooltipContent>{currentLabel}</TooltipContent>}
        </Tooltip>

        <DropdownMenuContent align="start" className="max-w-80 min-w-64">
            <DropdownMenuRadioGroup
                onValueChange={(value) => onWorkflowValueChange(Number(value))}
                value={currentProjectWorkflowId !== undefined ? currentProjectWorkflowId.toString() : ''}
            >
                {projectWorkflows.length > 0 && (
                    <>
                        <DropdownMenuLabel>Workflows</DropdownMenuLabel>

                        {projectWorkflows.map((workflow) => (
                            <DropdownMenuRadioItem
                                className="cursor-pointer"
                                key={workflow.projectWorkflowId!}
                                title={workflow.label!.length > 32 ? workflow.label! : undefined}
                                value={workflow.projectWorkflowId!.toString()}
                            >
                                <WorkflowIcon className="size-4 shrink-0" />

                                <span className="truncate">{workflow.label!}</span>
                            </DropdownMenuRadioItem>
                        ))}
                    </>
                )}
            </DropdownMenuRadioGroup>
        </DropdownMenuContent>
    </DropdownMenu>
);

export default ProjectItemSelect;
