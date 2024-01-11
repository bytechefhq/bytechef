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
import {IntegrationModel, IntegrationModelStatusEnum, TagModel} from '@/middleware/embedded/configuration';
import {
    useCreateIntegrationWorkflowMutation,
    useDeleteIntegrationMutation,
    useUpdateIntegrationTagsMutation,
} from '@/mutations/embedded/integrations.mutations';
import IntegrationDialog from '@/pages/embedded/integrations/components/IntegrationDialog';
import WorkflowDialog from '@/pages/platform/workflow/components/WorkflowDialog';
import {IntegrationCategoryKeys} from '@/queries/embedded/integrationCategories.queries';
import {IntegrationTagKeys} from '@/queries/embedded/integrationTags.quries';
import {IntegrationKeys} from '@/queries/embedded/integrations.queries';
import {useGetComponentDefinitionQuery} from '@/queries/platform/componentDefinitions.queries';
import {ChevronDownIcon, DotsVerticalIcon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {Link, useNavigate} from 'react-router-dom';

import TagList from '../../../../components/TagList';

interface IntegrationItemProps {
    integration: IntegrationModel;
    remainingTags?: TagModel[];
}

const IntegrationListItem = ({integration, remainingTags}: IntegrationItemProps) => {
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showWorkflowDialog, setShowWorkflowDialog] = useState(false);

    const navigate = useNavigate();

    const {data: componentDefinition} = useGetComponentDefinitionQuery({
        componentName: integration.componentName!,
    });

    const queryClient = useQueryClient();

    const createIntegrationWorkflowMutation = useCreateIntegrationWorkflowMutation({
        onSuccess: (workflow) => {
            navigate(`/embedded/integrations/${integration.id}/workflows/${workflow?.id}`);

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

    return (
        <>
            <div className="flex w-full items-center justify-between rounded-md px-2 py-5 hover:bg-gray-50">
                <div className="flex-1">
                    <div className="flex items-center justify-between">
                        <div className="relative flex items-center gap-3">
                            <Link className="flex items-center gap-2" to={`/embedded/integrations/${integration?.id}`}>
                                {componentDefinition?.icon && (
                                    <InlineSVG className="size-6 flex-none" src={componentDefinition.icon} />
                                )}

                                <span className="text-base font-semibold text-gray-900">
                                    {componentDefinition?.title}
                                </span>
                            </Link>

                            {integration.category && (
                                <span className="text-xs uppercase text-gray-700">{integration.category.name}</span>
                            )}
                        </div>
                    </div>

                    <div className="relative mt-2 sm:flex sm:items-center sm:justify-between">
                        <div className="flex items-center">
                            {integration.workflowIds && (
                                <CollapsibleTrigger
                                    className="group mr-4 flex text-xs font-semibold text-gray-700"
                                    disabled={integration.workflowIds?.length === 0}
                                >
                                    <div className="mr-1">
                                        {integration.workflowIds?.length === 1
                                            ? `${integration.workflowIds?.length} workflow`
                                            : `${integration.workflowIds?.length} workflows`}
                                    </div>

                                    {integration.workflowIds?.length > 1 && (
                                        <ChevronDownIcon className="duration-300 group-data-[state=open]:rotate-180" />
                                    )}
                                </CollapsibleTrigger>
                            )}

                            <div onClick={(event) => event.preventDefault()}>
                                {integration.tags && (
                                    <TagList
                                        getRequest={(id, tags) => ({
                                            id: id!,
                                            updateTagsRequestModel: {
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
                    <div className="flex flex-col items-end gap-y-4">
                        <Badge
                            variant={
                                integration.status === IntegrationModelStatusEnum.Published ? 'success' : 'secondary'
                            }
                        >
                            {integration.status === IntegrationModelStatusEnum.Published
                                ? `Published V${integration.integrationVersion}`
                                : 'Not Published'}
                        </Badge>

                        <Tooltip>
                            <TooltipTrigger>
                                <div className="flex items-center text-sm text-gray-500 sm:mt-0">
                                    {integration.status === IntegrationModelStatusEnum.Published ? (
                                        <span>
                                            {`Published at ${integration.publishedDate?.toLocaleDateString()} ${integration.publishedDate?.toLocaleTimeString()}`}
                                        </span>
                                    ) : (
                                        '-'
                                    )}
                                </div>
                            </TooltipTrigger>

                            <TooltipContent>Last Published Date</TooltipContent>
                        </Tooltip>
                    </div>

                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <Button size="icon" variant="ghost">
                                <DotsVerticalIcon className="size-4 hover:cursor-pointer" />
                            </Button>
                        </DropdownMenuTrigger>

                        <DropdownMenuContent align="end">
                            <DropdownMenuItem onClick={() => setShowEditDialog(true)}>Edit</DropdownMenuItem>

                            <DropdownMenuItem disabled={true} onClick={() => setShowWorkflowDialog(true)}>
                                New Workflow
                            </DropdownMenuItem>

                            <DropdownMenuSeparator />

                            <DropdownMenuItem className="text-red-600" onClick={() => setShowDeleteDialog(true)}>
                                Delete
                            </DropdownMenuItem>
                        </DropdownMenuContent>
                    </DropdownMenu>
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
                            className="bg-red-600"
                            onClick={() => {
                                if (integration.id) {
                                    deleteIntegrationMutation.mutate({
                                        id: integration.id,
                                    });
                                }
                            }}
                        >
                            Delete
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>

            {showEditDialog && <IntegrationDialog integration={integration} onClose={() => setShowEditDialog(false)} />}

            {showWorkflowDialog && !!integration.id && (
                <WorkflowDialog
                    createWorkflowMutation={createIntegrationWorkflowMutation}
                    onClose={() => setShowWorkflowDialog(false)}
                    parentId={integration.id}
                />
            )}
        </>
    );
};

export default IntegrationListItem;
