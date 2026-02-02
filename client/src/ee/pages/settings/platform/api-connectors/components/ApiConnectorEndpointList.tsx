import {ApiConnector} from '@/shared/middleware/graphql';

import ApiConnectorEndpointListItem from './ApiConnectorEndpointListItem';

interface ApiConnectorEndpointListProps {
    apiConnector: ApiConnector;
}

const ApiConnectorEndpointList = ({apiConnector}: ApiConnectorEndpointListProps) => {
    return (
        <div className="border-b border-b-border/50 py-3 pl-4">
            <h3 className="flex justify-start pl-2 text-sm font-semibold uppercase text-gray-400">Endpoints</h3>

            <ul>
                {apiConnector.endpoints?.map((apiConnectorEndpoint) => (
                    <li
                        className="flex items-center justify-between rounded-md p-2 hover:bg-gray-50"
                        key={apiConnectorEndpoint.id}
                    >
                        {apiConnectorEndpoint && (
                            <ApiConnectorEndpointListItem
                                apiConnectorEndpoint={apiConnectorEndpoint}
                                apiConnectorName={apiConnector.name}
                                specification={apiConnector.specification ?? undefined}
                            />
                        )}
                    </li>
                ))}
            </ul>
        </div>
    );
};

export default ApiConnectorEndpointList;
