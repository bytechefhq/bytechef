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
import {
    useIntegrationWorkflowsByIntegrationIdQuery,
    useUpdateIntegrationWorkflowPermissionExpressionMutation,
} from '@/shared/middleware/graphql';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {WorkflowTestConfigurationKeys} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {isJavaCodeWorkflow} from '@/shared/util/codeWorkflowLanguage-utils';
import {useQueryClient} from '@tanstack/react-query';
import {MouseEvent, useEffect, useRef, useState} from 'react';
import {Link, useNavigate, useSearchParams} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';

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
    const javaCodeWorkflow = isJavaCodeWorkflow(integration.codeWorkflow, integration.codeWorkflowLanguage);

    const [permissionExpression, setPermissionExpression] = useState<string | null>('');
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showEditDialog, setShowEditDialog] = useState(false);

    const pendingPermissionExpressionRef = useRef<string | null | undefined>(undefined);

    const [searchParams] = useSearchParams();

    const navigate = useNavigate();

    const queryClient = useQueryClient();

    const integrationWorkflowsQueryVariables = {integrationId: String(integration.id)};

    const updateIntegrationWorkflowPermissionExpressionMutation =
        useUpdateIntegrationWorkflowPermissionExpressionMutation({
            onSuccess: () => {
                void queryClient.invalidateQueries({
                    queryKey: ['integrationWorkflowsByIntegrationId', integrationWorkflowsQueryVariables],
                });
            },
        });

    const {data: integrationWorkflowsData, isFetching: isIntegrationWorkflowsFetching} =
        useIntegrationWorkflowsByIntegrationIdQuery(integrationWorkflowsQueryVariables, {
            enabled: showEditDialog && integration.id != null,
        });

    const integrationWorkflowPermissionExpression = integrationWorkflowsData?.integrationWorkflowsByIntegrationId.find(
        (integrationWorkflow) => integrationWorkflow.integrationWorkflowId === String(workflow.integrationWorkflowId)
    )?.permissionExpression;

    const deleteWorkflowMutation = useDeleteWorkflowMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: IntegrationKeys.integrations});

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

            void queryClient.invalidateQueries({
                queryKey: IntegrationWorkflowKeys.integrationWorkflows(integration.id!),
            });

            void queryClient.invalidateQueries({
                queryKey: WorkflowTestConfigurationKeys.workflowTestConfiguration(workflow.id!),
            });

            void queryClient.invalidateQueries({
                queryKey: WorkflowKeys.workflow(workflow.id!),
            });

            setShowEditDialog(false);
        },
    });

    useEffect(() => {
        if (
            !showEditDialog ||
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
        showEditDialog,
    ]);

    const workflowUrl = `/embedded/integrations/${integration.id}/integration-workflows/${workflow.integrationWorkflowId}?${searchParams}`;

    const handleWorkflowListItemClick = (event: MouseEvent<HTMLLIElement>) => {
        if (javaCodeWorkflow) {
            return;
        }

        const target = event.target as HTMLElement;

        // Clicks inside portaled dialogs and menus bubble through React but are not DOM descendants of the row.
        if (!event.currentTarget.contains(target)) {
            return;
        }

        if (target.closest('a, button:not([data-slot="tooltip-trigger"]), [role="menu"], [role="menuitem"]')) {
            return;
        }

        void navigate(workflowUrl);
    };

    return (
        <li
            className={twMerge(
                'group/row flex items-center justify-between rounded-md py-1 pl-3 hover:bg-surface-neutral-primary-hover',
                !javaCodeWorkflow && 'cursor-pointer'
            )}
            key={workflow.id}
            onClick={handleWorkflowListItemClick}
            role="presentation"
        >
            <Link
                aria-disabled={javaCodeWorkflow || undefined}
                className={twMerge('flex flex-1 items-center', javaCodeWorkflow && 'pointer-events-none')}
                tabIndex={javaCodeWorkflow ? -1 : undefined}
                to={workflowUrl}
            >
                <div className="flex w-80 items-center gap-2 text-sm font-semibold">
                    <span className="underline-offset-4 group-hover/row:text-content-brand-primary group-hover/row:underline">
                        {workflow.label}
                    </span>
                </div>

                <WorkflowTriggerAndComponentsRow
                    className="hidden sm:flex"
                    filteredComponentNames={filteredComponentNames}
                    workflow={workflow}
                    workflowComponentDefinitions={workflowComponentDefinitions}
                    workflowTaskDispatcherDefinitions={workflowTaskDispatcherDefinitions}
                />
            </Link>

            <div className="flex justify-end gap-x-6 [&>:last-child]:-ml-3">
                <Tooltip>
                    <TooltipTrigger className="flex items-center text-sm text-content-neutral-secondary">
                        <span className="text-xs">
                            {`Modified at ${workflow.lastModifiedDate?.toLocaleDateString()} ${workflow.lastModifiedDate?.toLocaleTimeString()}`}
                        </span>
                    </TooltipTrigger>

                    <TooltipContent>Last Modified Date</TooltipContent>
                </Tooltip>

                {integration.codeWorkflow ? (
                    <div aria-hidden className="size-9 shrink-0" />
                ) : (
                    <WorkflowListRowDropdownMenu
                        exportUrl={`/api/embedded/internal/workflows/${workflow.id}/export`}
                        onDeleteClick={() => setShowDeleteDialog(true)}
                        onEditClick={() => {
                            setIsPermissionExpressionSeeded(false);
                            setShowEditDialog(true);
                        }}
                        workflowLabel={workflow.label}
                    />
                )}
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
                        workflow.integrationWorkflowId != null &&
                        isPermissionExpressionSeeded && (
                            <IntegrationWorkflowPermissionExpressionField
                                onChange={setPermissionExpression}
                                value={permissionExpression}
                            />
                        )
                    }
                    onClose={() => setShowEditDialog(false)}
                    onSave={() => {
                        pendingPermissionExpressionRef.current =
                            workflow.integrationWorkflowId != null ? permissionExpression?.trim() || null : undefined;

                        void queryClient.invalidateQueries({
                            queryKey: IntegrationWorkflowKeys.integrationWorkflow(
                                integration.id!,
                                Number.parseInt(workflow.id!, 10)
                            ),
                        });
                    }}
                    parentId={integration.id}
                    saveDisabled={workflow.integrationWorkflowId != null && !isPermissionExpressionSeeded}
                    updateWorkflowMutation={updateWorkflowMutation}
                    useGetWorkflowQuery={useGetWorkflowQuery}
                    workflowId={workflow.id!}
                />
            )}
        </li>
    );
};

export default IntegrationWorkflowListItem;
