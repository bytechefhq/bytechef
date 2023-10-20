import {useState} from 'react';
import DropdownMenu, {
    DropdownMenuItemProps,
} from '../../components/DropdownMenu/DropdownMenu';
import {
    IntegrationModel,
    StatusModel,
    TagModel,
} from '../../middleware/integration';
import {
    useDeleteIntegrationMutation,
    useCreateIntegrationMutation,
} from '../../mutations/integrations.mutations';
import {IntegrationKeys} from '../../queries/integrations';
import {useQueryClient} from '@tanstack/react-query';
import {twMerge} from 'tailwind-merge';
import {Link} from 'react-router-dom';
import IntegrationModal from './IntegrationModal';
import TagList from './components/TagList';
import duplicate from './utils/duplicate';
import Name from './components/Name';
import AlertDialog from '../../components/AlertDialog/AlertDialog';
import WorkflowModal from './WorkflowModal';

interface IntegrationItemProps {
    integration: IntegrationModel;
    integrationNames: string[];
    remainingTags?: TagModel[];
}

const IntegrationItem = ({
    integration,
    remainingTags,
    integrationNames,
}: IntegrationItemProps) => {
    const [showEditModal, setShowEditModal] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showWorkflowModal, setShowWorkflowModal] = useState(false);

    const dropdownItems: DropdownMenuItemProps[] = [
        {
            label: 'Edit',
            onClick: () => {
                setShowEditModal(true);
            },
        },
        {
            label: 'Duplicate',
            onClick: () => {
                duplicate(
                    integration!,
                    integrationNames,
                    createIntegrationMutation
                );
            },
        },
        {
            label: 'New Workflow',
            onClick: () => {
                setShowWorkflowModal(true);
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

    const createIntegrationMutation = useCreateIntegrationMutation({
        onSuccess: () => {
            queryClient.invalidateQueries(IntegrationKeys.integrations);
            queryClient.invalidateQueries(
                IntegrationKeys.integrationCategories
            );
            queryClient.invalidateQueries(IntegrationKeys.integrationTags);
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
                                        integrationId={integration.id!}
                                        remainingTags={remainingTags}
                                        tags={integration.tags}
                                    />
                                )}
                            </footer>
                        </div>

                        <aside className="flex items-center">
                            <span
                                className={twMerge(
                                    'mr-4 rounded px-2.5 py-0.5 text-sm font-medium',
                                    integration.status === StatusModel.Published
                                        ? 'bg-green-100 text-green-800 dark:bg-green-200 dark:text-green-900'
                                        : 'bg-gray-100 text-gray-800 dark:bg-gray-200 dark:text-gray-900'
                                )}
                            >
                                {integration.status === StatusModel.Published
                                    ? `Published V${integration.integrationVersion}`
                                    : 'Not Published'}
                            </span>

                            <span className="mr-4 w-[76px] text-center text-sm text-gray-500">
                                {integration.status === StatusModel.Published
                                    ? integration.lastPublishedDate?.toLocaleDateString()
                                    : '-'}
                            </span>
                        </aside>
                    </div>
                </Link>

                <DropdownMenu
                    id={integration.id}
                    menuItems={dropdownItems}
                />
            </div>

            {showEditModal && (
                <IntegrationModal
                    integration={integration}
                    showTrigger={false}
                    visible
                    onClose={() => setShowEditModal(false)}
                />
            )}

            {showDeleteDialog && (
                <AlertDialog
                    danger
                    isOpen
                    message="This action cannot be undone. This will permanently delete the project and workflows it contains."
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

            {showWorkflowModal && (
                <WorkflowModal
                    id={integration.id}
                    visible
                    version={undefined}
                />
            )}
        </>
    );
};

export default IntegrationItem;
