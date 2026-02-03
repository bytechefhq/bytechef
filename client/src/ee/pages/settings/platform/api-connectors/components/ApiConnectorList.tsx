import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';

// TODO: Uncomment when api-connector middleware is implemented
// import {ApiConnector} from '@/ee/shared/middleware/platform/api-connector';

import ApiConnectorEndpointList, {type ApiConnectorEndpointI} from './ApiConnectorEndpointList';
import ApiConnectorListItem from './ApiConnectorListItem';

// TODO: Remove when ApiConnector type is available from middleware
export interface ApiConnectorI {
    description?: string;
    enabled: boolean;
    endpoints?: Array<ApiConnectorEndpointI>;
    id: string;
    lastModifiedDate?: Date;
    name: string;
    title: string;
}

const ApiConnectorList = ({apiConnectors}: {apiConnectors: ApiConnectorI[]}) => {
    return (
        <div className="w-full px-2 3xl:mx-auto 3xl:w-4/5">
            {apiConnectors.length > 0 && (
                <>
                    <div className="w-full divide-y divide-gray-100">
                        {apiConnectors.map((apiConnector) => {
                            return (
                                <Collapsible key={apiConnector.id}>
                                    <ApiConnectorListItem apiConnector={apiConnector} />

                                    <CollapsibleContent>
                                        <ApiConnectorEndpointList apiConnectorEndpoints={apiConnector.endpoints} />
                                    </CollapsibleContent>
                                </Collapsible>
                            );
                        })}
                    </div>
                </>
            )}
        </div>
    );
};

export default ApiConnectorList;
