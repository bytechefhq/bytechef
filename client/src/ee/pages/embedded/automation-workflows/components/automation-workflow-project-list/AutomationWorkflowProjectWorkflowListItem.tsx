import '@/shared/styles/dropdownMenu.css';
import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {AutomationWorkflowProjectsQuery} from '@/shared/middleware/graphql';
import {ComponentIcon, EllipsisVerticalIcon, Trash2Icon} from 'lucide-react';
import InlineSVG from 'react-inlinesvg';

type AutomationWorkflowProjectWorkflowTemplateType =
    AutomationWorkflowProjectsQuery['automationWorkflowProjects'][number]['workflowTemplates'][number];

interface AutomationWorkflowProjectWorkflowListItemProps {
    onDeleteWorkflow: (workflowUuid: string) => void;
    onSelectWorkflow: (workflowUuid: string) => void;
    workflow: AutomationWorkflowProjectWorkflowTemplateType;
}

const AutomationWorkflowProjectWorkflowListItem = ({
    onDeleteWorkflow,
    onSelectWorkflow,
    workflow,
}: AutomationWorkflowProjectWorkflowListItemProps) => {
    const trigger = workflow.triggers[0];

    const modifiedDate = workflow.lastModifiedDate
        ? new Date(workflow.lastModifiedDate).toLocaleDateString()
        : undefined;

    return (
        <li className="flex items-center justify-between rounded-md px-2 py-1 hover:bg-destructive-foreground">
            <div
                className="flex flex-1 cursor-pointer items-center gap-2"
                onClick={() => onSelectWorkflow(workflow.workflowUuid)}
                onKeyDown={(event) => {
                    if (event.key === 'Enter' || event.key === ' ') {
                        onSelectWorkflow(workflow.workflowUuid);
                    }
                }}
                role="button"
                tabIndex={0}
            >
                <div className="w-80 shrink-0 pr-1 text-sm font-semibold">
                    <Tooltip>
                        <TooltipTrigger className="line-clamp-1 text-start">
                            {workflow.label || workflow.workflowUuid}
                        </TooltipTrigger>

                        <TooltipContent>{workflow.label || workflow.workflowUuid}</TooltipContent>
                    </Tooltip>
                </div>

                {trigger && (
                    <div className="flex shrink-0 items-center gap-1">
                        <div className="flex shrink-0 items-center justify-center rounded-full border border-stroke-neutral-primary bg-surface-neutral-primary p-1">
                            {trigger.icon ? (
                                <InlineSVG
                                    className="size-5"
                                    loader={<ComponentIcon className="size-5 flex-none" />}
                                    src={trigger.icon}
                                    title={null}
                                />
                            ) : (
                                <ComponentIcon className="size-3 flex-none text-content-neutral-primary" />
                            )}
                        </div>

                        <div className="shrink-0">
                            <Badge
                                label={trigger.title || trigger.name}
                                styleType="outline-outline"
                                weight="semibold"
                            />
                        </div>
                    </div>
                )}

                {workflow.components.length > 0 && (
                    <div className="flex items-center gap-2">
                        {workflow.components.map((component) => (
                            <div
                                className="flex shrink-0 items-center justify-center rounded-full border bg-background p-1"
                                key={`component-${component.name}`}
                                title={component.title ?? component.name}
                            >
                                {component.icon && <InlineSVG className="size-5 flex-none" src={component.icon} />}
                            </div>
                        ))}
                    </div>
                )}
            </div>

            <div className="flex justify-end gap-x-6">
                {modifiedDate && (
                    <Tooltip>
                        <TooltipTrigger className="flex items-center text-sm text-muted-foreground">
                            <span className="text-xs">{`Modified at ${modifiedDate}`}</span>
                        </TooltipTrigger>

                        <TooltipContent>Last Modified Date</TooltipContent>
                    </Tooltip>
                )}

                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <Button
                            aria-label="Workflow Actions"
                            icon={<EllipsisVerticalIcon />}
                            onClick={(event) => event.stopPropagation()}
                            size="icon"
                            variant="ghost"
                        />
                    </DropdownMenuTrigger>

                    <DropdownMenuContent align="end" className="p-0">
                        <DropdownMenuItem
                            aria-label="Delete Workflow"
                            className="dropdown-menu-item-destructive"
                            onClick={(event) => {
                                event.stopPropagation();

                                onDeleteWorkflow(workflow.workflowUuid);
                            }}
                        >
                            <Trash2Icon /> Delete
                        </DropdownMenuItem>
                    </DropdownMenuContent>
                </DropdownMenu>
            </div>
        </li>
    );
};

export default AutomationWorkflowProjectWorkflowListItem;
