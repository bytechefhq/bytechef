import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {WorkflowComponentIconDefinitionType} from '@/pages/automation/project/components/projects-sidebar/components/WorkflowComponentsIcon';
import WorkflowTriggerAndComponentsRow from '@/shared/components/workflow/WorkflowTriggerAndComponentsRow';
import {SquareArrowOutUpRightIcon, WorkflowIcon} from 'lucide-react';
import {ReactNode} from 'react';

interface WorkflowToolRowWorkflowI {
    triggers?: Array<{description?: string; label?: string; type?: string}>;
    workflowTriggerComponentNames?: Array<string>;
}

interface WorkflowToolRowLabelProps {
    canOpenExecutions: boolean;
    filteredComponentNames: string[];
    label: string;
    onOpenInProjectClick?: () => void;
    onWorkflowClick: () => void;
    subLabel?: ReactNode;
    workflow?: WorkflowToolRowWorkflowI;
    workflowComponentDefinitions: Record<string, WorkflowComponentIconDefinitionType | undefined>;
    workflowTaskDispatcherDefinitions: Record<string, WorkflowComponentIconDefinitionType | undefined>;
}

/**
 * The left side of a workflow tool row: the workflow name that opens its executions, an optional link into the
 * project editor and the workflow's trigger pill with its component icons once the workflow is loaded.
 */
const WorkflowToolRowLabel = ({
    canOpenExecutions,
    filteredComponentNames,
    label,
    onOpenInProjectClick,
    onWorkflowClick,
    subLabel,
    workflow,
    workflowComponentDefinitions,
    workflowTaskDispatcherDefinitions,
}: WorkflowToolRowLabelProps) => (
    <div className="flex min-w-0 flex-1 items-center">
        <div className="flex w-full max-w-80 min-w-0 shrink flex-col sm:w-80 sm:shrink-0">
            <div className="flex min-w-0 items-center gap-2">
                <WorkflowIcon aria-hidden="true" className="size-4 shrink-0 text-content-neutral-secondary" />

                <Tooltip>
                    <TooltipTrigger asChild>
                        <button
                            className="min-w-0 truncate text-left text-sm font-medium disabled:cursor-default"
                            disabled={!canOpenExecutions}
                            onClick={onWorkflowClick}
                            type="button"
                        >
                            {label}
                        </button>
                    </TooltipTrigger>

                    <TooltipContent align="start">{canOpenExecutions ? 'Show executions' : label}</TooltipContent>
                </Tooltip>

                {onOpenInProjectClick && (
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <Button
                                aria-label="Open in project"
                                className="-ml-1.5 size-5 shrink-0 [&_svg]:size-3"
                                icon={<SquareArrowOutUpRightIcon className="size-3" />}
                                onClick={onOpenInProjectClick}
                                size="icon"
                                variant="ghost"
                            />
                        </TooltipTrigger>

                        <TooltipContent>Open in project</TooltipContent>
                    </Tooltip>
                )}
            </div>

            {subLabel}
        </div>

        {workflow && (
            <div className="ml-6 flex min-w-0 items-center gap-1 overflow-hidden">
                <WorkflowTriggerAndComponentsRow
                    className="hidden sm:flex"
                    filteredComponentNames={filteredComponentNames}
                    workflow={workflow}
                    workflowComponentDefinitions={workflowComponentDefinitions}
                    workflowTaskDispatcherDefinitions={workflowTaskDispatcherDefinitions}
                />
            </div>
        )}
    </div>
);

export default WorkflowToolRowLabel;
