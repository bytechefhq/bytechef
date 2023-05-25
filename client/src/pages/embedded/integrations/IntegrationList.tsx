import {Square3Stack3DIcon} from '@heroicons/react/24/outline';
import {useSearchParams} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';

import EmptyList from '../../../components/EmptyList/EmptyList';
import {
    useGetIntegrationTagsQuery,
    useGetIntegrationsQuery,
} from '../../../queries/integrations.queries';
import IntegrationDialog from './IntegrationDialog';
import IntegrationListItem from './IntegrationListItem';

const IntegrationList = () => {
    const [searchParams] = useSearchParams();

    const {
        data: integrations,
        error,
        isLoading,
    } = useGetIntegrationsQuery({
        categoryIds: searchParams.get('categoryId')
            ? [parseInt(searchParams.get('categoryId')!)]
            : undefined,
        tagIds: searchParams.get('tagId')
            ? [parseInt(searchParams.get('tagId')!)]
            : undefined,
    });

    const {data: tags} = useGetIntegrationTagsQuery();

    return (
        <div
            className={twMerge(
                'w-full px-2 2xl:mx-auto 2xl:w-4/5',
                integrations?.length === 0 ? 'place-self-center' : ''
            )}
        >
            <ul role="list">
                {isLoading && <span className="px-2">Loading...</span>}

                {error &&
                    !isLoading &&
                    `An error has occurred: ${error.message}`}

                {!isLoading &&
                    !error &&
                    (integrations?.length === 0 ? (
                        <EmptyList
                            button={
                                <IntegrationDialog integration={undefined} />
                            }
                            icon={
                                <Square3Stack3DIcon className="h-12 w-12 text-gray-400" />
                            }
                            message="Get started by creating a new project."
                            title="No projects"
                        />
                    ) : (
                        integrations.map((integration) => {
                            const integrationTagIds = integration.tags?.map(
                                (tag) => tag.id
                            );

                            return (
                                <li key={integration.id}>
                                    <div className="group rounded-md border-b border-b-gray-100 bg-white p-2 py-3 hover:bg-gray-50">
                                        <IntegrationListItem
                                            integration={integration}
                                            key={integration.id}
                                            remainingTags={tags?.filter(
                                                (tag) =>
                                                    !integrationTagIds?.includes(
                                                        tag.id
                                                    )
                                            )}
                                        />
                                    </div>
                                </li>
                            );
                        })
                    ))}
            </ul>
        </div>
    );
};
export default IntegrationList;
