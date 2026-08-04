import {ApiConnector} from '@/shared/middleware/graphql';

import ApiConnectorListItem from './ApiConnectorListItem';

const ApiConnectorList = ({apiConnectors}: {apiConnectors: ApiConnector[]}) => {
    return (
        <div className="w-full px-6 3xl:mx-auto 3xl:w-4/5">
            {apiConnectors.length > 0 && (
                <div className="w-full">
                    {apiConnectors.map((apiConnector) => (
                        <ApiConnectorListItem apiConnector={apiConnector} key={apiConnector.id} />
                    ))}
                </div>
            )}
        </div>
    );
};

export default ApiConnectorList;
