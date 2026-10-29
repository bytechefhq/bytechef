import AlertDialog from '@/components/AlertDialog';
import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import Switch from '@/components/Switch/Switch';
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from '@/components/ui/collapsible';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {
    AutomationWorkflowProjectConfigurationsQuery,
    AutomationWorkflowProjectsQuery,
    useEnableAutomationWorkflowProjectConfigurationMutation,
} from '@/shared/middleware/graphql';
import {Environment} from '@/shared/middleware/platform/configuration';
import {useQueryClient} from '@tanstack/react-query';
import {ChevronDownIcon, EllipsisVerticalIcon, PencilIcon, Trash2Icon} from 'lucide-react';
import {MouseEvent, useCallback, useRef, useState} from 'react';

import AutomationWorkflowProjectConfigurationWorkflowList from './AutomationWorkflowProjectConfigurationWorkflowList';

type AutomationWorkflowProjectConfigurationType =
    AutomationWorkflowProjectConfigurationsQuery['automationWorkflowProjectConfigurations'][number];
type AutomationWorkflowProjectType = AutomationWorkflowProjectsQuery['automationWorkflowProjects'][number];

interface AutomationWorkflowProjectConfigurationListItemProps {
    configuration: AutomationWorkflowProjectConfigurationType;
    environments: Environment[];
    onDelete: (configuration: AutomationWorkflowProjectConfigurationType) => void;
    onEdit: (configuration: AutomationWorkflowProjectConfigurationType) => void;
    project: AutomationWorkflowProjectType;
}

const AutomationWorkflowProjectConfigurationListItem = ({
    configuration,
    environments,
    onDelete,
    onEdit,
    project,
}: AutomationWorkflowProjectConfigurationListItemProps) => {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    const workflowsCollapsibleTriggerRef = useRef<HTMLButtonElement | null>(null);

    const queryClient = useQueryClient();

    const enableConfigurationMutation = useEnableAutomationWorkflowProjectConfigurationMutation();

    const environmentName =
        environments.find((environment) => String(environment.id) === configuration.environmentId)?.name ||
        configuration.environmentId;

    const newerVersionAvailable =
        project.lastPublishedVersion != null && project.lastPublishedVersion > configuration.projectVersion;

    const workflowCount = configuration.workflows.length;

    const lastExecutionDate = configuration.lastExecutionDate ? new Date(configuration.lastExecutionDate) : undefined;

    const handleConfigurationListItemClick = useCallback((event: MouseEvent) => {
        const target = event.target as HTMLElement;

        const interactiveSelectors = [
            '[data-interactive]',
            '[role="menuitem"]',
            '[role="menu"]',
            '[role="switch"]',
            '[data-radix-dropdown-menu-trigger]',
            '[data-radix-collapsible-trigger]',
        ].join(', ');

        if (target.closest(interactiveSelectors) || workflowsCollapsibleTriggerRef.current?.contains(target)) {
            return;
        }

        workflowsCollapsibleTriggerRef.current?.click();
    }, []);

    return (
        <Collapsible className="rounded border border-border/50" defaultOpen={workflowCount > 0}>
            <div
                className="flex w-full cursor-pointer items-center justify-between rounded-md px-3 hover:bg-surface-neutral-primary-hover"
                onClick={handleConfigurationListItemClick}
                role="presentation"
            >
                <div className="flex flex-1 items-center py-3">
                    <div className="flex-1">
                        <div className="flex min-h-8 items-center">
                            <span className="text-base font-semibold text-content-neutral-primary">
                                {environmentName}
                            </span>
                        </div>

                        <div className="mt-2 flex min-h-7 items-center">
                            <CollapsibleTrigger
                                className="group/configuration mr-4 flex items-center text-xs font-semibold text-muted-foreground"
                                disabled={workflowCount === 0}
                                ref={workflowsCollapsibleTriggerRef}
                            >
                                <span className="mr-1">
                                    {workflowCount === 1 ? '1 workflow' : `${workflowCount} workflows`}
                                </span>

                                <ChevronDownIcon className="size-4 duration-300 group-data-[state=open]/configuration:rotate-180" />
                            </CollapsibleTrigger>
                        </div>
                    </div>

                    <div className="flex items-center justify-end gap-x-6">
                        {!configuration.automationHubVisible && (
                            <Badge label="Hidden from hub" styleType="secondary-outline" />
                        )}

                        {newerVersionAvailable && <Badge label="Newer version available" styleType="warning-outline" />}

                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Badge
                                    label={`V${configuration.projectVersion}`}
                                    styleType="secondary-filled"
                                    weight="semibold"
                                />
                            </TooltipTrigger>

                            <TooltipContent>The project version released to this environment</TooltipContent>
                        </Tooltip>

                        <div className="flex min-w-52 flex-col items-end gap-y-2">
                            <div className="flex min-h-8 items-center">
                                <Switch
                                    aria-label="Enable Configuration"
                                    checked={configuration.enabled}
                                    disabled={enableConfigurationMutation.isPending}
                                    onCheckedChange={(enable) =>
                                        enableConfigurationMutation.mutate(
                                            {enable, id: configuration.id},
                                            {
                                                onSuccess: () => {
                                                    void queryClient.invalidateQueries({
                                                        queryKey: ['automationWorkflowProjectConfigurations'],
                                                    });
                                                    void queryClient.invalidateQueries({
                                                        queryKey: ['automationWorkflowProjects'],
                                                    });
                                                },
                                            }
                                        )
                                    }
                                />
                            </div>

                            <Tooltip>
                                <TooltipTrigger className="flex min-h-7 items-center text-sm text-content-neutral-secondary">
                                    {lastExecutionDate ? (
                                        <span className="text-xs">
                                            {`Executed at ${lastExecutionDate.toLocaleDateString()} ${lastExecutionDate.toLocaleTimeString()}`}
                                        </span>
                                    ) : (
                                        <span className="text-xs">No executions</span>
                                    )}
                                </TooltipTrigger>

                                <TooltipContent side="left">Last Execution Date</TooltipContent>
                            </Tooltip>
                        </div>

                        <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                                <Button
                                    aria-label="Configuration Actions"
                                    icon={<EllipsisVerticalIcon />}
                                    size="icon"
                                    variant="ghost"
                                />
                            </DropdownMenuTrigger>

                            <DropdownMenuContent align="end">
                                <DropdownMenuItem
                                    aria-label="Edit Configuration"
                                    icon={<PencilIcon />}
                                    label="Edit"
                                    onClick={() => onEdit(configuration)}
                                />

                                <DropdownMenuItem
                                    aria-label="Delete Configuration"
                                    icon={<Trash2Icon />}
                                    label="Delete"
                                    onClick={() => setShowDeleteDialog(true)}
                                    variant="destructive"
                                />
                            </DropdownMenuContent>
                        </DropdownMenu>
                    </div>
                </div>
            </div>

            <CollapsibleContent>
                <AutomationWorkflowProjectConfigurationWorkflowList configuration={configuration} project={project} />
            </CollapsibleContent>

            <AlertDialog
                description="Connected users' automations from this environment are paused and come back if you recreate the configuration. The project and its workflows are not affected."
                onCancel={() => setShowDeleteDialog(false)}
                onConfirm={() => {
                    onDelete(configuration);

                    setShowDeleteDialog(false);
                }}
                open={showDeleteDialog}
            />
        </Collapsible>
    );
};

export default AutomationWorkflowProjectConfigurationListItem;
