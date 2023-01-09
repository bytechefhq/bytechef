import {EllipsisVerticalIcon} from '@heroicons/react/20/solid';
import {useGetIntegrations} from '../../queries/integrations.queries';
import React from 'react';
import {IntegrationItem} from 'components/IntegrationItem/IntegrationItem';

export const IntegrationList: React.FC = () => {
    const {isLoading, error, data: items} = useGetIntegrations();

    const integrations = [
        {
            category: 'Category: CRM',
            createdBy: 'Comp',
            createdDate: '2022-12-22T21:28:07.051Z',
            id: '888',
            name: 'Pipedrive',
            description: 'string',
            lastModifiedBy: 'string',
            lastModifiedDate: 'Last modified 16.05.2023.',
            tags: ['Tag1', 'Tag2'],
            workflowIds: ['string'],
            status: true,
        },
        {
            category: 'Category: CRM',
            createdBy: 'Com',
            createdDate: '2022-12-22T21:28:07.051Z',
            id: '777',
            name: 'Salesforce',
            description: 'string',
            lastModifiedBy: 'string',
            lastModifiedDate: 'Last modified 16.05.2023.',
            tags: ['Tag1'],
            workflowIds: ['string'],
            status: true,
        },
        {
            category: 'Category: Marketing',
            createdBy: 'Copm',
            createdDate: '2022-12-22T21:28:07.051Z',
            id: '666',
            name: 'Pipedrive',
            description: 'string',
            lastModifiedBy: 'string',
            lastModifiedDate: 'Last modified 16.05.2023.',
            tags: ['Tag1', 'Tag2', 'Tag3'],
            workflowIds: ['string'],
            status: false,
        },
    ];

    return (
        <>
            {integrations.map((integration) => (
                <IntegrationItem
                    category={integration.category}
                    id={integration.id}
                    name={integration.name}
                    description={integration.description}
                    tags={integration.tags}
                    workflowIds={integration.workflowIds}
                    date={integration.lastModifiedDate}
                    status={integration.status}
                    button={''}
                />
            ))}
            <div>
                {isLoading && 'Loading...'}

                {error &&
                    !isLoading &&
                    `An error has occurred: ${error.message}`}

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
                                            <EllipsisVerticalIcon
                                                className="h-5 w-5 text-gray-400"
                                                aria-hidden="true"
                                            />
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
