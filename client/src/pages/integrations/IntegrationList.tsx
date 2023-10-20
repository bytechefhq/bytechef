import {useGetIntegrationsQuery} from '../../queries/integrations.queries';
import React from 'react';
import {IntegrationItem} from 'components/IntegrationItem/IntegrationItem';
import {useSearchParams} from 'react-router-dom';

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
