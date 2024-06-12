import {ConnectionDefinitionModel} from '@/shared/middleware/platform/configuration';
import {ConnectionModel} from '@/shared/middleware/platform/connection';

const ConnectionParameters = ({
    connection,
    connectionDefinition,
}: {
    connection: ConnectionModel;
    connectionDefinition: ConnectionDefinitionModel;
}) => {
    const authorizations = connectionDefinition.authorizations?.filter(
        (authorization) =>
            authorization.properties &&
            authorization.properties.filter((property) => !!connection.authorizationParameters![property.name!])
                .length > 0
    );

    return (
        <div className="flex flex-col gap-1">
            <div className="text-sm font-medium">Connection Parameters</div>

            <div className="flex flex-col gap-3 text-sm">
                <div>
                    {connectionDefinition.properties &&
                        connection.connectionParameters &&
                        Object.keys(connection.connectionParameters).length > 0 &&
                        connectionDefinition.properties
                            .filter((property) => !!connection.connectionParameters![property.name!])
                            .map((property) => (
                                <div className="flex" key={property.name}>
                                    <div className="w-4/12">{property.name}:</div>

                                    <div className="col-span-2">{connection.connectionParameters![property.name!]}</div>
                                </div>
                            ))}
                </div>

                {authorizations &&
                    connection.authorizationParameters &&
                    Object.keys(connection.authorizationParameters).length > 0 &&
                    authorizations.map((authorization) => (
                        <div key={authorization.name}>
                            <div className="flex">
                                <div className="w-4/12 font-medium">Authorization:</div>

                                <div className="col-span-2 font-medium">{authorization.title}</div>
                            </div>

                            {authorization.properties &&
                                authorization.properties
                                    .filter((property) => !!connection.authorizationParameters![property.name!])
                                    .map((property) => (
                                        <div className="flex" key={property.name}>
                                            <div className="w-4/12">{property.name}:</div>

                                            <div className="col-span-2">
                                                {connection.authorizationParameters![property.name!]}
                                            </div>
                                        </div>
                                    ))}
                        </div>
                    ))}
            </div>
        </div>
    );
};

export default ConnectionParameters;
