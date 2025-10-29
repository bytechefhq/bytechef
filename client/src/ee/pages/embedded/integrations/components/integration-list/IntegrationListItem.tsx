import '@/shared/styles/dropdownMenu.css';
import Button from '@/components/Button/Button';
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import {Badge} from '@/components/ui/badge';
import {CollapsibleTrigger} from '@/components/ui/collapsible';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import IntegrationDialog from '@/ee/pages/embedded/integrations/components/IntegrationDialog';
import IntegrationPublishDialog from '@/ee/pages/embedded/integrations/components/IntegrationPublishDialog';
import {Integration, Tag} from '@/ee/shared/middleware/embedded/configuration';
import {
    useDeleteIntegrationMutation,
    useUpdateIntegrationTagsMutation,
} from '@/ee/shared/mutations/embedded/integrations.mutations';
import {useCreateIntegrationWorkflowMutation} from '@/ee/shared/mutations/embedded/workflows.mutations';
import {IntegrationCategoryKeys} from '@/ee/shared/queries/embedded/integrationCategories.queries';
import {IntegrationTagKeys} from '@/ee/shared/queries/embedded/integrationTags.quries';
import {IntegrationKeys} from '@/ee/shared/queries/embedded/integrations.queries';
import {useToast} from '@/hooks/use-toast';
import TagList from '@/shared/components/TagList';
import WorkflowDialog from '@/shared/components/workflow/WorkflowDialog';
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
import {ChangeEvent, useRef, useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {Link, useNavigate, useSearchParams} from 'react-router-dom';

interface IntegrationItemProps {
    integration: Integration;
    remainingTags?: Tag[];
}

const IntegrationListItem = ({integration, remainingTags}: IntegrationItemProps) => {
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showPublishIntegrationDialog, setShowPublishIntegrationDialog] = useState(false);
    const [showWorkflowDialog, setShowWorkflowDialog] = useState(false);

    const hiddenFileInputRef = useRef<HTMLInputElement>(null);

    const {captureIntegrationWorkflowCreated, captureIntegrationWorkflowImported} = useAnalytics();

    const navigate = useNavigate();

    const [searchParams] = useSearchParams();

    const {toast} = useToast();

    const queryClient = useQueryClient();

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
            queryClient.invalidateQueries({
                queryKey: IntegrationKeys.integrations,
            });
            queryClient.invalidateQueries({
                queryKey: IntegrationCategoryKeys.integrationCategories,
            });
            queryClient.invalidateQueries({
                queryKey: IntegrationTagKeys.integrationTags,
            });
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

            toast({
                description: 'Workflow is imported.',
            });
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

    return (
        <>
            <div className="flex w-full items-center justify-between rounded-md px-2 hover:bg-gray-50">
                <div className="flex flex-1 items-center py-5 group-data-[state='open']:border-none">
                    <div className="flex-1">
                        <div className="flex items-center justify-between">
                            <div className="relative flex items-center gap-2">
                                {integration?.integrationWorkflowIds &&
                                integration?.integrationWorkflowIds.length > 0 ? (
                                    <Link
                                        className="flex items-center gap-1"
                                        to={`/embedded/integrations/${integration?.id}/integration-workflows/${integration?.integrationWorkflowIds![0]}?${searchParams}`}
                                    >
                                        {integration?.icon && (
                                            <InlineSVG className="size-5 flex-none" src={integration.icon} />
                                        )}

                                        <span className="text-base font-semibold text-gray-900">
                                            {integration?.name}
                                        </span>
                                    </Link>
                                ) : (
                                    <CollapsibleTrigger>
                                        <div className="flex items-center gap-1">
                                            {integration?.icon && (
                                                <InlineSVG className="size-5 flex-none" src={integration.icon} />
                                            )}

                                            <span className="text-base font-semibold text-gray-900">
                                                {integration?.name}
                                            </span>
                                        </div>
                                    </CollapsibleTrigger>
                                )}
                            </div>
                        </div>

                        <div className="relative mt-2 sm:flex sm:items-center sm:justify-between">
                            <div className="flex items-center">
                                <CollapsibleTrigger className="group mr-4 flex items-center text-xs font-semibold text-muted-foreground">
                                    <div className="mr-1">
                                        {integration.integrationWorkflowIds?.length === 1
                                            ? `${integration.integrationWorkflowIds?.length} workflow`
                                            : `${integration.integrationWorkflowIds?.length} workflows`}
                                    </div>

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
                            <div className="flex flex-col items-end gap-y-4">
                                {integration.lastPublishedDate && integration.lastIntegrationVersion ? (
                                    <Badge className="flex space-x-1" variant="success">
                                        <span>V{integration.lastIntegrationVersion - 1}</span>

                                        <span>PUBLISHED</span>
                                    </Badge>
                                ) : (
                                    <Badge className="flex space-x-1" variant="secondary">
                                        <span>V{integration.lastIntegrationVersion}</span>

                                        <span>{integration.lastStatus}</span>
                                    </Badge>
                                )}

                                <Tooltip>
                                    <TooltipTrigger>
                                        <div className="flex items-center text-sm text-gray-500 sm:mt-0">
                                            {integration.lastPublishedDate ? (
                                                <span className="text-xs">
                                                    {`Published at ${integration.lastPublishedDate?.toLocaleDateString()} ${integration.lastPublishedDate?.toLocaleTimeString()}`}
                                                </span>
                                            ) : (
                                                <span className="text-xs">Not yet published</span>
                                            )}
                                        </div>
                                    </TooltipTrigger>

                                    <TooltipContent>Last Published Date</TooltipContent>
                                </Tooltip>
                            </div>
                        )}

                        <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                                <Button icon={<EllipsisVerticalIcon />} size="icon" variant="ghost" />
                            </DropdownMenuTrigger>

                            <DropdownMenuContent align="end" className="p-0">
                                <DropdownMenuItem
                                    className="dropdown-menu-item"
                                    onClick={() => setShowEditDialog(true)}
                                >
                                    <EditIcon /> Edit
                                </DropdownMenuItem>

                                {integration?.integrationWorkflowIds &&
                                    integration?.integrationWorkflowIds.length > 0 && (
                                        <DropdownMenuItem
                                            className="dropdown-menu-item"
                                            onClick={() =>
                                                navigate(
                                                    `/embedded/integrations/${integration?.id}/integration-workflows/${integration.integrationWorkflowIds![0]}`
                                                )
                                            }
                                        >
                                            <WorkflowIcon /> View Workflows
                                        </DropdownMenuItem>
                                    )}

                                <DropdownMenuItem
                                    className="dropdown-menu-item"
                                    onClick={() => setShowWorkflowDialog(true)}
                                >
                                    <PlusIcon /> New Workflow
                                </DropdownMenuItem>

                                <DropdownMenuItem
                                    className="dropdown-menu-item"
                                    onClick={() => setShowPublishIntegrationDialog(true)}
                                >
                                    <SendIcon /> Publish
                                </DropdownMenuItem>

                                <DropdownMenuItem
                                    className="dropdown-menu-item"
                                    onClick={() => {
                                        if (hiddenFileInputRef.current) {
                                            hiddenFileInputRef.current.click();
                                        }
                                    }}
                                >
                                    <UploadIcon /> Import Workflow
                                </DropdownMenuItem>

                                <DropdownMenuSeparator className="m-0" />

                                <DropdownMenuItem
                                    className="dropdown-menu-item-destructive"
                                    onClick={() => setShowDeleteDialog(true)}
                                >
                                    <Trash2Icon /> Delete
                                </DropdownMenuItem>
                            </DropdownMenuContent>
                        </DropdownMenu>
                    </div>
                </div>
            </div>

            <AlertDialog open={showDeleteDialog}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>

                        <AlertDialogDescription>
                            This action cannot be undone. This will permanently delete the integration and workflows it
                            contains..
                        </AlertDialogDescription>
                    </AlertDialogHeader>

                    <AlertDialogFooter>
                        <AlertDialogCancel onClick={() => setShowDeleteDialog(false)}>Cancel</AlertDialogCancel>

                        <AlertDialogAction
                            className="bg-surface-destructive-primary shadow-none hover:bg-surface-destructive-primary-hover active:bg-surface-destructive-primary-active"
                            onClick={() => {
                                if (integration.id) {
                                    deleteIntegrationMutation.mutate(integration.id);
                                }
                            }}
                        >
                            Delete
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>

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
                    integrationId={integration.id}
                    onClose={() => setShowWorkflowDialog(false)}
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
