import AlertDialog from '@/components/AlertDialog';
import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import {ButtonGroup} from '@/components/ui/button-group';
import {CollapsibleContent, CollapsibleTrigger} from '@/components/ui/collapsible';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import IntegrationInstanceConfigurationDialog from '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialog';
import IntegrationDialog from '@/ee/pages/embedded/integrations/components/IntegrationDialog';
import IntegrationPublishDialog from '@/ee/pages/embedded/integrations/components/IntegrationPublishDialog';
import IntegrationListItemTabs, {
    IntegrationListItemTabType,
} from '@/ee/pages/embedded/integrations/components/integration-list/IntegrationListItemTabs';
import {Integration, IntegrationInstanceConfiguration, Tag} from '@/ee/shared/middleware/embedded/configuration';
import {
    useDeleteIntegrationMutation,
    useUpdateIntegrationTagsMutation,
} from '@/ee/shared/mutations/embedded/integrations.mutations';
import {useCreateIntegrationWorkflowMutation} from '@/ee/shared/mutations/embedded/workflows.mutations';
import {IntegrationCategoryKeys} from '@/ee/shared/queries/embedded/integrationCategories.queries';
import {IntegrationTagKeys} from '@/ee/shared/queries/embedded/integrationTags.quries';
import {IntegrationKeys} from '@/ee/shared/queries/embedded/integrations.queries';
import TagList from '@/shared/components/TagList';
import WorkflowDialog from '@/shared/components/workflow/WorkflowDialog';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {useGetWorkflowQuery} from '@/shared/queries/automation/workflows.queries';
import {useQueryClient} from '@tanstack/react-query';
import {
    ChevronDownIcon,
    EditIcon,
    EllipsisVerticalIcon,
    PlusIcon,
    SendIcon,
    Trash2Icon,
    UploadIcon,
    WorkflowIcon,
} from 'lucide-react';
import {ChangeEvent, ReactNode, useCallback, useRef, useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {Link, useNavigate, useSearchParams} from 'react-router-dom';
import {toast} from 'sonner';

interface IntegrationItemProps {
    activeTab?: IntegrationListItemTabType;
    configurationsContent: ReactNode;
    integration: Integration;
    onTabChange: (tab?: IntegrationListItemTabType) => void;
    remainingTags?: Tag[];
    workflowsContent: ReactNode;
}

const IntegrationListItem = ({
    activeTab,
    configurationsContent,
    integration,
    onTabChange,
    remainingTags,
    workflowsContent,
}: IntegrationItemProps) => {
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showPublishIntegrationDialog, setShowPublishIntegrationDialog] = useState(false);
    const [showWorkflowDialog, setShowWorkflowDialog] = useState(false);

    const hiddenFileInputRef = useRef<HTMLInputElement>(null);

    const {captureIntegrationWorkflowCreated, captureIntegrationWorkflowImported} = useAnalytics();

    const navigate = useNavigate();

    const [searchParams] = useSearchParams();

    const queryClient = useQueryClient();

    const configurationCount = integration.developmentConfigurationCount;
    const workflowCount = integration.integrationWorkflowIds?.length ?? 0;
    const isPublished = !!(integration.lastPublishedDate && integration.lastIntegrationVersion);

    const createIntegrationWorkflowMutation = useCreateIntegrationWorkflowMutation({
        onSuccess: (integrationWorkflowId) => {
            captureIntegrationWorkflowCreated();

            navigate(
                `/embedded/integrations/${integration.id}/integration-workflows/${integrationWorkflowId}?${searchParams}`
            );

            setShowWorkflowDialog(false);
        },
    });

    const deleteIntegrationMutation = useDeleteIntegrationMutation({
        onSuccess: () => {
            queryClient.removeQueries({
                queryKey: IntegrationKeys.integration(integration.id!),
            });

            queryClient.invalidateQueries({
                queryKey: IntegrationKeys.integrations,
            });
            queryClient.invalidateQueries({
                queryKey: IntegrationCategoryKeys.integrationCategories,
            });
            queryClient.invalidateQueries({
                queryKey: IntegrationTagKeys.integrationTags,
            });

            setShowDeleteDialog(false);
        },
    });

    const importIntegrationWorkflowMutation = useCreateIntegrationWorkflowMutation({
        onSuccess: () => {
            captureIntegrationWorkflowImported();

            queryClient.invalidateQueries({
                queryKey: IntegrationKeys.integrations,
            });

            if (hiddenFileInputRef.current) {
                hiddenFileInputRef.current.value = '';
            }

            toast('Workflow is imported.');

            if (!activeTab) {
                onTabChange('workflows');
            }
        },
    });

    const updateIntegrationTagsMutation = useUpdateIntegrationTagsMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: IntegrationKeys.integrations,
            });
            queryClient.invalidateQueries({
                queryKey: IntegrationTagKeys.integrationTags,
            });
        },
    });

    const handleFileChange = async (e: ChangeEvent<HTMLInputElement>) => {
        if (e.target.files) {
            importIntegrationWorkflowMutation.mutate({
                id: integration.id!,
                workflow: {
                    definition: await e.target.files[0].text(),
                },
            });
        }
    };

    const handleIntegrationListItemClick = useCallback(
        (event: React.MouseEvent) => {
            const target = event.target as HTMLElement;

            const interactiveSelectors = [
                '[data-interactive]',
                '[role="menuitem"]',
                '[role="menu"]',
                '[data-radix-dropdown-menu-trigger]',
                '[data-radix-collapsible-trigger]',
                'button',
                'input',
                'svg',
            ].join(', ');

            if (target.closest(interactiveSelectors)) {
                return;
            }

            onTabChange(activeTab ? undefined : 'workflows');
        },
        [activeTab, onTabChange]
    );

    return (
        <>
            <div
                className="flex w-full cursor-pointer items-center justify-between rounded-md px-3 hover:bg-surface-neutral-primary-hover"
                onClick={(event) => handleIntegrationListItemClick(event)}
            >
                <div className="flex flex-1 items-center py-3 group-data-[state='open']/integration:border-none">
                    <div className="flex-1">
                        <div className="flex min-h-8 items-center justify-between">
                            <div className="relative flex items-center gap-2">
                                {integration?.integrationWorkflowIds &&
                                integration?.integrationWorkflowIds.length > 0 ? (
                                    <Link
                                        className="flex items-center gap-1"
                                        onClick={(event) => event.stopPropagation()}
                                        to={`/embedded/integrations/${integration?.id}/integration-workflows/${integration?.integrationWorkflowIds![0]}?${searchParams}`}
                                    >
                                        {integration?.icon && (
                                            <InlineSVG className="size-5 flex-none" src={integration.icon} />
                                        )}

                                        <span className="text-base font-semibold text-content-neutral-primary">
                                            {integration?.name}
                                        </span>
                                    </Link>
                                ) : (
                                    <div className="flex items-center gap-1">
                                        {integration?.icon && (
                                            <InlineSVG className="size-5 flex-none" src={integration.icon} />
                                        )}

                                        <span className="text-base font-semibold text-content-neutral-primary">
                                            {integration?.name}
                                        </span>
                                    </div>
                                )}
                            </div>
                        </div>

                        <div className="relative mt-2 min-h-7 sm:flex sm:items-center sm:justify-between">
                            <div className="flex items-center gap-2">
                                <CollapsibleTrigger className="group mr-2 flex items-center text-xs font-semibold text-muted-foreground">
                                    <span className="mr-1">
                                        {workflowCount === 1 ? '1 workflow' : `${workflowCount} workflows`}
                                    </span>

                                    <span className="mx-1">-</span>

                                    <span className="mr-1">
                                        {configurationCount === undefined
                                            ? 'configurations'
                                            : configurationCount === 1
                                              ? '1 configuration'
                                              : `${configurationCount} configurations`}
                                    </span>

                                    <ChevronDownIcon className="size-4 duration-300 group-data-[state=open]:rotate-180" />
                                </CollapsibleTrigger>

                                <div onClick={(event) => event.preventDefault()}>
                                    {integration.tags && (
                                        <TagList
                                            getRequest={(id, tags) => ({
                                                id: id!,
                                                updateTagsRequest: {
                                                    tags: tags || [],
                                                },
                                            })}
                                            id={integration.id!}
                                            remainingTags={remainingTags}
                                            tags={integration.tags}
                                            updateTagsMutation={updateIntegrationTagsMutation}
                                        />
                                    )}
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="flex items-center justify-end gap-x-6">
                        {integration.lastIntegrationVersion && (
                            <div className="flex flex-col items-end gap-y-2">
                                <div className="flex min-h-8 items-center">
                                    {integration.lastPublishedDate && integration.lastIntegrationVersion ? (
                                        <Badge
                                            className="flex space-x-1 border-transparent"
                                            styleType="success-outline"
                                            weight="semibold"
                                        >
                                            <span>V{integration.lastIntegrationVersion - 1}</span>

                                            <span>PUBLISHED</span>
                                        </Badge>
                                    ) : (
                                        <Badge
                                            className="flex space-x-1"
                                            styleType="secondary-filled"
                                            weight="semibold"
                                        >
                                            <span>V{integration.lastIntegrationVersion}</span>

                                            <span>{integration.lastStatus}</span>
                                        </Badge>
                                    )}
                                </div>

                                <Tooltip>
                                    <TooltipTrigger>
                                        <div className="flex min-h-7 items-center text-sm text-content-neutral-secondary sm:mt-0">
                                            {integration.lastPublishedDate ? (
                                                <span className="text-xs">
                                                    {`Published at ${integration.lastPublishedDate?.toLocaleDateString()} ${integration.lastPublishedDate?.toLocaleTimeString()}`}
                                                </span>
                                            ) : (
                                                <span className="text-xs">Not yet published</span>
                                            )}
                                        </div>
                                    </TooltipTrigger>

                                    <TooltipContent side="left">Last Published Date</TooltipContent>
                                </Tooltip>
                            </div>
                        )}

                        <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                                <Button icon={<EllipsisVerticalIcon />} size="icon" variant="ghost" />
                            </DropdownMenuTrigger>

                            <DropdownMenuContent align="end">
                                <DropdownMenuItem
                                    icon={<EditIcon />}
                                    label="Edit"
                                    onClick={() => setShowEditDialog(true)}
                                />

                                {integration?.integrationWorkflowIds &&
                                    integration?.integrationWorkflowIds.length > 0 && (
                                        <DropdownMenuItem
                                            icon={<WorkflowIcon />}
                                            label="View Workflows"
                                            onClick={() =>
                                                navigate(
                                                    `/embedded/integrations/${integration?.id}/integration-workflows/${integration.integrationWorkflowIds![0]}`
                                                )
                                            }
                                        />
                                    )}

                                <DropdownMenuItem
                                    icon={<SendIcon />}
                                    label="Publish"
                                    onClick={() => setShowPublishIntegrationDialog(true)}
                                />

                                <DropdownMenuSeparator />

                                <DropdownMenuItem
                                    icon={<Trash2Icon />}
                                    label="Delete"
                                    onClick={() => setShowDeleteDialog(true)}
                                    variant="destructive"
                                />
                            </DropdownMenuContent>
                        </DropdownMenu>
                    </div>
                </div>
            </div>

            <CollapsibleContent className="mx-3 mt-1 mb-3">
                <IntegrationListItemTabs
                    actions={
                        activeTab === 'workflows' ? (
                            <ButtonGroup aria-label="Workflow Creation Actions">
                                <Button
                                    aria-label="Create Workflow"
                                    onClick={(event) => {
                                        event.stopPropagation();

                                        setShowWorkflowDialog(true);
                                    }}
                                    size="sm"
                                    variant="secondary"
                                >
                                    <PlusIcon />
                                    New Workflow
                                </Button>

                                <DropdownMenu>
                                    <DropdownMenuTrigger asChild>
                                        <Button
                                            aria-label="More Workflow Creation Actions"
                                            icon={<ChevronDownIcon />}
                                            size="sm"
                                            variant="secondary"
                                        >
                                            {' '}
                                        </Button>
                                    </DropdownMenuTrigger>

                                    <DropdownMenuContent align="end">
                                        <DropdownMenuItem
                                            aria-label="Import Workflow"
                                            icon={<UploadIcon />}
                                            label="Import Workflow"
                                            onClick={(event) => {
                                                event.stopPropagation();

                                                if (hiddenFileInputRef.current) {
                                                    hiddenFileInputRef.current.click();
                                                }
                                            }}
                                        />
                                    </DropdownMenuContent>
                                </DropdownMenu>
                            </ButtonGroup>
                        ) : activeTab === 'configurations' ? (
                            isPublished ? (
                                <IntegrationInstanceConfigurationDialog
                                    integrationInstanceConfiguration={
                                        {
                                            environmentId: DEVELOPMENT_ENVIRONMENT,
                                            integrationId: integration.id,
                                        } as IntegrationInstanceConfiguration
                                    }
                                    triggerNode={
                                        <Button aria-label="Create Configuration" size="sm" variant="secondary">
                                            <PlusIcon />
                                            New Configuration
                                        </Button>
                                    }
                                />
                            ) : (
                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <span className="inline-block">
                                            <Button
                                                aria-label="Create Configuration"
                                                disabled
                                                size="sm"
                                                variant="secondary"
                                            >
                                                <PlusIcon />
                                                New Configuration
                                            </Button>
                                        </span>
                                    </TooltipTrigger>

                                    <TooltipContent>Publish the integration first</TooltipContent>
                                </Tooltip>
                            )
                        ) : undefined
                    }
                    activeTab={activeTab ?? 'workflows'}
                    configurationsContent={configurationsContent}
                    onTabChange={(tab) => onTabChange(tab)}
                    workflowsContent={workflowsContent}
                />
            </CollapsibleContent>

            <AlertDialog
                description="This action cannot be undone. This will permanently delete the integration and workflows it contains."
                isPending={deleteIntegrationMutation.isPending}
                onCancel={() => setShowDeleteDialog(false)}
                onConfirm={() => {
                    if (integration.id) {
                        deleteIntegrationMutation.mutate(integration.id);
                    }
                }}
                open={showDeleteDialog}
            />

            {showEditDialog && <IntegrationDialog integration={integration} onClose={() => setShowEditDialog(false)} />}

            {showPublishIntegrationDialog && !!integration.id && (
                <IntegrationPublishDialog
                    integration={integration}
                    onClose={() => setShowPublishIntegrationDialog(false)}
                />
            )}

            {showWorkflowDialog && !!integration.id && (
                <WorkflowDialog
                    createWorkflowMutation={createIntegrationWorkflowMutation}
                    onClose={() => setShowWorkflowDialog(false)}
                    parentId={integration.id}
                    useGetWorkflowQuery={useGetWorkflowQuery}
                />
            )}

            <input
                accept=".json,.yaml,.yml"
                className="hidden"
                onChange={handleFileChange}
                ref={hiddenFileInputRef}
                type="file"
            />
        </>
    );
};

export default IntegrationListItem;
