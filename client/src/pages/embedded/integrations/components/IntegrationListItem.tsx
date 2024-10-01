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
import {Button} from '@/components/ui/button';
import {CollapsibleTrigger} from '@/components/ui/collapsible';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useToast} from '@/components/ui/use-toast';
import IntegrationDialog from '@/pages/embedded/integrations/components/IntegrationDialog';
import IntegrationPublishDialog from '@/pages/embedded/integrations/components/IntegrationPublishDialog';
import WorkflowDialog from '@/pages/platform/workflow/components/WorkflowDialog';
import {Integration, Tag} from '@/shared/middleware/embedded/configuration';
import {
    useDeleteIntegrationMutation,
    useUpdateIntegrationTagsMutation,
} from '@/shared/mutations/embedded/integrations.mutations';
import {useCreateIntegrationWorkflowMutation} from '@/shared/mutations/embedded/workflows.mutations';
import {useGetWorkflowQuery} from '@/shared/queries/automation/workflows.queries';
import {IntegrationCategoryKeys} from '@/shared/queries/embedded/integrationCategories.queries';
import {IntegrationTagKeys} from '@/shared/queries/embedded/integrationTags.quries';
import {IntegrationKeys} from '@/shared/queries/embedded/integrations.queries';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {ChevronDownIcon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {EllipsisVerticalIcon} from 'lucide-react';
import {ChangeEvent, useRef, useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {Link, useNavigate} from 'react-router-dom';

import TagList from '../../../../components/TagList';

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

    const navigate = useNavigate();

    const {toast} = useToast();

    const {data: componentDefinition} = useGetComponentDefinitionQuery({
        componentName: integration.componentName!,
    });

    const queryClient = useQueryClient();

    const createIntegrationWorkflowMutation = useCreateIntegrationWorkflowMutation({
        onSuccess: (workflow) => {
            navigate(
                `/embedded/integrations/${integration.id}/integration-workflows/${workflow?.integrationWorkflowId}`
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
                <div className="flex flex-1 items-center border-b border-muted py-5 group-data-[state='open']:border-none">
                    <div className="flex-1">
                        <div className="flex items-center justify-between">
                            <div className="relative flex items-center gap-2">
                                {integration?.integrationWorkflowIds &&
                                integration?.integrationWorkflowIds.length > 0 ? (
                                    <Link
                                        className="flex items-center gap-2"
                                        to={`/embedded/integrations/${integration?.id}/integration-workflows/${integration?.integrationWorkflowIds![0]}`}
                                    >
                                        {componentDefinition?.icon && (
                                            <InlineSVG className="size-6 flex-none" src={componentDefinition.icon} />
                                        )}

                                        <span className="text-base font-semibold text-gray-900">
                                            {integration?.name}
                                        </span>
                                    </Link>
                                ) : (
                                    <div className="flex items-center gap-2">
                                        {componentDefinition?.icon && (
                                            <InlineSVG className="size-6 flex-none" src={componentDefinition.icon} />
                                        )}

                                        <span className="text-base font-semibold text-gray-900">
                                            {integration?.name}
                                        </span>
                                    </div>
                                )}

                                {integration.category && (
                                    <span className="text-xs uppercase text-gray-700">{integration.category.name}</span>
                                )}
                            </div>
                        </div>

                        <div className="relative mt-2 sm:flex sm:items-center sm:justify-between">
                            <div className="flex items-center">
                                <CollapsibleTrigger className="group mr-4 flex items-center text-xs font-semibold text-gray-700">
                                    <div className="mr-1">
                                        {integration.integrationWorkflowIds?.length === 1
                                            ? `${integration.integrationWorkflowIds?.length} workflow`
                                            : `${integration.integrationWorkflowIds?.length} workflows`}
                                    </div>

                                    <ChevronDownIcon className="duration-300 group-data-[state=open]:rotate-180" />
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
                                {integration.lastPublishedDate && integration.lastIntegrationVersion && (
                                    <Badge className="flex space-x-1" variant="secondary">
                                        <span>V{integration.lastIntegrationVersion - 1}</span>

                                        <span>PUBLISHED</span>
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
                                <Button size="icon" variant="ghost">
                                    <EllipsisVerticalIcon className="size-4 hover:cursor-pointer" />
                                </Button>
                            </DropdownMenuTrigger>

                            <DropdownMenuContent align="end">
                                <DropdownMenuItem onClick={() => setShowEditDialog(true)}>Edit</DropdownMenuItem>

                                {integration?.integrationWorkflowIds &&
                                    integration?.integrationWorkflowIds.length > 0 && (
                                        <DropdownMenuItem
                                            onClick={() =>
                                                navigate(
                                                    `/embedded/integrations/${integration?.id}/integration-workflows/${integration.integrationWorkflowIds![0]}`
                                                )
                                            }
                                        >
                                            View Workflows
                                        </DropdownMenuItem>
                                    )}

                                <DropdownMenuItem onClick={() => setShowWorkflowDialog(true)}>
                                    New Workflow
                                </DropdownMenuItem>

                                <DropdownMenuItem onClick={() => setShowPublishIntegrationDialog(true)}>
                                    Publish
                                </DropdownMenuItem>

                                <DropdownMenuItem
                                    onClick={() => {
                                        if (hiddenFileInputRef.current) {
                                            hiddenFileInputRef.current.click();
                                        }
                                    }}
                                >
                                    Import Workflow
                                </DropdownMenuItem>

                                <DropdownMenuSeparator />

                                <DropdownMenuItem
                                    className="text-destructive"
                                    onClick={() => setShowDeleteDialog(true)}
                                >
                                    Delete
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
                            className="bg-destructive"
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
                    onClose={() => setShowWorkflowDialog(false)}
                    parentId={integration.id}
                    useGetWorkflowQuery={useGetWorkflowQuery}
                />
            )}

            <input className="hidden" onChange={handleFileChange} ref={hiddenFileInputRef} type="file" />
        </>
    );
};

export default IntegrationListItem;
