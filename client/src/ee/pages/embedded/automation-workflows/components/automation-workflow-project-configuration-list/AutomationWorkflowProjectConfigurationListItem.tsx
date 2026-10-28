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
import {
    AutomationWorkflowProjectConfigurationsQuery,
    AutomationWorkflowProjectsQuery,
    useEnableAutomationWorkflowProjectConfigurationMutation,
} from '@/shared/middleware/graphql';
import {Environment} from '@/shared/middleware/platform/configuration';
import {useQueryClient} from '@tanstack/react-query';
import {ChevronDownIcon, EllipsisVerticalIcon, PencilIcon, Trash2Icon} from 'lucide-react';
import {useState} from 'react';

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

    const queryClient = useQueryClient();

    const enableConfigurationMutation = useEnableAutomationWorkflowProjectConfigurationMutation();

    const environmentName =
        environments.find((environment) => String(environment.id) === configuration.environmentId)?.name ||
        configuration.environmentId;

    const newerVersionAvailable =
        project.lastPublishedVersion != null && project.lastPublishedVersion > configuration.projectVersion;

    const workflowCount = configuration.workflows.length;

    return (
        <Collapsible className="group rounded border border-border/50">
            <div className="flex w-full items-center justify-between rounded-md px-3 py-3 hover:bg-surface-neutral-primary-hover">
                <div className="flex flex-col gap-2">
                    <span className="text-base font-semibold">{environmentName}</span>

                    <CollapsibleTrigger
                        className="group flex items-center text-xs font-semibold text-muted-foreground"
                        disabled={workflowCount === 0}
                    >
                        <span className="mr-1">
                            {workflowCount === 1 ? '1 workflow' : `${workflowCount} workflows`}
                        </span>

                        <ChevronDownIcon className="size-4 duration-300 group-data-[state=open]:rotate-180" />
                    </CollapsibleTrigger>
                </div>

                <div className="flex items-center justify-end gap-x-4">
                    {!configuration.automationHubVisible && (
                        <Badge label="Hidden from hub" styleType="secondary-outline" />
                    )}

                    {newerVersionAvailable && <Badge label="Newer version available" styleType="warning-outline" />}

                    <Badge label={`V${configuration.projectVersion}`} styleType="secondary-filled" weight="semibold" />

                    <Switch
                        aria-label="Enable Configuration"
                        checked={configuration.enabled}
                        disabled={enableConfigurationMutation.isPending}
                        onCheckedChange={(enable) =>
                            enableConfigurationMutation.mutate(
                                {enable, id: configuration.id},
                                {
                                    onSuccess: () => {
                                        queryClient.invalidateQueries({
                                            queryKey: ['automationWorkflowProjectConfigurations'],
                                        });
                                        queryClient.invalidateQueries({queryKey: ['automationWorkflowProjects']});
                                    },
                                }
                            )
                        }
                    />

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
