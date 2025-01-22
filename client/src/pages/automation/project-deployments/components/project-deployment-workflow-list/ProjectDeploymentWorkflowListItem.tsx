import LoadingIcon from '@/components/LoadingIcon';
import {Button} from '@/components/ui/button';
import {Switch} from '@/components/ui/switch';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useToast} from '@/hooks/use-toast';
import ProjectDeploymentEditWorkflowDialog from '@/pages/automation/project-deployments/components/ProjectDeploymentEditWorkflowDialog';
import ProjectDeploymentWorkflowListItemDropdownMenu from '@/pages/automation/project-deployments/components/project-deployment-workflow-list/ProjectDeploymentWorkflowListItemDropdownMenu';
import useReadOnlyWorkflowEditorSheetStore from '@/shared/components/read-only-workflow-editor/stores/useReadOnlyWorkflowEditorSheetStore';
import {
    Environment,
    ProjectDeploymentApi,
    ProjectDeploymentWorkflow,
    Workflow,
} from '@/shared/middleware/automation/configuration';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useEnableProjectDeploymentWorkflowMutation} from '@/shared/mutations/automation/projectDeploymentWorkflows.mutations';
import {ProjectDeploymentKeys} from '@/shared/queries/automation/projectDeployments.queries';
import {Component1Icon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {useCopyToClipboard} from '@uidotdev/usehooks';
import {ClipboardIcon, MessageCircleMoreIcon, PlayIcon} from 'lucide-react';
import {useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {twMerge} from 'tailwind-merge';

const projectDeploymentApi = new ProjectDeploymentApi();

const ProjectDeploymentWorkflowListItem = ({
    environment,
    filteredComponentNames,
    projectDeploymentEnabled,
    projectDeploymentId,
    projectDeploymentWorkflow,
    workflow,
    workflowComponentDefinitions,
    workflowTaskDispatcherDefinitions,
}: {
    environment?: Environment;
    filteredComponentNames?: string[];
    projectDeploymentEnabled: boolean;
    projectDeploymentId: number;
    projectDeploymentWorkflow: ProjectDeploymentWorkflow;
    workflow: Workflow;
    workflowComponentDefinitions: {
        [key: string]: ComponentDefinitionBasic | undefined;
    };
    workflowTaskDispatcherDefinitions: {
        [key: string]: ComponentDefinitionBasic | undefined;
    };
}) => {
    const [showEditWorkflowDialog, setShowEditWorkflowDialog] = useState(false);

    const {setReadOnlyWorkflowEditorSheetOpen, setWorkflowId} = useReadOnlyWorkflowEditorSheetStore();

    /* eslint-disable @typescript-eslint/no-unused-vars */
    const [_, copyToClipboard] = useCopyToClipboard();
    const {toast} = useToast();

    const queryClient = useQueryClient();

    const enableProjectDeploymentWorkflowMutation = useEnableProjectDeploymentWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ProjectDeploymentKeys.projectDeployments,
            });
        },
    });

    const hostedChatTrigger =
        workflow.triggers &&
        workflow.triggers.findIndex((trigger) => trigger.type.includes('chat/')) !== -1 &&
        (workflow.triggers?.[0]?.parameters?.mode ?? 1) === 1;

    const handleWorkflowClick = () => {
        setWorkflowId(workflow.id!);
        setReadOnlyWorkflowEditorSheetOpen(true);
    };

    const handleRunWorkflowClick = (event: React.MouseEvent<HTMLButtonElement>) => {
        event?.stopPropagation();

        projectDeploymentApi
            .createProjectDeploymentWorkflowJob({
                id: projectDeploymentId,
                workflowId: workflow.id!,
            })
            .then(() =>
                toast({
                    description: 'Workflow request sent.',
                })
            );
    };

    const handleEnableProjectDeploymentWorkflow = (value: boolean) => {
        enableProjectDeploymentWorkflowMutation.mutate(
            {
                enable: value,
                id: projectDeploymentId,
                workflowId: workflow.id!,
            },
            {
                onSuccess: () => {
                    projectDeploymentWorkflow.enabled = !projectDeploymentWorkflow?.enabled;
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
                        !projectDeploymentWorkflow.enabled && 'text-muted-foreground'
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

            <div className="flex items-center gap-x-4">
                {projectDeploymentWorkflow?.lastExecutionDate ? (
                    <Tooltip>
                        <TooltipTrigger className="flex items-center text-sm text-gray-500">
                            <span className="text-xs">
                                {`Executed at ${projectDeploymentWorkflow.lastExecutionDate?.toLocaleDateString()} ${projectDeploymentWorkflow.lastExecutionDate?.toLocaleTimeString()}`}
                            </span>
                        </TooltipTrigger>

                        <TooltipContent>Last Execution Date</TooltipContent>
                    </Tooltip>
                ) : (
                    <span className="text-xs">No executions</span>
                )}

                <div className="flex items-center gap-x-6">
                    <div className="min-w-[36px]">
                        {(!workflow.triggers?.length || workflow.triggers?.[0]?.type.includes('manual')) && (
                            <Button
                                disabled={!projectDeploymentEnabled || !projectDeploymentWorkflow.enabled}
                                onClick={handleRunWorkflowClick}
                                size="icon"
                                variant="ghost"
                            >
                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <PlayIcon className="text-success" />
                                    </TooltipTrigger>

                                    <TooltipContent>Run workflow manually</TooltipContent>
                                </Tooltip>
                            </Button>
                        )}

                        {!hostedChatTrigger && projectDeploymentWorkflow.staticWebhookUrl && (
                            <Button
                                disabled={!projectDeploymentWorkflow.enabled}
                                onClick={() => copyToClipboard(projectDeploymentWorkflow.staticWebhookUrl!)}
                                size="icon"
                                variant="ghost"
                            >
                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <ClipboardIcon />
                                    </TooltipTrigger>

                                    <TooltipContent>Copy static workflow webhook trigger url</TooltipContent>
                                </Tooltip>
                            </Button>
                        )}

                        {hostedChatTrigger && projectDeploymentWorkflow.staticWebhookUrl && (
                            <Button
                                disabled={!projectDeploymentWorkflow.enabled}
                                onClick={() =>
                                    window.open(
                                        `/chat/${environment === Environment.Production ? '' : 'test/'}` +
                                            projectDeploymentWorkflow.staticWebhookUrl?.substring(
                                                projectDeploymentWorkflow.staticWebhookUrl?.lastIndexOf('/webhooks/') +
                                                    10,
                                                projectDeploymentWorkflow.staticWebhookUrl?.length
                                            ),
                                        '_blank'
                                    )
                                }
                                size="icon"
                                variant="ghost"
                            >
                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        {hostedChatTrigger ? <MessageCircleMoreIcon /> : <ClipboardIcon />}
                                    </TooltipTrigger>

                                    <TooltipContent>Copy static workflow webhook trigger url</TooltipContent>
                                </Tooltip>
                            </Button>
                        )}
                    </div>

                    <div className="relative flex items-center">
                        {enableProjectDeploymentWorkflowMutation.isPending && (
                            <LoadingIcon className="absolute left-[-15px] top-[3px]" />
                        )}

                        <Switch
                            checked={projectDeploymentWorkflow.enabled}
                            className="mr-2"
                            disabled={enableProjectDeploymentWorkflowMutation.isPending}
                            onCheckedChange={handleEnableProjectDeploymentWorkflow}
                            onClick={(event) => event.stopPropagation()}
                        />
                    </div>
                </div>

                <ProjectDeploymentWorkflowListItemDropdownMenu
                    onEditClick={() => setShowEditWorkflowDialog(true)}
                    workflow={workflow}
                />
            </div>

            {showEditWorkflowDialog && projectDeploymentWorkflow && (
                <ProjectDeploymentEditWorkflowDialog
                    onClose={() => setShowEditWorkflowDialog(false)}
                    projectDeploymentWorkflow={projectDeploymentWorkflow}
                    workflow={workflow}
                />
            )}
        </li>
    );
};

export default ProjectDeploymentWorkflowListItem;
