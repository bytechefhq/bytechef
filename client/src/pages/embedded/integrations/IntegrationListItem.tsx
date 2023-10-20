import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';
import {Link} from 'react-router-dom';

import AlertDialog from '../../../components/AlertDialog/AlertDialog';
import Badge from '../../../components/Badge/Badge';
import DropdownMenu, {
    IDropdownMenuItem,
} from '../../../components/DropdownMenu/DropdownMenu';
import HoverCard from '../../../components/HoverCard/HoverCard';
import TagList from '../../../components/TagList/TagList';
import WorkflowDialog from '../../../components/WorkflowDialog/WorkflowDialog';
import {
    IntegrationModel,
    IntegrationModelStatusEnum,
    TagModel,
} from '../../../middleware/integration';
import {
    useCreateIntegrationWorkflowRequestMutation,
    useDeleteIntegrationMutation,
    useUpdateIntegrationTagsMutation,
} from '../../../mutations/integrations.mutations';
import {IntegrationKeys} from '../../../queries/integrations.queries';
import IntegrationDialog from './IntegrationDialog';

interface IntegrationListItemProps {
    integration: IntegrationModel;
    remainingTags?: TagModel[];
}

const IntegrationListItem = ({
    integration,
    remainingTags,
}: IntegrationListItemProps) => {
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showWorkflowDialog, setShowWorkflowDialog] = useState(false);

    const dropdownItems: IDropdownMenuItem[] = [
        {
            label: 'Edit',
            onClick: () => {
                setShowEditDialog(true);
            },
        },
        {
            label: 'New Workflow',
            onClick: () => {
                setShowWorkflowDialog(true);
            },
        },
        {
            separator: true,
        },
        {
            danger: true,
            label: 'Delete',
            onClick: () => setShowDeleteDialog(true),
        },
    ];

    const queryClient = useQueryClient();

    const createIntegrationWorkflowRequestMutation =
        useCreateIntegrationWorkflowRequestMutation({
            onSuccess: () => {
                queryClient.invalidateQueries(IntegrationKeys.integrations);

                setShowWorkflowDialog(false);
            },
        });

    const deleteIntegrationMutation = useDeleteIntegrationMutation({
        onSuccess: () => {
            queryClient.invalidateQueries(IntegrationKeys.integrations);
            queryClient.invalidateQueries(
                IntegrationKeys.integrationCategories
            );
            queryClient.invalidateQueries(IntegrationKeys.integrationTags);
        },
    });

    const updateIntegrationTagsMutation = useUpdateIntegrationTagsMutation({
        onSuccess: () => {
            queryClient.invalidateQueries(IntegrationKeys.integrations);
            queryClient.invalidateQueries(IntegrationKeys.integrationTags);
        },
    });

    return (
        <>
            <div className="flex items-center">
                <Link
                    className="flex-1 pr-8"
                    to={`/automation/integrations/${integration.id}`}
                >
                    <div className="flex items-center justify-between">
                        <div className="relative flex items-center">
                            {integration.description ? (
                                <HoverCard text={integration.description}>
                                    <span className="mr-2 text-base font-semibold text-gray-900">
                                        {integration.name}
                                    </span>
                                </HoverCard>
                            ) : (
                                <span className="mr-2 text-base font-semibold text-gray-900">
                                    {integration.name}
                                </span>
                            )}

                            {integration.category && (
                                <span className="text-xs uppercase text-gray-700">
                                    {integration.category.name}
                                </span>
                            )}
                        </div>

                        <div className="ml-2 flex shrink-0">
                            <Badge
                                color={
                                    integration.status ===
                                    IntegrationModelStatusEnum.Published
                                        ? 'green'
                                        : 'default'
                                }
                                text={
                                    integration.status ===
                                    IntegrationModelStatusEnum.Published
                                        ? `Published V${integration.integrationVersion}`
                                        : 'Not Published'
                                }
                            />
                        </div>
                    </div>

                    <div className="mt-2 sm:flex sm:items-center sm:justify-between">
                        <div
                            className="flex h-[38px] items-center"
                            onClick={(event) => event.preventDefault()}
                        >
                            <div className="mr-4 text-xs font-semibold text-gray-700">
                                {integration.workflowIds?.length === 1
                                    ? `${integration.workflowIds?.length} workflow`
                                    : `${integration.workflowIds?.length} workflows`}
                            </div>

                            {integration.tags && (
                                <TagList
                                    id={integration.id!}
                                    remainingTags={remainingTags}
                                    tags={integration.tags}
                                    updateTagsMutation={
                                        updateIntegrationTagsMutation
                                    }
                                    getRequest={(id, tags) => ({
                                        id: id!,
                                        updateProjectTagsRequestModel: {
                                            tags: tags || [],
                                        },
                                    })}
                                />
                            )}
                        </div>

                        <div className="mt-2 flex items-center text-sm text-gray-500 sm:mt-0">
                            {integration.status ===
                            IntegrationModelStatusEnum.Published
                                ? `${integration.createdDate?.toLocaleDateString()}`
                                : '-'}
                        </div>
                    </div>
                </Link>

                <DropdownMenu id={integration.id} menuItems={dropdownItems} />
            </div>

            {showEditDialog && (
                <IntegrationDialog
                    integration={integration}
                    showTrigger={false}
                    visible
                    onClose={() => setShowEditDialog(false)}
                />
            )}

            {showDeleteDialog && (
                <AlertDialog
                    danger
                    isOpen
                    message="This action cannot be undone. This will permanently delete the integration and workflows it contains."
                    title="Are you absolutely sure?"
                    setIsOpen={setShowDeleteDialog}
                    onConfirmClick={() => {
                        if (integration.id) {
                            deleteIntegrationMutation.mutate({
                                id: integration.id,
                            });
                        }
                    }}
                />
            )}

            {showWorkflowDialog && !!integration.id && (
                <WorkflowDialog
                    id={integration.id}
                    visible
                    createWorkflowRequestMutation={
                        createIntegrationWorkflowRequestMutation
                    }
                />
            )}
        </>
    );
};

export default IntegrationListItem;
