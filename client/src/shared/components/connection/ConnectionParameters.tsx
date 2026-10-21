import {ConnectionDefinition} from '@/shared/middleware/platform/configuration';
import {Fragment} from 'react';

const HIDDEN_CREDENTIALS_HINT = "Only this connection's owner can see or replace its credentials.";

interface ConnectionParametersProps {
    authorizationType?: string;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    authorizationParameters?: {[key: string]: any};
    baseUri?: string;
    connectionDefinition: ConnectionDefinition;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    connectionParameters?: {[key: string]: any};
    credentialsHidden?: boolean;
}

const ConnectionParameters = ({
    authorizationParameters,
    authorizationType,
    baseUri,
    connectionDefinition,
    connectionParameters,
    credentialsHidden = false,
}: ConnectionParametersProps) => {
    const {authorizations, properties: connectionProperties} = connectionDefinition;

    if (credentialsHidden) {
        return (
            <HiddenConnectionParameters
                authorizationType={authorizationType}
                baseUri={baseUri}
                connectionDefinition={connectionDefinition}
            />
        );
    }

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

    if (!hasConnectionParameters && !hasAuthorizationParameters && !baseUri) {
        return <></>;
    }

    return (
        <div className="w-full space-y-2 overflow-hidden">
            {baseUri && (
                <div className="mb-8 flex">
                    <span className="w-1/3 text-sm font-medium text-muted-foreground">Base URI:</span>

                    <span className="text-sm">{baseUri}</span>
                </div>
            )}

            {(hasConnectionParameters || hasAuthorizationParameters) && (
                <h2 className="text-sm heading-tertiary">
                    {hasConnectionParameters ? 'Connection' : 'Authorization'} Parameters
                </h2>
            )}

            <ul className="flex w-full flex-col space-y-1 text-sm">
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

                {hasAuthorizationParameters &&
                    existingAuthorizations.map((authorization) => (
                        <Fragment key={authorization.name}>
                            <li className="flex">
                                <span className="w-1/3 shrink-0 font-medium text-muted-foreground">Authorization:</span>

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
        </div>
    );
};

const HiddenConnectionParameters = ({
    authorizationType,
    baseUri,
    connectionDefinition,
}: Pick<ConnectionParametersProps, 'authorizationType' | 'baseUri' | 'connectionDefinition'>) => {
    const {authorizations, properties: connectionProperties} = connectionDefinition;

    const authorization = authorizations?.find((authorization) => authorization.type === authorizationType);

    const hiddenPropertyNames = [...(connectionProperties ?? []), ...(authorization?.properties ?? [])]
        .map((property) => property.name)
        .filter((propertyName): propertyName is string => !!propertyName);

    if (!hiddenPropertyNames.length && !baseUri) {
        return <></>;
    }

    return (
        <div className="w-full space-y-2 overflow-hidden">
            {baseUri && (
                <div className="mb-8 flex">
                    <span className="w-1/3 text-sm font-medium text-muted-foreground">Base URI:</span>

                    <span className="text-sm">{baseUri}</span>
                </div>
            )}

            {hiddenPropertyNames.length > 0 && (
                <>
                    <h2 className="text-sm heading-tertiary">Credentials</h2>

                    <ul className="flex w-full flex-col space-y-1 text-sm">
                        {authorization && (
                            <li className="flex">
                                <span className="w-1/3 shrink-0 font-medium text-muted-foreground">Authorization:</span>

                                <span>{authorization.title}</span>
                            </li>
                        )}

                        {hiddenPropertyNames.map((propertyName) => (
                            <li className="flex w-full" key={propertyName}>
                                <span className="w-1/3 shrink-0 text-muted-foreground">{propertyName}:</span>

                                <span className="min-w-0 flex-1 text-xs text-muted-foreground italic">Hidden</span>
                            </li>
                        ))}
                    </ul>

                    <p className="text-xs text-muted-foreground">{HIDDEN_CREDENTIALS_HINT}</p>
                </>
            )}
        </div>
    );
};

export default ConnectionParameters;
