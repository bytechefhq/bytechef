import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import {Switch} from '@/components/ui/switch';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useToast} from '@/hooks/use-toast';
import ProjectDeploymentEditWorkflowDialog from '@/pages/automation/project-deployments/components/ProjectDeploymentEditWorkflowDialog';
import ProjectDeploymentWorkflowListItemDropdownMenu from '@/pages/automation/project-deployments/components/project-deployment-workflow-list/ProjectDeploymentWorkflowListItemDropdownMenu';
import {getPageUrl} from '@/pages/automation/project-deployments/components/project-deployment-workflow-list/util/pageUrl-utils';
import WorkflowComponentsList from '@/shared/components/WorkflowComponentsList';
import useReadOnlyWorkflow from '@/shared/components/read-only-workflow-editor/hooks/useReadOnlyWorkflow';
import {ProjectDeploymentApi, ProjectDeploymentWorkflow, Workflow} from '@/shared/middleware/automation/configuration';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useEnableProjectDeploymentWorkflowMutation} from '@/shared/mutations/automation/projectDeploymentWorkflows.mutations';
import {ProjectDeploymentKeys} from '@/shared/queries/automation/projectDeployments.queries';
import {useQueryClient} from '@tanstack/react-query';
import {useCopyToClipboard} from '@uidotdev/usehooks';
import {ClipboardIcon, FormIcon, MessageCircleMoreIcon, PlayIcon} from 'lucide-react';
import {MouseEvent, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';

const projectDeploymentApi = new ProjectDeploymentApi();

const ProjectDeploymentWorkflowListItem = ({
    environmentId,
    filteredComponentNames,
    projectDeploymentEnabled,
    projectDeploymentId,
    projectDeploymentWorkflow,
    workflow,
    workflowComponentDefinitions,
    workflowTaskDispatcherDefinitions,
}: {
    environmentId: number;
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

    const {openReadOnlyWorkflowSheet} = useReadOnlyWorkflow();

    /* eslint-disable @typescript-eslint/no-unused-vars */
    const [_, copyToClipboard] = useCopyToClipboard();
    const navigate = useNavigate();
    const {toast} = useToast();
    const queryClient = useQueryClient();

    const formTrigger =
        workflow.triggers && workflow.triggers.findIndex((trigger) => trigger.type.includes('form/')) !== -1;

    const formTriggerPageUrl = getPageUrl('form', environmentId, projectDeploymentWorkflow.staticWebhookUrl);

    const hostedChatTrigger =
        workflow.triggers &&
        workflow.triggers.findIndex((trigger) => trigger.type.includes('chat/')) !== -1 &&
        (workflow.triggers?.[0]?.parameters?.mode ?? 1) === 1;

    const hostedChatTriggerPageUrl = getPageUrl('chat', undefined, projectDeploymentWorkflow.staticWebhookUrl);

    const enableProjectDeploymentWorkflowMutation = useEnableProjectDeploymentWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ProjectDeploymentKeys.projectDeployments,
            });
        },
    });

    const handleWorkflowClick = () => {
        if (workflow) {
            openReadOnlyWorkflowSheet(workflow);
        }
    };

    const handleRunWorkflowClick = (event: MouseEvent<HTMLButtonElement>) => {
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
                    <WorkflowComponentsList
                        filteredComponentNames={filteredComponentNames || []}
                        workflowComponentDefinitions={workflowComponentDefinitions}
                        workflowTaskDispatcherDefinitions={workflowTaskDispatcherDefinitions}
                    />
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
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <Button
                                        disabled={!projectDeploymentEnabled || !projectDeploymentWorkflow.enabled}
                                        icon={<PlayIcon className="text-success" />}
                                        onClick={handleRunWorkflowClick}
                                        size="icon"
                                        variant="ghost"
                                    />
                                </TooltipTrigger>

                                <TooltipContent>Run workflow manually</TooltipContent>
                            </Tooltip>
                        )}

                        {!hostedChatTrigger && !formTrigger && projectDeploymentWorkflow.staticWebhookUrl && (
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <Button
                                        disabled={!projectDeploymentWorkflow.enabled}
                                        icon={<ClipboardIcon />}
                                        onClick={() =>
                                            copyToClipboard(
                                                projectDeploymentWorkflow.staticWebhookUrl! +
                                                    (workflow.sseStreamResponse ? '/sse' : '')
                                            )
                                        }
                                        size="icon"
                                        variant="ghost"
                                    />
                                </TooltipTrigger>

                                <TooltipContent>Copy static workflow webhook trigger url</TooltipContent>
                            </Tooltip>
                        )}

                        {formTrigger && projectDeploymentWorkflow.staticWebhookUrl && (
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <Button
                                        disabled={!projectDeploymentWorkflow.enabled}
                                        icon={<FormIcon />}
                                        onClick={() => navigate(formTriggerPageUrl)}
                                        size="icon"
                                        variant="ghost"
                                    />
                                </TooltipTrigger>

                                <TooltipContent>Click to open a form</TooltipContent>
                            </Tooltip>
                        )}

                        {hostedChatTrigger && projectDeploymentWorkflow.staticWebhookUrl && (
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <Button
                                        disabled={!projectDeploymentWorkflow.enabled}
                                        icon={<MessageCircleMoreIcon />}
                                        onClick={() => window.open(hostedChatTriggerPageUrl, '_self')}
                                        size="icon"
                                        variant="ghost"
                                    />
                                </TooltipTrigger>

                                <TooltipContent>Open hosted chat (URL is not copied)</TooltipContent>
                            </Tooltip>
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
