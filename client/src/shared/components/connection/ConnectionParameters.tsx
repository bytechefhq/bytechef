import {ConnectionDefinition} from '@/shared/middleware/platform/configuration';
import {Fragment} from 'react';

interface ConnectionParametersProps {
    authorizationType?: string;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    authorizationParameters?: {[key: string]: any};
    baseUri?: string;
    connectionDefinition: ConnectionDefinition;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    connectionParameters?: {[key: string]: any};
}

const ConnectionParameters = ({
    authorizationParameters,
    authorizationType,
    baseUri,
    connectionDefinition,
    connectionParameters,
}: ConnectionParametersProps) => {
    const {authorizations, properties: connectionProperties} = connectionDefinition;

    const existingAuthorizations =
        authorizations?.filter(
            (authorization) =>
                authorization.type === authorizationType &&
                authorization.properties &&
                authorization.properties.some((property) => !!authorizationParameters?.[property.name!])
        ) ?? [];

    const hasConnectionParameters =
        !!connectionParameters && !!connectionProperties?.some((property) => !!connectionParameters[property.name!]);

    const hasAuthorizationParameters = !!authorizationParameters && existingAuthorizations.length > 0;

    const showBaseUri = !!baseUri && !(hasConnectionParameters && connectionParameters?.baseUri === baseUri);

    if (!hasConnectionParameters && !hasAuthorizationParameters && !showBaseUri) {
        return <></>;
    }

    const showConnectionSection = showBaseUri || hasConnectionParameters;

    return (
        <div className="w-full space-y-6 overflow-hidden">
            {showConnectionSection && (
                <section className="space-y-2">
                    <h2 className="text-sm heading-tertiary">Connection Parameters</h2>

                    <ul className="flex w-full flex-col space-y-1 text-sm">
                        {showBaseUri && (
                            <li className="flex w-full">
                                <span className="w-1/3 shrink-0 font-medium text-muted-foreground">Base URI:</span>

                                <span className="min-w-0 flex-1 truncate" title={baseUri}>
                                    {baseUri}
                                </span>
                            </li>
                        )}

                        {hasConnectionParameters &&
                            connectionProperties!
                                .filter((property) => !!connectionParameters![property.name!])
                                .map((property) => (
                                    <li className="flex w-full" key={property.name}>
                                        <span className="w-1/3 shrink-0 text-muted-foreground">{property.name}:</span>

                                        <pre
                                            className="min-w-0 flex-1 self-end truncate text-xs"
                                            title={String(connectionParameters![property.name!])}
                                        >
                                            {connectionParameters![property.name!]}
                                        </pre>
                                    </li>
                                ))}
                    </ul>
                </section>
            )}

            {hasAuthorizationParameters && (
                <section className="space-y-2">
                    <h2 className="text-sm heading-tertiary">Authorization Parameters</h2>

                    <ul className="flex w-full flex-col space-y-1 text-sm">
                        {existingAuthorizations.map((authorization) => (
                            <Fragment key={authorization.name}>
                                <li className="flex">
                                    <span className="w-1/3 shrink-0 font-medium text-muted-foreground">
                                        Authorization:
                                    </span>

                                    <span>{authorization.title}</span>
                                </li>

                                {authorization.properties &&
                                    authorization.properties
                                        .filter((property) => !!authorizationParameters![property.name!])
                                        .map((property) => (
                                            <li className="flex w-full" key={property.name}>
                                                <span className="w-1/3 shrink-0 text-muted-foreground">
                                                    {property.name}:
                                                </span>

                                                <pre
                                                    className="min-w-0 flex-1 self-end truncate text-xs"
                                                    title={String(authorizationParameters![property.name!])}
                                                >
                                                    {authorizationParameters![property.name!]}
                                                </pre>
                                            </li>
                                        ))}
                            </Fragment>
                        ))}
                    </ul>
                </section>
            )}
        </div>
    );
};

export default ConnectionParameters;
