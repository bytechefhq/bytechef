import {useGetIntegrations} from '../../queries/integrations.queries';
import React from 'react';
import {IntegrationItem} from 'components/IntegrationItem/IntegrationItem';
import {Dropdown} from '../../components/IntegrationItem/Dropdown';

export const IntegrationList: React.FC = () => {
    const {isLoading, error, data: items} = useGetIntegrations();

    return (
        <>
            <div>
                {isLoading && 'Loading...'}

                {error &&
                    !isLoading &&
                    `An error has occurred: ${error.message}`}

                {items &&
                    items.map((integration) => (
                        <IntegrationItem
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
                {!isLoading && !error && (
                    <ul
                        role="list"
                        className="divide-y divide-gray-100 dark:divide-gray-800"
                    >
                        {items.map((item) => (
                            <li key={item.id}>
                                <a
                                    href={''}
                                    className="block hover:bg-gray-50 dark:hover:bg-gray-700"
                                >
                                    <div className="flex items-center py-4">
                                        <div className="flex min-w-0 flex-1 items-center">
                                            <div className="min-w-0 flex-1 md:grid md:grid-cols-2 md:gap-4">
                                                <div>
                                                    <p className="truncate text-sm font-medium text-black dark:text-sky-400">
                                                        {item.name}
                                                    </p>

                                                    <p className="mt-2 flex items-center text-sm text-gray-500 dark:text-gray-300">
                                                        <span className="truncate">
                                                            {item.description}
                                                        </span>
                                                    </p>
                                                </div>

                                                <div className="hidden md:block">
                                                    <div>
                                                        <p className="text-sm text-gray-900">
                                                            {/*Applied on <time dateTime={item.createdDate}>{item.createdDate}</time>*/}
                                                        </p>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>

                                        <div>
                                            <Dropdown id={item.id} />
                                        </div>
                                    </div>
                                </a>
                            </li>
                        ))}
                    </ul>
                )}
            </div>
        </>
    );
};
