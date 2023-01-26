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
        <div className="flex w-4/5 place-self-center">
            <ul role="list" className="space-y-3">
                {isLoading && 'Loading...'}

                {error &&
                    !isLoading &&
                    `An error has occurred: ${error.message}`}

                {!isLoading &&
                    !error &&
                    items &&
                    items.map((integration) => (
                        <div key={integration.id}>
                            <Link to={`/integrations/${integration.id}`}>
                                <li className="overflow-hidden rounded-md bg-white px-4 py-2 shadow-2xl">
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
                                    />
                                </li>
                            </Link>
                        </div>
                    ))}
            </ul>
        </div>
    );
};
export default IntegrationList;
