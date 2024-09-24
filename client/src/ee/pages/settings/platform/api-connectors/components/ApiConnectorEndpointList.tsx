import {ApiConnectorEndpoint} from '@/middleware/platform/api-connector';

import ApiConnectorEndpointListItem from './ApiConnectorEndpointListItem';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const ApiConnectorEndpointList = ({apiConnectorEndpoints}: {apiConnectorEndpoints?: Array<ApiConnectorEndpoint>}) => {
    return (
        <div className="border-b border-b-gray-100 py-3 pl-4">
            <h3 className="flex justify-start pl-2 text-sm font-semibold uppercase text-gray-400">Endpoints</h3>

            <ul>
                {apiConnectorEndpoints?.map((apiConnectorEndpoint) => (
                    <li
                        className="flex items-center justify-between rounded-md p-2 hover:bg-gray-50"
                        key={apiConnectorEndpoint.id}
                    >
                        {apiConnectorEndpoint && (
                            <ApiConnectorEndpointListItem apiConnectorEndpoint={apiConnectorEndpoint} />
                        )}
                    </li>
                ))}
            </ul>
        </div>
    );
};

export default ApiConnectorEndpointList;
