import {useGetIntegrationsQuery} from '../../queries/integrations.queries';
import React from 'react';
import {IntegrationItem} from 'components/IntegrationItem/IntegrationItem';
import {Link} from 'react-router-dom';

    const {isLoading, error, data: items} = useGetIntegrationsQuery();
const IntegrationList: React.FC = () => {

    return (
        <div>
            <ul role="list" className="space-y-3">
                {isLoading && 'Loading...'}

                {error &&
                    !isLoading &&
                    `An error has occurred: ${error.message}`}

                {!isLoading &&
                    !error &&
                    items &&
                    items.map((integration) => (
                        <li
                            key={integration.id}
                            className="overflow-hidden rounded-md bg-white px-6 py-4 shadow"
                        >
                            <Link to={`/integrations/${integration.id}`}>
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
                            </Link>
                        </li>
                    ))}
            </ul>
        </div>
    );
};

export default IntegrationList;
