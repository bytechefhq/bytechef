import AlertDialog from '@/components/AlertDialog';
import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import Switch from '@/components/Switch/Switch';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {TURNED_OFF_BY_ADMINISTRATOR_REASON} from '@/ee/pages/embedded/automation-hub/utils/attentionReason';
import useAutomationExecutionsSheetStore from '@/ee/pages/embedded/automations/stores/useAutomationExecutionsSheetStore';
import ConnectedUserSheetDeleteDropdownMenu from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetDeleteDropdownMenu';
import ConnectedUserProjectWorkflowEditDialog from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/connected-user-project-workflow-list/ConnectedUserProjectWorkflowEditDialog';
import {WorkflowComponentIconDefinitionType} from '@/pages/automation/project/components/projects-sidebar/components/WorkflowComponentsIcon';
import isCollapsibleRowClick from '@/shared/components/mcp-server/utils/isCollapsibleRowClick';
import WorkflowTriggerAndComponentsRow from '@/shared/components/workflow/WorkflowTriggerAndComponentsRow';
import {
    AutomationProjectsQuery,
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
    AutomationProjectsQuery['automationProjects'][number]['workflowTemplates'][number];

export interface ConnectedUserWorkflowTemplateSourceI {
    projectId: string;
    projectName: string;
    workflowTemplate: ConnectedUserWorkflowTemplateType;
}

const ConnectedUserProjectWorkflowListItem = ({
    configuration,
    connectedUserProjectWorkflow,
    projectName,
    workflowTemplate,
}: {
    configuration?: {configurationEnabled?: boolean; configurationId: string; projectId: string};
    connectedUserProjectWorkflow: ConnectedUserProjectWorkflowType;
    projectName?: string;
    workflowTemplate?: ConnectedUserWorkflowTemplateType;
}) => {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showEditDialog, setShowEditDialog] = useState(false);

    const openAutomationExecutionsSheet = useAutomationExecutionsSheetStore(
        (state) => state.openAutomationExecutionsSheet
    );
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    /* eslint-disable @typescript-eslint/no-unused-vars */
    const [_, copyToClipboard] = useCopyToClipboard();
    const queryClient = useQueryClient();

    const deleteConnectedUserProjectWorkflowMutation = useDeleteConnectedUserProjectWorkflowMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['connectedUserProjects']});

            setShowDeleteDialog(false);
        },
    });

    const enableConnectedUserProjectWorkflowMutation = useEnableConnectedUserProjectWorkflowMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['connectedUserProjects']});
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

    const triggerSource = useMemo(() => {
        const workflowTriggers = connectedUserProjectWorkflow.workflow.triggers;
        const templateTriggers = workflowTemplate?.triggers ?? [];

        return {
            triggers:
                workflowTriggers.length > 0
                    ? workflowTriggers.map((trigger) => ({
                          label:
                              templateTriggers.find((templateTrigger) => templateTrigger.name === trigger.name)
                                  ?.title ?? undefined,
                          type: trigger.type,
                      }))
                    : templateTriggers.map((templateTrigger) => ({label: templateTrigger.title ?? undefined})),
            workflowTriggerComponentNames: templateTriggers.map((trigger) => trigger.name),
        };
    }, [connectedUserProjectWorkflow.workflow.triggers, workflowTemplate]);

    const lastExecutionDate = connectedUserProjectWorkflow.lastExecutionDate
        ? new Date(Date.parse(connectedUserProjectWorkflow.lastExecutionDate))
        : undefined;

    const provisioned = connectedUserProjectWorkflow.automationUuid != null;

    const isDraft = !provisioned && connectedUserProjectWorkflow.workflowVersion == null;

    // An unpublished copy has no deployment to switch on; the server rejects enabling it until it is published
    const enableDisabled = isDraft && !connectedUserProjectWorkflow.enabled;

    const turnedOffByAdministrator =
        connectedUserProjectWorkflow.attentionReason === TURNED_OFF_BY_ADMINISTRATOR_REASON;

    const enableTooltip = useMemo(() => {
        if (turnedOffByAdministrator) {
            return configuration?.configurationEnabled === false
                ? 'The configuration is disabled. This workflow runs again when the configuration is enabled.'
                : 'This workflow is disabled in the configuration. It runs again when it is enabled there.';
        }

        return enableDisabled ? 'Publish the workflow before enabling it' : undefined;
    }, [configuration?.configurationEnabled, enableDisabled, turnedOffByAdministrator]);

    const versionLabel = useMemo(() => {
        if (provisioned) {
            return undefined;
        }

        return isDraft ? 'V1 DRAFT' : `V${connectedUserProjectWorkflow.workflowVersion}`;
    }, [connectedUserProjectWorkflow.workflowVersion, isDraft, provisioned]);

    const {automationUuid, staticWebhookUrl} = connectedUserProjectWorkflow;

    const editable = !!automationUuid && !!configuration;

    const handleWorkflowClick = () => {
        openAutomationExecutionsSheet({
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
            <li>
                <div
                    className="flex min-h-12 cursor-pointer items-center justify-between gap-x-4 rounded-md px-3 py-1 hover:bg-surface-neutral-primary-hover"
                    onClick={(event) => {
                        if (isCollapsibleRowClick(event)) {
                            handleWorkflowClick();
                        }
                    }}
                    role="presentation"
                >
                    <div className="flex min-w-0 flex-1 items-center gap-2">
                        <div className="flex w-80 min-w-0 shrink-0 flex-col gap-y-1 pr-1">
                            <div className="flex min-w-0 items-center gap-2">
                                <button
                                    className="min-w-0 cursor-pointer truncate text-left text-sm font-semibold"
                                    onClick={handleWorkflowClick}
                                    type="button"
                                >
                                    {connectedUserProjectWorkflow.workflow.label}
                                </button>
                            </div>

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
                                fitToWidth
                                workflow={triggerSource}
                                workflowComponentDefinitions={workflowComponentDefinitions}
                                workflowTaskDispatcherDefinitions={workflowComponentDefinitions}
                            />
                        )}
                    </div>

                    <div className="flex items-center gap-x-2">
                        <div className="flex items-center gap-x-2">
                            <div className="flex w-28 shrink-0 justify-start">
                                {versionLabel && (
                                    <Tooltip>
                                        <TooltipTrigger asChild>
                                            <Badge
                                                label={versionLabel}
                                                styleType="secondary-filled"
                                                weight="semibold"
                                            />
                                        </TooltipTrigger>

                                        <TooltipContent>The workflow version</TooltipContent>
                                    </Tooltip>
                                )}
                            </div>

                            <div className="flex w-72 shrink-0 items-center justify-end gap-x-4">
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
                                                checked={
                                                    connectedUserProjectWorkflow.enabled && !turnedOffByAdministrator
                                                }
                                                disabled={enableDisabled || turnedOffByAdministrator}
                                                onCheckedChange={(value) => {
                                                    enableConnectedUserProjectWorkflowMutation.mutate({
                                                        enable: value,
                                                        id: connectedUserProjectWorkflow.id,
                                                    });
                                                }}
                                            />
                                        </span>
                                    </TooltipTrigger>

                                    {enableTooltip && <TooltipContent side="left">{enableTooltip}</TooltipContent>}
                                </Tooltip>
                            </div>
                        </div>

                        <ConnectedUserSheetDeleteDropdownMenu
                            onDeleteClick={() => setShowDeleteDialog(true)}
                            onEditClick={editable ? () => setShowEditDialog(true) : undefined}
                        />
                    </div>
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

            {showEditDialog && automationUuid && configuration && (
                <ConnectedUserProjectWorkflowEditDialog
                    configurationId={configuration.configurationId}
                    connectedUserId={Number(connectedUserProjectWorkflow.connectedUserId)}
                    connectedUserProjectWorkflowId={connectedUserProjectWorkflow.id}
                    onClose={() => setShowEditDialog(false)}
                    projectId={configuration.projectId}
                    workflowLabel={connectedUserProjectWorkflow.workflow.label}
                    workflowUuid={automationUuid}
                />
            )}
        </>
    );
};

export default ConnectedUserProjectWorkflowListItem;
