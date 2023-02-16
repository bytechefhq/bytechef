import {
    useGetIntegrationsQuery,
    useGetIntegrationTagsQuery,
} from '../../queries/integrations.queries';
import IntegrationItem from 'pages/integrations/IntegrationItem';
import {Link, useSearchParams} from 'react-router-dom';

const IntegrationList = () => {
    const [searchParams] = useSearchParams();

    const {
        isLoading,
        error,
        data: integrations,
    } = useGetIntegrationsQuery({
        categoryId: searchParams.get('categoryId')
            ? parseInt(searchParams.get('categoryId')!)
            : undefined,
        tagId: searchParams.get('tagId')
            ? parseInt(searchParams.get('tagId')!)
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
                                        onClick={(event) => {
                                            event.preventDefault();
                                        }}
                                    >
                                        <li className="group my-3 rounded-md bg-white p-2 hover:bg-gray-50">
                                            <IntegrationItem
                                                key={integration.id}
                                                category={integration.category}
                                                description={
                                                    integration.description
                                                }
                                                id={integration.id}
                                                lastDatePublished={
                                                    undefined // missing lastDatePublished
                                                }
                                                name={integration.name}
                                                published={false} // missing api
                                                remainingTags={tags?.filter(
                                                    (tag) =>
                                                        !integrationTagIds?.includes(
                                                            tag.id
                                                        )
                                                )}
                                                tags={integration.tags}
                                                version={integration.version}
                                                workflowIds={
                                                    integration.workflowIds
                                                }
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
