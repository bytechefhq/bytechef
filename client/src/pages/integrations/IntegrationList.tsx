import {useGetIntegrationsQuery} from '../../queries/integrations.queries';
import React from 'react';
import {IntegrationItem} from 'components/IntegrationItem/IntegrationItem';
import {Link, useSearchParams} from 'react-router-dom';

const IntegrationList: React.FC = () => {
    const [searchParams] = useSearchParams();

    const {
        isLoading,
        error,
        data: items,
    } = useGetIntegrationsQuery({
        categoryId: searchParams.get('categoryId')
            ? +searchParams.get('categoryId')!
            : undefined,
        tagId: searchParams.get('tagId')
            ? +searchParams.get('tagId')!
            : undefined,
    });

    return (
        <div className="flex place-self-center px-4 sm:w-full xl:w-4/5">
            <ul role="list" className="w-full divide-y divide-gray-100">
                {isLoading && 'Loading...'}

                {error &&
                    !isLoading &&
                    `An error has occurred: ${error.message}`}

                {!isLoading &&
                    !error &&
                    (items?.length === 0 ? (
                        <p>You do not have any Integration created yet.</p>
                    ) : (
                        items.map((integration) => (
                            <div key={integration.id}>
                                <Link to={`/integrations/${integration.id}`}>
                                    <li className="my-3 overflow-hidden rounded-md bg-white p-3 hover:bg-gray-50">
                                        <IntegrationItem
                                            key={integration.id}
                                            category={integration.category}
                                            id={integration.id}
                                            name={integration.name}
                                            description={
                                                integration.description
                                            }
                                            tags={integration.tags}
                                            workflowIds={
                                                integration.workflowIds
                                            }
                                            date={integration.lastModifiedDate}
                                            status={false} // missing api
                                            button={''}
                                        />
                                    </li>
                                </Link>
                            </div>
                        ))
                    ))}
            </ul>
        </div>
    );
};
export default IntegrationList;
