import AlertDialog from '@/components/AlertDialog';
import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import Switch from '@/components/Switch/Switch';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import useAutomationWorkflowExecutionsSheetStore from '@/ee/pages/embedded/automation-workflows/stores/useAutomationWorkflowExecutionsSheetStore';
import ConnectedUserSheetDeleteDropdownMenu from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetDeleteDropdownMenu';
import {WorkflowComponentIconDefinitionType} from '@/pages/automation/project/components/projects-sidebar/components/WorkflowComponentsIcon';
import WorkflowTriggerAndComponentsRow from '@/shared/components/workflow/WorkflowTriggerAndComponentsRow';
import {
    AutomationWorkflowProjectsQuery,
    ConnectedUserProjectsQuery,
    useDeleteConnectedUserProjectWorkflowMutation,
    useEnableConnectedUserProjectWorkflowMutation,
} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {useCopyToClipboard} from '@uidotdev/usehooks';
import {ClipboardIcon} from 'lucide-react';
import {useMemo, useState} from 'react';
import {toast} from 'sonner';

export type ConnectedUserProjectWorkflowType =
    ConnectedUserProjectsQuery['connectedUserProjects'][number]['connectedUserProjectWorkflows'][number];

export type ConnectedUserWorkflowTemplateType =
    AutomationWorkflowProjectsQuery['automationWorkflowProjects'][number]['workflowTemplates'][number];

export interface ConnectedUserWorkflowTemplateSourceI {
    projectName: string;
    workflowTemplate: ConnectedUserWorkflowTemplateType;
}

const ConnectedUserProjectWorkflowListItem = ({
    connectedUserProjectWorkflow,
    projectName,
    workflowTemplate,
}: {
    connectedUserProjectWorkflow: ConnectedUserProjectWorkflowType;
    projectName?: string;
    workflowTemplate?: ConnectedUserWorkflowTemplateType;
}) => {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    const openAutomationWorkflowExecutionsSheet = useAutomationWorkflowExecutionsSheetStore(
        (state) => state.openAutomationWorkflowExecutionsSheet
    );
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    /* eslint-disable @typescript-eslint/no-unused-vars */
    const [_, copyToClipboard] = useCopyToClipboard();
    const queryClient = useQueryClient();

    const deleteConnectedUserProjectWorkflowMutation = useDeleteConnectedUserProjectWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['connectedUserProjects']});

            setShowDeleteDialog(false);
        },
    });

    const enableConnectedUserProjectWorkflowMutation = useEnableConnectedUserProjectWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['connectedUserProjects']});
        },
    });

    const filteredComponentNames = useMemo(() => {
        const componentNames = [...(workflowTemplate?.triggers ?? []), ...(workflowTemplate?.components ?? [])].map(
            (component) => component.name
        );

        return componentNames.filter((componentName, index) => componentNames.indexOf(componentName) === index);
    }, [workflowTemplate]);

    const workflowComponentDefinitions = useMemo(
        () =>
            Object.fromEntries(
                [...(workflowTemplate?.triggers ?? []), ...(workflowTemplate?.components ?? [])].map((component) => [
                    component.name,
                    {icon: component.icon ?? undefined, name: component.name, title: component.title ?? undefined},
                ])
            ) as Record<string, WorkflowComponentIconDefinitionType>,
        [workflowTemplate]
    );

    const triggerSource = useMemo(
        () => ({
            triggers: connectedUserProjectWorkflow.workflow.triggers.map((trigger) => ({
                label:
                    workflowTemplate?.triggers.find((templateTrigger) => templateTrigger.name === trigger.name)
                        ?.title ?? undefined,
                type: trigger.type,
            })),
            workflowTriggerComponentNames: (workflowTemplate?.triggers ?? []).map((trigger) => trigger.name),
        }),
        [connectedUserProjectWorkflow.workflow.triggers, workflowTemplate]
    );

    const lastExecutionDate = connectedUserProjectWorkflow.lastExecutionDate
        ? new Date(Date.parse(connectedUserProjectWorkflow.lastExecutionDate))
        : undefined;

    const isDraft = connectedUserProjectWorkflow.workflowVersion == null;

    // An unpublished copy has no deployment to switch on; the server rejects enabling it until it is published
    const enableDisabled = isDraft && !connectedUserProjectWorkflow.enabled;

    const {staticWebhookUrl} = connectedUserProjectWorkflow;

    const handleWorkflowClick = () => {
        openAutomationWorkflowExecutionsSheet({
            connectedUserId: Number(connectedUserProjectWorkflow.connectedUserId),
            environmentId: currentEnvironmentId,
            projectName,
            workflow: {
                label: connectedUserProjectWorkflow.workflow.label,
                workflowId: connectedUserProjectWorkflow.workflow.id,
                workflowUuid: connectedUserProjectWorkflow.workflowUuid,
            },
        });
    };

    const handleCopyStaticWebhookUrlClick = async () => {
        if (staticWebhookUrl) {
            await copyToClipboard(staticWebhookUrl);

            toast('Webhook URL copied to clipboard');
        }
    };

    return (
        <>
            <li className="flex min-h-12 items-center justify-between rounded-md px-3 py-1 hover:bg-surface-neutral-primary-hover">
                <div className="flex min-w-0 flex-1 items-center gap-2">
                    <div className="flex w-80 min-w-0 shrink-0 flex-col gap-y-1 pr-1">
                        <button
                            className="cursor-pointer truncate text-left text-sm font-semibold"
                            onClick={handleWorkflowClick}
                            type="button"
                        >
                            {connectedUserProjectWorkflow.workflow.label}
                        </button>

                        {staticWebhookUrl && (
                            <div className="flex min-w-0 items-center gap-x-1">
                                <span
                                    className="truncate font-mono text-xs text-content-neutral-secondary"
                                    title={staticWebhookUrl}
                                >
                                    {staticWebhookUrl}
                                </span>

                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <Button
                                            aria-label="Copy webhook URL"
                                            icon={<ClipboardIcon />}
                                            onClick={handleCopyStaticWebhookUrlClick}
                                            size="iconXs"
                                            variant="ghost"
                                        />
                                    </TooltipTrigger>

                                    <TooltipContent>Copy static workflow webhook trigger url</TooltipContent>
                                </Tooltip>
                            </div>
                        )}
                    </div>

                    {workflowTemplate && (
                        <WorkflowTriggerAndComponentsRow
                            className="hidden sm:flex"
                            filteredComponentNames={filteredComponentNames}
                            workflow={triggerSource}
                            workflowComponentDefinitions={workflowComponentDefinitions}
                            workflowTaskDispatcherDefinitions={workflowComponentDefinitions}
                        />
                    )}
                </div>

                <div className="flex items-center gap-x-2">
                    <div className="flex items-center gap-x-4">
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Badge
                                    label={isDraft ? 'V1 DRAFT' : `V${connectedUserProjectWorkflow.workflowVersion}`}
                                    styleType="secondary-filled"
                                    weight="semibold"
                                />
                            </TooltipTrigger>

                            <TooltipContent>The workflow version</TooltipContent>
                        </Tooltip>

                        <div className="flex min-w-52 items-center justify-end gap-x-4">
                            <Tooltip>
                                <TooltipTrigger className="text-xs text-content-neutral-secondary">
                                    {lastExecutionDate
                                        ? `Executed at ${lastExecutionDate.toLocaleDateString()} ${lastExecutionDate.toLocaleTimeString()}`
                                        : 'No executions'}
                                </TooltipTrigger>

                                <TooltipContent side="left">Last Execution Date</TooltipContent>
                            </Tooltip>

                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <span className="relative flex items-center">
                                        {enableConnectedUserProjectWorkflowMutation.isPending && (
                                            <LoadingIcon className="absolute top-[3px] left-[-15px]" />
                                        )}

                                        <Switch
                                            aria-label={`Enable ${connectedUserProjectWorkflow.workflow.label}`}
                                            checked={connectedUserProjectWorkflow.enabled}
                                            disabled={enableDisabled}
                                            onCheckedChange={(value) => {
                                                enableConnectedUserProjectWorkflowMutation.mutate({
                                                    enable: value,
                                                    id: connectedUserProjectWorkflow.id,
                                                });
                                            }}
                                        />
                                    </span>
                                </TooltipTrigger>

                                {enableDisabled && (
                                    <TooltipContent side="left">Publish the workflow before enabling it</TooltipContent>
                                )}
                            </Tooltip>
                        </div>
                    </div>

                    <ConnectedUserSheetDeleteDropdownMenu onDeleteClick={() => setShowDeleteDialog(true)} />
                </div>
            </li>

            <AlertDialog
                isPending={deleteConnectedUserProjectWorkflowMutation.isPending}
                onCancel={() => setShowDeleteDialog(false)}
                onConfirm={() =>
                    deleteConnectedUserProjectWorkflowMutation.mutate({id: connectedUserProjectWorkflow.id})
                }
                open={showDeleteDialog}
            />
        </>
    );
};

export default ConnectedUserProjectWorkflowListItem;
