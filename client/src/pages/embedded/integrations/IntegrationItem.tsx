import {useState} from 'react';
import DropdownMenu, {
    IDropdownMenuItem,
} from '../../../components/DropdownMenu/DropdownMenu';
import {
    IntegrationModel,
    IntegrationModelStatusEnum,
    TagModel,
} from '../../../middleware/integration';
import {
    useDeleteIntegrationMutation,
    useUpdateIntegrationTagsMutation,
    useCreateIntegrationWorkflowRequestMutation,
} from '../../../mutations/integrations.mutations';
import {IntegrationKeys} from '../../../queries/integrations.queries';
import {useQueryClient} from '@tanstack/react-query';
import {twMerge} from 'tailwind-merge';
import {Link} from 'react-router-dom';
import IntegrationDialog from './IntegrationDialog';
import Name from './components/Name';
import AlertDialog from '../../../components/AlertDialog/AlertDialog';
import TagList from '../../../components/TagList/TagList';
import WorkflowDialog from '../../../components/WorkflowDialog/WorkflowDialog';

interface IntegrationItemProps {
    integration: IntegrationModel;
    remainingTags?: TagModel[];
}

const IntegrationItem = ({
    integration,
    remainingTags,
}: IntegrationItemProps) => {
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
                    className="flex-1"
                    to={`/automation/integrations/${integration.id}`}
                >
                    <div className="flex justify-between">
                        <div>
                            <header className="relative mb-2 flex items-center">
                                {integration.description ? (
                                    <Name
                                        description={integration.description}
                                        name={integration.name}
                                    />
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
                            </header>

                            <footer
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
                                            updateIntegrationTagsRequestModel: {
                                                tags: tags || [],
                                            },
                                        })}
                                    />
                                )}
                            </footer>
                        </div>

                        <aside className="flex items-center">
                            <span
                                className={twMerge(
                                    'mr-4 rounded px-2.5 py-0.5 text-sm font-medium',
                                    integration.status ===
                                        IntegrationModelStatusEnum.Published
                                        ? 'bg-green-100 text-green-800 dark:bg-green-200 dark:text-green-900'
                                        : 'bg-gray-100 text-gray-800 dark:bg-gray-200 dark:text-gray-900'
                                )}
                            >
                                {integration.status ===
                                IntegrationModelStatusEnum.Published
                                    ? `Published V${integration.integrationVersion}`
                                    : 'Not Published'}
                            </span>

                            <span className="mr-4 w-[76px] text-center text-sm text-gray-500">
                                {integration.status ===
                                IntegrationModelStatusEnum.Published
                                    ? integration.lastPublishedDate?.toLocaleDateString()
                                    : '-'}
                            </span>
                        </aside>
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

            {showWorkflowDialog && (
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

export default IntegrationItem;
