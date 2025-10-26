import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import {ApiConnector} from '@/ee/shared/middleware/platform/api-connector';

import ApiConnectorEndpointList from './ApiConnectorEndpointList';
import ApiConnectorListItem from './ApiConnectorListItem';

const ApiConnectorList = ({apiConnectors}: {apiConnectors: ApiConnector[]}) => {
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
