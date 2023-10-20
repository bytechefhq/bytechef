import {useGetIntegrationsQuery} from '../../queries/integrations.queries';
import React from 'react';
import {IntegrationItem} from 'components/IntegrationItem/IntegrationItem';

    const {isLoading, error, data: items} = useGetIntegrationsQuery();
const IntegrationList: React.FC = () => {

    return (
        <div>
            {isLoading && 'Loading...'}

            {error && !isLoading && `An error has occurred: ${error.message}`}

            {!isLoading &&
                !error &&
                items &&
                items.map((integration) => (
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
                ))}
        </div>
    );
};

export default IntegrationList;
