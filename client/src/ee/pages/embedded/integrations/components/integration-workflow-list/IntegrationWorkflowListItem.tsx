import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import IntegrationWorkflowPermissionExpressionField from '@/ee/pages/embedded/integrations/components/integration-workflow-list/IntegrationWorkflowPermissionExpressionField';
import {Integration, Workflow} from '@/ee/shared/middleware/embedded/configuration';
import {useDeleteWorkflowMutation, useUpdateWorkflowMutation} from '@/ee/shared/mutations/embedded/workflows.mutations';
import {IntegrationWorkflowKeys} from '@/ee/shared/queries/embedded/integrationWorkflows.queries';
import {IntegrationKeys} from '@/ee/shared/queries/embedded/integrations.queries';
import {WorkflowKeys, useGetWorkflowQuery} from '@/ee/shared/queries/embedded/workflows.queries';
import DeleteWorkflowAlertDialog from '@/shared/components/DeleteWorkflowAlertDialog';
import WorkflowDialog from '@/shared/components/workflow/WorkflowDialog';
import WorkflowListRowDropdownMenu from '@/shared/components/workflow/WorkflowListRowDropdownMenu';
import WorkflowTriggerAndComponentsRow from '@/shared/components/workflow/WorkflowTriggerAndComponentsRow';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {UNION_ENVIRONMENT_ID, useHasEmbeddedScope} from '@/shared/hooks/useHasEmbeddedScope';
import {
    useIntegrationWorkflowsByIntegrationIdQuery,
    useUpdateIntegrationWorkflowPermissionExpressionMutation,
} from '@/shared/middleware/graphql';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {WorkflowTestConfigurationKeys} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useRef, useState} from 'react';
import {Link, useSearchParams} from 'react-router-dom';

const IntegrationWorkflowListItem = ({
    filteredComponentNames,
    integration,
    workflow,
    workflowComponentDefinitions,
    workflowTaskDispatcherDefinitions,
}: {
    filteredComponentNames?: string[];
    integration: Integration;
    workflow: Workflow;
    workflowComponentDefinitions: {
        [key: string]: ComponentDefinitionBasic | undefined;
    };
    workflowTaskDispatcherDefinitions: {
        [key: string]: ComponentDefinitionBasic | undefined;
    };
}) => {
    const [isPermissionExpressionSeeded, setIsPermissionExpressionSeeded] = useState(false);
    const [permissionExpression, setPermissionExpression] = useState<string | null>('');
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showEditDialog, setShowEditDialog] = useState(false);

    const pendingPermissionExpressionRef = useRef<string | null | undefined>(undefined);

    const [searchParams] = useSearchParams();

    const queryClient = useQueryClient();

    const canDeleteWorkflow = useHasEmbeddedScope('WORKFLOW_DELETE', DEVELOPMENT_ENVIRONMENT);
    const canEditWorkflow = useHasEmbeddedScope('WORKFLOW_EDIT', DEVELOPMENT_ENVIRONMENT);
    const canManageIntegrationSettings = useHasEmbeddedScope('INTEGRATION_SETTINGS', UNION_ENVIRONMENT_ID);

    const permissionExpressionEditable = workflow.integrationWorkflowId != null && canManageIntegrationSettings;

    const integrationWorkflowsQueryVariables = {integrationId: String(integration.id)};

    const updateIntegrationWorkflowPermissionExpressionMutation =
        useUpdateIntegrationWorkflowPermissionExpressionMutation({
            onSuccess: () => {
                queryClient.invalidateQueries({
                    queryKey: ['integrationWorkflowsByIntegrationId', integrationWorkflowsQueryVariables],
                });
            },
        });

    const {data: integrationWorkflowsData, isFetching: isIntegrationWorkflowsFetching} =
        useIntegrationWorkflowsByIntegrationIdQuery(integrationWorkflowsQueryVariables, {
            enabled: showEditDialog && permissionExpressionEditable && integration.id != null,
        });

    const integrationWorkflowPermissionExpression = integrationWorkflowsData?.integrationWorkflowsByIntegrationId.find(
        (integrationWorkflow) => integrationWorkflow.integrationWorkflowId === String(workflow.integrationWorkflowId)
    )?.permissionExpression;

    const deleteWorkflowMutation = useDeleteWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: IntegrationKeys.integrations});

            setShowDeleteDialog(false);
        },
    });

    const updateWorkflowMutation = useUpdateWorkflowMutation({
        onSuccess: () => {
            const pendingPermissionExpression = pendingPermissionExpressionRef.current;

            pendingPermissionExpressionRef.current = undefined;

            if (pendingPermissionExpression !== undefined) {
                updateIntegrationWorkflowPermissionExpressionMutation.mutate({
                    integrationWorkflowId: String(workflow.integrationWorkflowId),
                    permissionExpression: pendingPermissionExpression,
                });
            }

            queryClient.invalidateQueries({
                queryKey: IntegrationWorkflowKeys.integrationWorkflows(integration.id!),
            });

            void queryClient.invalidateQueries({
                queryKey: WorkflowTestConfigurationKeys.workflowTestConfiguration(workflow.id!),
            });

            queryClient.invalidateQueries({
                queryKey: WorkflowKeys.workflow(workflow.id!),
            });

            setShowEditDialog(false);
        },
    });

    useEffect(() => {
        if (
            !showEditDialog ||
            !permissionExpressionEditable ||
            isPermissionExpressionSeeded ||
            !integrationWorkflowsData ||
            isIntegrationWorkflowsFetching
        ) {
            return;
        }

        setPermissionExpression(integrationWorkflowPermissionExpression ?? '');
        setIsPermissionExpressionSeeded(true);
    }, [
        integrationWorkflowPermissionExpression,
        integrationWorkflowsData,
        isIntegrationWorkflowsFetching,
        isPermissionExpressionSeeded,
        permissionExpressionEditable,
        showEditDialog,
    ]);

    return (
        <li
            className="flex items-center justify-between rounded-md px-3 py-1 hover:bg-surface-neutral-primary-hover"
            key={workflow.id}
        >
            <Link
                className="flex flex-1 items-center"
                to={`/embedded/integrations/${integration.id}/integration-workflows/${workflow.integrationWorkflowId}?${searchParams}`}
            >
                <div className="w-80 text-sm font-semibold">{workflow.label}</div>

                <WorkflowTriggerAndComponentsRow
                    className="hidden sm:flex"
                    filteredComponentNames={filteredComponentNames}
                    workflow={workflow}
                    workflowComponentDefinitions={workflowComponentDefinitions}
                    workflowTaskDispatcherDefinitions={workflowTaskDispatcherDefinitions}
                />
            </Link>

            <div className="flex justify-end gap-x-6">
                <Tooltip>
                    <TooltipTrigger className="flex items-center text-sm text-content-neutral-secondary">
                        <span className="text-xs">
                            {`Modified at ${workflow.lastModifiedDate?.toLocaleDateString()} ${workflow.lastModifiedDate?.toLocaleTimeString()}`}
                        </span>
                    </TooltipTrigger>

                    <TooltipContent>Last Modified Date</TooltipContent>
                </Tooltip>

                <WorkflowListRowDropdownMenu
                    exportUrl={`/api/embedded/internal/workflows/${workflow.id}/export`}
                    onDeleteClick={canDeleteWorkflow ? () => setShowDeleteDialog(true) : undefined}
                    onEditClick={
                        canEditWorkflow
                            ? () => {
                                  setIsPermissionExpressionSeeded(false);
                                  setShowEditDialog(true);
                              }
                            : undefined
                    }
                />
            </div>

            {showDeleteDialog && (
                <DeleteWorkflowAlertDialog
                    isPending={deleteWorkflowMutation.isPending}
                    onClose={() => setShowDeleteDialog(false)}
                    onDelete={() => {
                        if (workflow?.id) {
                            deleteWorkflowMutation.mutate({
                                id: workflow?.id,
                            });
                        }
                    }}
                />
            )}

            {showEditDialog && workflow && (
                <WorkflowDialog
                    additionalContent={
                        permissionExpressionEditable &&
                        isPermissionExpressionSeeded && (
                            <IntegrationWorkflowPermissionExpressionField
                                onChange={setPermissionExpression}
                                value={permissionExpression}
                            />
                        )
                    }
                    onClose={() => setShowEditDialog(false)}
                    onSave={() => {
                        pendingPermissionExpressionRef.current = permissionExpressionEditable
                            ? permissionExpression?.trim() || null
                            : undefined;

                        void queryClient.invalidateQueries({
                            queryKey: IntegrationWorkflowKeys.integrationWorkflow(
                                integration.id!,
                                Number.parseInt(workflow.id!, 10)
                            ),
                        });
                    }}
                    parentId={integration.id}
                    saveDisabled={permissionExpressionEditable && !isPermissionExpressionSeeded}
                    updateWorkflowMutation={updateWorkflowMutation}
                    useGetWorkflowQuery={useGetWorkflowQuery}
                    workflowId={workflow.id!}
                />
            )}
        </li>
    );
};

export default IntegrationWorkflowListItem;
