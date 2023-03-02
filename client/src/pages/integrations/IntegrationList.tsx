import {
    useGetIntegrationsQuery,
    useGetIntegrationTagsQuery,
} from '../../queries/integrations';
import IntegrationItem from 'pages/integrations/IntegrationItem';
import {Link, useSearchParams} from 'react-router-dom';

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
        <div className="flex place-self-center px-2 sm:w-full 2xl:w-4/5">
            <ul role="list" className="w-full divide-y divide-gray-100">
                {isLoading && 'Loading...'}

                {error &&
                    !isLoading &&
                    `An error has occurred: ${error.message}`}

                {!isLoading &&
                    !error &&
                    (integrations?.length === 0 ? (
                        <p>You do not have any Integration created yet.</p>
                    ) : (
                        integrations.map((integration) => {
                            const integrationTagIds = integration.tags?.map(
                                (tag) => tag.id
                            );

                            return (
                                <div key={integration.id}>
                                    <Link
                                        to={`/automation/integrations/${integration.id}`}
                                    >
                                        <li className="group my-3 flex items-center justify-between rounded-md bg-white p-2 hover:bg-gray-50">
                                            <IntegrationItem
                                                componentVersion={undefined} // missing api
                                                integration={integration}
                                                integrationNames={integrations.map(
                                                    (integration) =>
                                                        integration.name
                                                )}
                                                key={integration.id}
                                                lastDatePublished={undefined} // missing api
                                                published={false} // missing api
                                                remainingTags={tags?.filter(
                                                    (tag) =>
                                                        !integrationTagIds?.includes(
                                                            tag.id
                                                        )
                                                )}
                                            />
                                        </li>
                                    </Link>
                                </div>
                            );
                        })
                    ))}
            </ul>
        </div>
    );
};
export default IntegrationList;
