import '@/shared/styles/dropdownMenu.css';
import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import IntegrationWorkflowPermissionExpressionField from '@/ee/pages/embedded/integrations/components/integration-workflow-list/IntegrationWorkflowPermissionExpressionField';
import {Integration, Workflow} from '@/ee/shared/middleware/embedded/configuration';
import {useDeleteWorkflowMutation, useUpdateWorkflowMutation} from '@/ee/shared/mutations/embedded/workflows.mutations';
import {IntegrationWorkflowKeys} from '@/ee/shared/queries/embedded/integrationWorkflows.queries';
import {IntegrationKeys} from '@/ee/shared/queries/embedded/integrations.queries';
import {WorkflowKeys, useGetWorkflowQuery} from '@/ee/shared/queries/embedded/workflows.queries';
import DeleteWorkflowAlertDialog from '@/shared/components/DeleteWorkflowAlertDialog';
import WorkflowComponentsList from '@/shared/components/WorkflowComponentsList';
import WorkflowDialog from '@/shared/components/workflow/WorkflowDialog';
import {
    useIntegrationWorkflowsByIntegrationIdQuery,
    useUpdateIntegrationWorkflowPermissionExpressionMutation,
} from '@/shared/middleware/graphql';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {WorkflowTestConfigurationKeys} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {isJavaCodeWorkflow} from '@/shared/util/codeWorkflowLanguage-utils';
import {useQueryClient} from '@tanstack/react-query';
import {ComponentIcon, DownloadIcon, EditIcon, EllipsisVerticalIcon, Trash2Icon} from 'lucide-react';
import {useEffect, useMemo, useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {Link, useSearchParams} from 'react-router-dom';
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
    const javaCodeWorkflow = isJavaCodeWorkflow(integration.codeWorkflow, integration.codeWorkflowLanguage);

    // The projects list shows a workflow's trigger as its own icon plus a label chip, and its task components
    // through the shared WorkflowComponentsList (icons, then a +N pill). This mirrors that so the two lists read
    // the same; the trigger's own name comes off the workflow rather than a component definition lookup, which
    // this surface does not have.
    const triggerComponentName = workflow.workflowTriggerComponentNames?.[0];

    const triggerData = useMemo(() => {
        const trigger = workflow.triggers?.[0];

        if (!triggerComponentName && !trigger) {
            return null;
        }

        const triggerDefinition = workflowComponentDefinitions[triggerComponentName ?? ''];

        return {
            componentName: triggerDefinition?.title ?? triggerComponentName ?? 'Unknown Trigger',
            iconSrc: triggerDefinition?.icon ?? '',
            label: trigger?.label ?? '',
        };
    }, [triggerComponentName, workflow.triggers, workflowComponentDefinitions]);

    const taskOnlyComponentNames = useMemo(
        () => (filteredComponentNames ?? []).slice(workflow.workflowTriggerComponentNames?.length ?? 0),
        [filteredComponentNames, workflow.workflowTriggerComponentNames]
    );

    const [permissionExpression, setPermissionExpression] = useState<string | null>('');
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showEditDialog, setShowEditDialog] = useState(false);

    const [searchParams] = useSearchParams();

    const queryClient = useQueryClient();

    const updateIntegrationWorkflowPermissionExpressionMutation =
        useUpdateIntegrationWorkflowPermissionExpressionMutation();

    const {data: integrationWorkflowsData} = useIntegrationWorkflowsByIntegrationIdQuery(
        {integrationId: String(integration.id)},
        {enabled: showEditDialog && integration.id != null}
    );

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
            queryClient.invalidateQueries({
                queryKey: IntegrationWorkflowKeys.integrationWorkflows(integration.id!),
            });

            queryClient.invalidateQueries({
                queryKey: WorkflowTestConfigurationKeys.workflowTestConfiguration(workflow.id!),
            });

            queryClient.invalidateQueries({
                queryKey: WorkflowKeys.workflow(workflow.id!),
            });

            setShowEditDialog(false);
        },
    });

    useEffect(() => {
        if (showEditDialog) {
            setPermissionExpression(integrationWorkflowPermissionExpression ?? '');
        }
    }, [integrationWorkflowPermissionExpression, showEditDialog]);

    return (
        <li
            className="flex items-center justify-between rounded-md px-3 py-1 hover:bg-destructive-foreground"
            key={workflow.id}
        >
            <Link
                aria-disabled={javaCodeWorkflow || undefined}
                className={twMerge(
                    'flex flex-1 items-center',
                    // A polyglot code integration opens its source editor here, same as the automation side; a Java
                    // one comes from a compiled JAR and has nothing to open.
                    javaCodeWorkflow && 'pointer-events-none'
                )}
                tabIndex={javaCodeWorkflow ? -1 : undefined}
                to={`/embedded/integrations/${integration.id}/integration-workflows/${workflow.integrationWorkflowId}?${searchParams}`}
            >
                <div className="w-80 text-sm font-semibold">{workflow.label}</div>

                <div className="flex items-center gap-1">
                    {triggerData && (
                        <div className="flex shrink-0 items-center gap-1">
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <div className="flex shrink-0 items-center justify-center rounded-full border border-stroke-neutral-primary bg-surface-neutral-primary p-1">
                                        {triggerData.iconSrc ? (
                                            <InlineSVG
                                                className="size-5"
                                                loader={<ComponentIcon className="size-5 flex-none" />}
                                                src={triggerData.iconSrc}
                                                title={null}
                                            />
                                        ) : (
                                            <ComponentIcon className="size-3 flex-none text-content-neutral-primary" />
                                        )}
                                    </div>
                                </TooltipTrigger>

                                <TooltipContent>{triggerData.componentName}</TooltipContent>
                            </Tooltip>

                            <div className="shrink-0">
                                <Badge
                                    label={triggerData.label || triggerData.componentName}
                                    styleType="outline-outline"
                                    weight="semibold"
                                />
                            </div>
                        </div>
                    )}

                    <WorkflowComponentsList
                        filteredComponentNames={taskOnlyComponentNames}
                        workflowComponentDefinitions={workflowComponentDefinitions}
                        workflowTaskDispatcherDefinitions={workflowTaskDispatcherDefinitions}
                    />
                </div>
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

                {!integration.codeWorkflow && (
                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <Button icon={<EllipsisVerticalIcon />} size="icon" variant="ghost" />
                        </DropdownMenuTrigger>

                        <DropdownMenuContent align="end" className="p-0">
                            <DropdownMenuItem
                                className="dropdown-menu-item"
                                onClick={() => {
                                    setShowEditDialog(true);
                                }}
                            >
                                <EditIcon /> Edit
                            </DropdownMenuItem>

                            <DropdownMenuItem
                                className="dropdown-menu-item"
                                onClick={() =>
                                    (window.location.href = `/api/embedded/internal/workflows/${workflow.id}/export`)
                                }
                            >
                                <DownloadIcon /> Export
                            </DropdownMenuItem>

                            <DropdownMenuSeparator className="m-0" />

                            <DropdownMenuItem
                                className="dropdown-menu-item-destructive"
                                onClick={() => {
                                    setShowDeleteDialog(true);
                                }}
                                variant="destructive"
                            >
                                <Trash2Icon /> Delete
                            </DropdownMenuItem>
                        </DropdownMenuContent>
                    </DropdownMenu>
                )}
            </div>

            {showDeleteDialog && (
                <DeleteWorkflowAlertDialog
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
                        workflow.integrationWorkflowId != null && (
                            <IntegrationWorkflowPermissionExpressionField
                                onChange={setPermissionExpression}
                                value={permissionExpression}
                            />
                        )
                    }
                    onClose={() => setShowEditDialog(false)}
                    onSave={() => {
                        if (workflow.integrationWorkflowId != null) {
                            updateIntegrationWorkflowPermissionExpressionMutation.mutate({
                                integrationWorkflowId: String(workflow.integrationWorkflowId),
                                permissionExpression: permissionExpression?.trim() || null,
                            });
                        }

                        queryClient.invalidateQueries({
                            queryKey: IntegrationWorkflowKeys.integrationWorkflow(
                                integration.id!,
                                parseInt(workflow.id!)
                            ),
                        });
                    }}
                    parentId={integration.id}
                    updateWorkflowMutation={updateWorkflowMutation}
                    useGetWorkflowQuery={useGetWorkflowQuery}
                    workflowId={workflow.id!}
                />
            )}
        </li>
    );
};

export default IntegrationWorkflowListItem;
