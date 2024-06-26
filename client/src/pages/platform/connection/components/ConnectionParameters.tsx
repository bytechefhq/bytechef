import {ConnectionDefinitionModel} from '@/shared/middleware/platform/configuration';
import {ConnectionModel} from '@/shared/middleware/platform/connection';

const ConnectionParameters = ({
    connection,
    connectionDefinition,
}: {
    connection: ConnectionModel;
    connectionDefinition: ConnectionDefinitionModel;
}) => {
    const {authorizations, properties: connectionProperties} = connectionDefinition;
    const {authorizationParameters, connectionParameters} = connection;

    const existingAuthorizations = authorizations?.filter(
        (authorization) =>
            authorization.properties &&
            authorization.properties.filter((property) => !!authorizationParameters![property.name!]).length > 0
    );

    if (
        connectionParameters &&
        Object.values(connectionParameters).every((parameter) => parameter === null || parameter === '') &&
        authorizationParameters &&
        Object.values(authorizationParameters).every((parameter) => parameter === null || parameter === '')
    ) {
        return <></>;
    }

    const hasConnectionParameters =
        connectionProperties && connectionParameters && !!Object.keys(connectionParameters).length;

    const hasAuthorizationParameters =
        existingAuthorizations && authorizationParameters && !!Object.keys(authorizationParameters).length;

    return (
        <div className="mt-4 space-y-4">
            <h2 className="heading-tertiary text-center">
                {hasConnectionParameters ? 'Connection' : 'Authorization'} Parameters
            </h2>

            <ul className="flex w-full flex-col text-sm">
                {hasConnectionParameters &&
                    connectionProperties!
                        .filter((property) => !!connectionParameters![property.name!])
                        .map((property) => (
                            <li className="flex w-full justify-between" key={property.name}>
                                <span className="text-muted-foreground">{property.name}:</span>

                                <pre className="self-end text-xs">{connectionParameters![property.name!]}</pre>
                            </li>
                        ))}

                {hasAuthorizationParameters &&
                    existingAuthorizations.map((authorization) => (
                        <>
                            <li className="flex justify-between">
                                <span className="font-medium text-muted-foreground">Authorization:</span>

                                <span>{authorization.title}</span>
                            </li>

                            {authorization.properties &&
                                authorization.properties
                                    .filter((property) => !!authorizationParameters![property.name!])
                                    .map((property) => (
                                        <li className="flex w-full justify-between" key={property.name}>
                                            <span className="text-muted-foreground">{property.name}:</span>

                                            <pre className="self-end text-xs">
                                                {authorizationParameters![property.name!]}
                                            </pre>
                                        </li>
                                    ))}
                        </>
                    ))}
            </ul>
        </div>
    );
};

export default ConnectionParameters;
