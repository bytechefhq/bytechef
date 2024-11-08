import LoadingIcon from '@/components/LoadingIcon';
import {Button} from '@/components/ui/button';
import {Switch} from '@/components/ui/switch';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useToast} from '@/hooks/use-toast';
import ProjectInstanceEditWorkflowDialog from '@/pages/automation/project-instances/components/ProjectInstanceEditWorkflowDialog';
import ProjectInstanceWorkflowListItemDropdownMenu from '@/pages/automation/project-instances/components/project-instance-workflow-list/ProjectInstanceWorkflowListItemDropdownMenu';
import useProjectInstanceWorkflowSheetStore from '@/pages/automation/project-instances/stores/useProjectInstanceWorkflowSheetStore';
import {ProjectInstanceApi, ProjectInstanceWorkflow, Workflow} from '@/shared/middleware/automation/configuration';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useEnableProjectInstanceWorkflowMutation} from '@/shared/mutations/automation/projectInstanceWorkflows.mutations';
import {ProjectInstanceKeys} from '@/shared/queries/automation/projectInstances.queries';
import {Component1Icon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {useCopyToClipboard} from '@uidotdev/usehooks';
import {ClipboardIcon, PlayIcon} from 'lucide-react';
import {useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {twMerge} from 'tailwind-merge';

const projectInstanceApi = new ProjectInstanceApi();

const ProjectInstanceWorkflowListItem = ({
    filteredComponentNames,
    projectInstanceEnabled,
    projectInstanceId,
    projectInstanceWorkflow,
    workflow,
    workflowComponentDefinitions,
    workflowTaskDispatcherDefinitions,
}: {
    filteredComponentNames?: string[];
    projectInstanceEnabled: boolean;
    projectInstanceId: number;
    projectInstanceWorkflow: ProjectInstanceWorkflow;
    workflow: Workflow;
    workflowComponentDefinitions: {
        [key: string]: ComponentDefinitionBasic | undefined;
    };
    workflowTaskDispatcherDefinitions: {
        [key: string]: ComponentDefinitionBasic | undefined;
    };
}) => {
    const [showEditWorkflowDialog, setShowEditWorkflowDialog] = useState(false);

    const {setProjectInstanceWorkflowSheetOpen, setWorkflowId} = useProjectInstanceWorkflowSheetStore();

    /* eslint-disable @typescript-eslint/no-unused-vars */
    const [_, copyToClipboard] = useCopyToClipboard();
    const {toast} = useToast();

    const queryClient = useQueryClient();

    const enableProjectInstanceWorkflowMutation = useEnableProjectInstanceWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ProjectInstanceKeys.projectInstances,
            });
        },
    });

    const handleProjectInstanceEnable = () => {
        enableProjectInstanceWorkflowMutation.mutate(
            {
                enable: !projectInstanceWorkflow.enabled,
                id: projectInstanceId,
                workflowId: workflow.id!,
            },
            {
                onSuccess: () => {
                    projectInstanceWorkflow = {
                        ...projectInstanceWorkflow,
                        enabled: !projectInstanceWorkflow?.enabled,
                    };
                },
            }
        );
    };

    const handleWorkflowClick = () => {
        setWorkflowId(workflow.id!);

        setProjectInstanceWorkflowSheetOpen(true);
    };

    const handleRunWorkflowClick = (event: React.MouseEvent<HTMLButtonElement>) => {
        event?.stopPropagation();

        projectInstanceApi
            .createProjectInstanceWorkflowJob({
                id: projectInstanceId,
                workflowId: workflow.id!,
            })
            .then(() =>
                toast({
                    description: 'Workflow request sent.',
                })
            );
    };

    const handleEnableProjectInstanceWorkflow = (value: boolean) => {
        enableProjectInstanceWorkflowMutation.mutate(
            {
                enable: value,
                id: projectInstanceId,
                workflowId: workflow.id!,
            },
            {
                onSuccess: () => {
                    projectInstanceWorkflow.enabled = !projectInstanceWorkflow?.enabled;
                },
            }
        );
    };

    return (
        <li className="flex items-center justify-between rounded-md px-2 py-1 hover:bg-gray-50">
            <div className="flex flex-1 cursor-pointer items-center" onClick={handleWorkflowClick}>
                <span
                    className={twMerge(
                        'w-80 text-sm font-semibold',
                        !projectInstanceWorkflow.enabled && 'text-muted-foreground'
                    )}
                >
                    {workflow.label}
                </span>

                <div className="ml-6 flex space-x-1">
                    {filteredComponentNames?.map((name) => {
                        const componentDefinition = workflowComponentDefinitions[name];
                        const taskDispatcherDefinition = workflowTaskDispatcherDefinitions[name];

                        return (
                            <div className="flex items-center justify-center rounded-full border-2 p-1" key={name}>
                                <Tooltip>
                                    <TooltipTrigger>
                                        <InlineSVG
                                            className="size-5"
                                            key={name}
                                            loader={<Component1Icon className="size-5 flex-none text-gray-900" />}
                                            src={
                                                componentDefinition?.icon
                                                    ? componentDefinition?.icon
                                                    : (taskDispatcherDefinition?.icon ?? '')
                                            }
                                        />
                                    </TooltipTrigger>

                                    <TooltipContent side="top">{componentDefinition?.title}</TooltipContent>
                                </Tooltip>
                            </div>
                        );
                    })}
                </div>
            </div>

            <div className="flex items-center justify-end gap-x-6">
                {projectInstanceWorkflow?.lastExecutionDate ? (
                    <Tooltip>
                        <TooltipTrigger className="flex items-center text-sm text-gray-500">
                            <span className="text-xs">
                                {`Executed at ${projectInstanceWorkflow.lastExecutionDate?.toLocaleDateString()} ${projectInstanceWorkflow.lastExecutionDate?.toLocaleTimeString()}`}
                            </span>
                        </TooltipTrigger>

                        <TooltipContent>Last Execution Date</TooltipContent>
                    </Tooltip>
                ) : (
                    <span className="text-xs">No executions</span>
                )}

                {projectInstanceWorkflow && (
                    <div className="flex items-center gap-x-4">
                        {(!workflow.triggers?.length || workflow.triggers?.[0]?.type.includes('manual')) && (
                            <Button
                                disabled={!projectInstanceEnabled || !projectInstanceWorkflow.enabled}
                                onClick={handleRunWorkflowClick}
                                size="icon"
                                variant="ghost"
                            >
                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <PlayIcon className="h-5 text-success" />
                                    </TooltipTrigger>

                                    <TooltipContent>Run workflow manually</TooltipContent>
                                </Tooltip>
                            </Button>
                        )}

                        {projectInstanceWorkflow.staticWebhookUrl && (
                            <Button
                                disabled={!projectInstanceWorkflow.enabled}
                                onClick={() => copyToClipboard(projectInstanceWorkflow.staticWebhookUrl!)}
                                size="icon"
                                variant="ghost"
                            >
                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <ClipboardIcon className="h-5" />
                                    </TooltipTrigger>

                                    <TooltipContent>Copy static workflow webhook trigger url</TooltipContent>
                                </Tooltip>
                            </Button>
                        )}

                        <div className="relative flex items-center">
                            {enableProjectInstanceWorkflowMutation.isPending && (
                                <LoadingIcon className="absolute left-[-15px] top-[3px]" />
                            )}

                            <Switch
                                checked={projectInstanceWorkflow.enabled}
                                className="mr-2"
                                disabled={enableProjectInstanceWorkflowMutation.isPending}
                                onCheckedChange={handleEnableProjectInstanceWorkflow}
                                onClick={(event) => event.stopPropagation()}
                            />
                        </div>

                        <ProjectInstanceWorkflowListItemDropdownMenu
                            onEditClick={() => setShowEditWorkflowDialog(true)}
                            workflow={workflow}
                        />
                    </div>
                )}
            </div>

            {showEditWorkflowDialog && projectInstanceWorkflow && (
                <ProjectInstanceEditWorkflowDialog
                    onClose={() => setShowEditWorkflowDialog(false)}
                    projectInstanceWorkflow={projectInstanceWorkflow}
                    workflow={workflow}
                />
            )}
        </li>
    );
};

export default ProjectInstanceWorkflowListItem;
