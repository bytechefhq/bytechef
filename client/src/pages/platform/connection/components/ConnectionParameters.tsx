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
        <div className="flex w-8/12 flex-col gap-2">
            <div className="text-sm font-medium">Connection Parameters</div>

            <div className="flex flex-col gap-3 text-sm">
                {connectionDefinition.properties &&
                    connection.connectionParameters &&
                    Object.keys(connection.connectionParameters).length > 0 &&
                    connectionDefinition.properties
                        .filter((property) => !!connection.connectionParameters![property.name!])
                        .map((property) => (
                            <div className="flex justify-between" key={property.name}>
                                <div>{property.name}:</div>

                                <div>{connection.connectionParameters![property.name!]}</div>
                            </div>
                        ))}

                {authorizations &&
                    connection.authorizationParameters &&
                    Object.keys(connection.authorizationParameters).length > 0 &&
                    authorizations.map((authorization) => (
                        <div className="flex flex-col gap-1" key={authorization.name}>
                            <div className="font-medium">Authorization: {authorization.title}</div>

                            <div className="flex flex-col gap-1">
                                {authorization.properties &&
                                    authorization.properties
                                        .filter((property) => !!connection.authorizationParameters![property.name!])
                                        .map((property) => (
                                            <div className="flex justify-between" key={property.name}>
                                                <div>{property.name}:</div>

                                                <div>{connection.authorizationParameters![property.name!]}</div>
                                            </div>
                                        ))}
                            </div>
                        </div>
                    ))}
            </div>
        </div>
    );
};

export default ConnectionParameters;
