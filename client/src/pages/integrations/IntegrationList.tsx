import {
    useGetIntegrationsQuery,
    useGetIntegrationTagsQuery,
} from '../../queries/integrations.queries';
import React from 'react';
import {IntegrationItem} from 'pages/integrations/IntegrationItem/IntegrationItem';
import {Link, useSearchParams} from 'react-router-dom';

const IntegrationList: React.FC = () => {
    const [searchParams] = useSearchParams();

    const {
        isLoading,
        error,
        data: integrations,
    } = useGetIntegrationsQuery({
        categoryId: searchParams.get('categoryId')
            ? +searchParams.get('categoryId')!
            : undefined,
        tagId: searchParams.get('tagId')
            ? +searchParams.get('tagId')!
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
                        integrations.map((integration) => (
                            <div key={integration.id}>
                                {/* <Link
                                    to={`/automation/integrations/${integration.id}`}
                                > */}
                                <li className="group my-3 rounded-md bg-white p-2 hover:bg-gray-50">
                                    <IntegrationItem
                                        key={integration.id}
                                        category={integration.category}
                                        id={integration.id}
                                        name={integration.name}
                                        description={integration.description}
                                        tags={integration.tags}
                                        workflowIds={integration.workflowIds}
                                        date={integration.lastModifiedDate}
                                        status={false} // missing api
                                        button={''}
                                        remainingTags={tags?.filter(
                                            (tag) =>
                                                integration.tags?.findIndex(
                                                    (x) => x.id === tag.id
                                                ) === -1
                                        )}
                                    />
                                </li>
                                {/* </Link> */}
                            </div>
                        ))
                    ))}
            </ul>
        </div>
    );
};
export default IntegrationList;
