import {Button} from '@/components/ui/button';
import {Switch} from '@/components/ui/switch';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useToast} from '@/components/ui/use-toast';
import {ProjectInstanceApi, ProjectInstanceWorkflowModel, WorkflowModel} from '@/middleware/automation/configuration';
import {ComponentDefinitionBasicModel} from '@/middleware/platform/configuration';
import {useEnableProjectInstanceWorkflowMutation} from '@/mutations/automation/projectInstanceWorkflows.mutations';
import ProjectInstanceEditWorkflowDialog from '@/pages/automation/project-instances/components/ProjectInstanceEditWorkflowDialog';
import ProjectInstanceWorkflowListItemDropdownMenu from '@/pages/automation/project-instances/components/ProjectInstanceWorkflowListItemDropdownMenu';
import useProjectInstanceWorkflowSheetStore from '@/pages/automation/project-instances/stores/useProjectInstanceWorkflowSheetStore';
import {ProjectInstanceKeys} from '@/queries/automation/projectInstances.queries';
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
    projectInstanceWorkflow: ProjectInstanceWorkflowModel;
    workflow: WorkflowModel;
    workflowComponentDefinitions: {
        [key: string]: ComponentDefinitionBasicModel | undefined;
    };
    workflowTaskDispatcherDefinitions: {
        [key: string]: ComponentDefinitionBasicModel | undefined;
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

    const handleWorkflowLabelClick = () => {
        setWorkflowId(workflow.id!);
        setProjectInstanceWorkflowSheetOpen(true);
    };

    const handleWorkflowRun = () => {
        projectInstanceApi
            .createProjectInstanceWorkflowJob({
                id: projectInstanceId,
                workflowId: workflow.id!,
            })
            .then(() => {
                toast({
                    description: 'Workflow request sent.',
                });
            });
    };

    return (
        <>
            <div className="flex flex-1 items-center">
                <div
                    className={twMerge(
                        'w-96 text-sm font-semibold',
                        !projectInstanceWorkflow.enabled && 'text-muted-foreground'
                    )}
                >
                    <button onClick={handleWorkflowLabelClick}>{workflow.label}</button>
                </div>

                <div className="ml-6 flex">
                    {filteredComponentNames?.map((name) => {
                        const componentDefinition = workflowComponentDefinitions[name];
                        const taskDispatcherDefinition = workflowTaskDispatcherDefinitions[name];

                        return (
                            <div className="mr-0.5 flex items-center justify-center rounded-full border p-1" key={name}>
                                <Tooltip>
                                    <TooltipTrigger>
                                        <InlineSVG
                                            className="size-5 flex-none"
                                            key={name}
                                            src={
                                                componentDefinition?.icon
                                                    ? componentDefinition?.icon
                                                    : taskDispatcherDefinition?.icon ?? ''
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
                            <span>
                                {`Executed at ${projectInstanceWorkflow.lastExecutionDate?.toLocaleDateString()} ${projectInstanceWorkflow.lastExecutionDate?.toLocaleTimeString()}`}
                            </span>
                        </TooltipTrigger>

                        <TooltipContent>Last Execution Date</TooltipContent>
                    </Tooltip>
                ) : (
                    '-'
                )}

                {projectInstanceWorkflow && (
                    <div className="flex items-center gap-x-4">
                        <Switch
                            checked={projectInstanceWorkflow.enabled}
                            disabled={projectInstanceEnabled}
                            onCheckedChange={(value) => {
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
                            }}
                        />

                        {(workflow.triggers?.length == 0 || workflow.triggers?.[0]?.name === 'manual') && (
                            <Button
                                disabled={!projectInstanceEnabled || !projectInstanceWorkflow.enabled}
                                onClick={() => handleWorkflowRun()}
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

                        {!(workflow.triggers?.length == 0 || workflow.triggers?.[0]?.name === 'manual') &&
                            !projectInstanceWorkflow.staticWebhookUrl && <div className="w-9"></div>}

                        <ProjectInstanceWorkflowListItemDropdownMenu
                            onEditClick={() => setShowEditWorkflowDialog(true)}
                            onEnableClick={() => handleProjectInstanceEnable()}
                            projectInstanceEnabled={projectInstanceEnabled}
                            projectInstanceWorkflowEnabled={projectInstanceWorkflow.enabled!}
                            workflow={workflow}
                        />
                    </div>
                )}
            </div>

            {showEditWorkflowDialog && projectInstanceWorkflow && (
                <ProjectInstanceEditWorkflowDialog
                    onClose={() => setShowEditWorkflowDialog(false)}
                    projectInstanceEnabled={projectInstanceEnabled}
                    projectInstanceWorkflow={projectInstanceWorkflow}
                    workflow={workflow}
                />
            )}
        </>
    );
};

export default ProjectInstanceWorkflowListItem;
