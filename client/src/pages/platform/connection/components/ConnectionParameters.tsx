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

    if (
        connection.connectionParameters &&
        Object.values(connection.connectionParameters).every((x) => x === null || x === '') &&
        connection.authorizationParameters &&
        Object.values(connection.authorizationParameters).every((x) => x === null || x === '')
    ) {
        return <></>;
    }

    return (
        <div className="flex flex-col gap-1">
            <div className="text-sm font-medium">Connection Parameters</div>

            <div className="flex flex-col gap-3 text-sm">
                <div className="flex gap-1">
                    {connectionDefinition.properties &&
                        connection.connectionParameters &&
                        Object.keys(connection.connectionParameters).length > 0 &&
                        connectionDefinition.properties
                            .filter((property) => !!connection.connectionParameters![property.name!])
                            .map((property) => (
                                <div className="flex w-full" key={property.name}>
                                    <div className="w-4/12 text-muted-foreground">{property.name}:</div>

                                    <div className="col-span-2">{connection.connectionParameters![property.name!]}</div>
                                </div>
                            ))}
                </div>

                {authorizations &&
                    connection.authorizationParameters &&
                    Object.keys(connection.authorizationParameters).length > 0 &&
                    authorizations.map((authorization) => (
                        <div className="flex w-full gap-1" key={authorization.name}>
                            <div className="flex">
                                <div className="w-4/12 font-medium text-muted-foreground">Authorization:</div>

                                <div className="col-span-2">{authorization.title}</div>
                            </div>

                            {authorization.properties &&
                                authorization.properties
                                    .filter((property) => !!connection.authorizationParameters![property.name!])
                                    .map((property) => (
                                        <div className="flex w-full" key={property.name}>
                                            <div className="w-4/12 text-muted-foreground">{property.name}:</div>

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
