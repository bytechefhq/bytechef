import {useState} from 'react';
import {
    Dropdown,
    DropdownMenuItemType,
} from '../../components/Dropdown/Dropdown';
import {IntegrationModel, TagModel} from '../../middleware/integration';
import {
    useIntegrationDeleteMutation,
    useIntegrationMutation,
} from '../../mutations/integrations.mutations';
import {useGetIntegrationQuery, IntegrationKeys} from 'queries/integrations';
import {useQueryClient} from '@tanstack/react-query';
import {twMerge} from 'tailwind-merge';
import IntegrationModal from './IntegrationModal';
import duplicate from './utils/duplicate';
import TagList from './components/TagList';
import Name from './components/Name';
import WorkflowModal from './WorkflowModal';

interface IntegrationItemProps {
    componentVersion: undefined;
    integration: IntegrationModel;
    integrationNames: string[];
    lastDatePublished: Date | undefined;
    published: boolean;
    remainingTags?: TagModel[];
}

const IntegrationItem = ({
    componentVersion,
    integration,
    integrationNames,
    lastDatePublished,
    published,
    remainingTags,
}: IntegrationItemProps) => {
    const [showEditModal, setShowEditModal] = useState(false);
    const [showWorkflowModal, setShowWorkflowModal] = useState(false);

    const {category, description, id, name, tags, workflowIds} = integration;

    const {data: integrationItem} = useGetIntegrationQuery(id!);

    const dropdownItems: DropdownMenuItemType[] = [
        {
            label: 'Edit',
            onClick: (id: number, event: React.MouseEvent) => {
                event.preventDefault();

                setShowEditModal(true);
            },
        },
        {
            label: 'Duplicate',
            onClick: (id: number, event: React.MouseEvent) => {
                event.preventDefault();

                duplicate(
                    integrationItem!,
                    integrationNames,
                    duplicateMutation
                );
            },
        },
        {
            label: 'New Workflow',
            onClick: (id: number, event: React.MouseEvent) => {
                event.preventDefault();

                setShowWorkflowModal(true);
            },
        },
        {
            separator: true,
        },
        {
            danger: true,
            label: 'Delete',
            onClick: (id: number, event: React.MouseEvent) => {
                event.preventDefault();

                if (
                    confirm('Are you sure you want to delete this integration?')
                ) {
                    if (id) deletion.mutate({id});
                }
            },
        },
    ];

    const queryClient = useQueryClient();

    const duplicateMutation = useIntegrationMutation({
        onSuccess: (data) => {
            queryClient.invalidateQueries(IntegrationKeys.integrations);

            queryClient.invalidateQueries(
                IntegrationKeys.integrationCategories
            );

            queryClient.invalidateQueries(IntegrationKeys.integrationTags);

            const integrationsData = queryClient.getQueryData<
                IntegrationModel[]
            >(IntegrationKeys.integrations);

            if (integrationsData) {
                queryClient.setQueryData<IntegrationModel[]>(
                    IntegrationKeys.integrations,
                    [...integrationsData, data]
                );
            }
        },
    });

    const deletion = useIntegrationDeleteMutation({
        onSuccess: () => {
            queryClient.invalidateQueries(IntegrationKeys.integrations);
        },
    });

    return (
        <>
            <div>
                <header className="relative mb-2 flex items-center">
                    {description ? (
                        <Name description={description} name={name} />
                    ) : (
                        <span className="mr-2 text-base font-semibold text-gray-900">
                            {name}
                        </span>
                    )}

                    {category && (
                        <span className="text-xs uppercase text-gray-700">
                            {category.name}
                        </span>
                    )}
                </header>

                <footer
                    className="flex h-[38px] items-center"
                    onClick={(event) => event.preventDefault()}
                >
                    <div className="mr-4 text-xs font-semibold text-gray-700">
                        {workflowIds?.length === 1
                            ? `${workflowIds?.length} workflow`
                            : `${workflowIds?.length} workflows`}
                    </div>

                    {tags && !!id && (
                        <TagList
                            integrationItemId={id}
                            remainingTags={remainingTags}
                            tags={tags}
                        />
                    )}
                </footer>
            </div>

            <aside className="flex items-center">
                <span
                    className={twMerge(
                        'mr-4 rounded px-2.5 py-0.5 text-sm font-medium',
                        published
                            ? 'bg-green-100 text-green-800 dark:bg-green-200 dark:text-green-900'
                            : 'bg-gray-100 text-gray-800 dark:bg-gray-200 dark:text-gray-900'
                    )}
                >
                    {published
                        ? `Published V${componentVersion}`
                        : 'Not Published'}
                </span>

                <span className="mr-4 w-[76px] text-center text-sm text-gray-500">
                    {lastDatePublished && published
                        ? lastDatePublished.toLocaleDateString()
                        : '-'}
                </span>

                <Dropdown id={id} menuItems={dropdownItems} />
            </aside>

            {showEditModal && (
                <IntegrationModal
                    id={id}
                    integrationItem={integrationItem}
                    visible
                />
            )}

            {showWorkflowModal && (
                <WorkflowModal id={id} visible version={undefined} />
            )}
        </>
    );
};

export default IntegrationItem;
function setShowTrigger(arg0: boolean) {
    throw new Error('Function not implemented.');
}
