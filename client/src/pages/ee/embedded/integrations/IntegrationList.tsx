import {
    useGetIntegrationsQuery,
    useGetIntegrationTagsQuery,
} from '../../../../queries/integrations.queries';
import IntegrationItem from 'pages/ee/embedded/integrations/IntegrationItem';
import {useSearchParams} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';
import EmptyList from '../../../../components/EmptyList/EmptyList';
import IntegrationDialog from './IntegrationDialog';
import {Square3Stack3DIcon} from '@heroicons/react/24/outline';

const IntegrationList = () => {
    const [searchParams] = useSearchParams();

    const {
        isLoading,
        error,
        data: integrations,
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
                'flex place-self-center px-2 sm:w-full 2xl:w-4/5',
                integrations?.length === 0 ? 'h-full items-center' : ''
            )}
        >
            <ul role="list" className="w-full divide-y divide-gray-100">
                {isLoading && 'Loading...'}

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
                                <div key={integration.id}>
                                    <li className="group my-3 rounded-md bg-white p-2 hover:bg-gray-50">
                                        <IntegrationItem
                                            integration={integration}
                                            key={integration.id}
                                            remainingTags={tags?.filter(
                                                (tag) =>
                                                    !integrationTagIds?.includes(
                                                        tag.id
                                                    )
                                            )}
                                        />
                                    </li>
                                </div>
                            );
                        })
                    ))}
            </ul>
        </div>
    );
};
export default IntegrationList;
