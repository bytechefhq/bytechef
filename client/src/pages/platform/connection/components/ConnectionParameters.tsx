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
                <div className="grid grid-cols-3 gap-1">
                    {connectionDefinition.properties &&
                        connection.connectionParameters &&
                        Object.keys(connection.connectionParameters).length > 0 &&
                        connectionDefinition.properties
                            .filter((property) => !!connection.connectionParameters![property.name!])
                            .map((property) => (
                                <>
                                    <div>{property.name}:</div>

                                    <div className="col-span-2">{connection.connectionParameters![property.name!]}</div>
                                </>
                            ))}
                </div>

                {authorizations &&
                    connection.authorizationParameters &&
                    Object.keys(connection.authorizationParameters).length > 0 &&
                    authorizations.map((authorization) => (
                        <div className="grid grid-cols-3 gap-1" key={authorization.name}>
                            <div className="font-medium">Authorization:</div>

                            <div className="col-span-2 font-medium">{authorization.title}</div>

                            {authorization.properties &&
                                authorization.properties
                                    .filter((property) => !!connection.authorizationParameters![property.name!])
                                    .map((property) => (
                                        <>
                                            <div>{property.name}:</div>

                                            <div className="col-span-2">
                                                {connection.authorizationParameters![property.name!]}
                                            </div>
                                        </>
                                    ))}
                        </div>
                    ))}
            </div>
        </div>
    );
};

export default ConnectionParameters;
