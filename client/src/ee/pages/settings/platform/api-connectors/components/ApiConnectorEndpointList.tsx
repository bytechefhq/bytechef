// TODO: Uncomment when api-connector middleware is implemented
// import {ApiConnectorEndpoint} from '@/ee/shared/middleware/platform/api-connector';

import ApiConnectorEndpointListItem from './ApiConnectorEndpointListItem';

// TODO: Remove when ApiConnectorEndpoint type is available from middleware
export interface ApiConnectorEndpointI {
    httpMethod: string;
    id: string;
    lastExecutionDate?: Date;
    name: string;
    path: string;
}

const ApiConnectorEndpointList = ({apiConnectorEndpoints}: {apiConnectorEndpoints?: Array<ApiConnectorEndpointI>}) => {
    return (
        <div className="border-b border-b-border/50 py-3 pl-4">
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
