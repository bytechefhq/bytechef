import {ClipboardIcon} from '@heroicons/react/24/outline';
import {QuestionMarkCircledIcon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import Alert from 'components/Alert/Alert';
import Button from 'components/Button/Button';
import Checkbox from 'components/Checkbox/Checkbox';
import CreatableSelect from 'components/CreatableSelect/CreatableSelect';
import Dialog from 'components/Dialog/Dialog';
import FilterableSelect, {
    ISelectOption,
} from 'components/FilterableSelect/FilterableSelect';
import Input from 'components/Input/Input';
import Label from 'components/Label/Label';
import NativeSelect from 'components/NativeSelect/NativeSelect';
import Properties, {PropertyFormProps} from 'components/Properties/Properties';
import Tooltip from 'components/Tooltip/Tooltip';
import useCopyToClipboard from 'hooks/useCopyToClipboard';
import {ConnectionModel, TagModel} from 'middleware/automation/connection';
import {
    AuthorizationModel,
    ComponentDefinitionBasicModel,
    ComponentDefinitionModel,
    GetOAuth2AuthorizationParametersRequestModel,
} from 'middleware/core/definition-registry';
import {
    useCreateConnectionMutation,
    useUpdateConnectionMutation,
} from 'mutations/connections.mutations';
import {
    ComponentDefinitionKeys,
    useGetComponentDefinitionsQuery,
} from 'queries/componentDefinitions.queries';
import {useGetConnectionDefinitionQuery} from 'queries/connectionDefinitions.queries';
import {
    ConnectionKeys,
    useGetConnectionTagsQuery,
    useGetOAuth2AuthorizationParametersQuery,
} from 'queries/connections.queries';
import {useGetOAuth2PropertiesQuery} from 'queries/oauth2Properties.queries';
import React, {useEffect, useMemo, useState} from 'react';
import {Controller, useForm} from 'react-hook-form';
import {OnChangeValue} from 'react-select';

import {AuthTokenPayload} from '../oauth2/useOAuth2';
import OAuth2Button from './OAuth2Button';

interface ConnectionDialogProps {
    component?: ComponentDefinitionModel;
    connection?: ConnectionModel | undefined;
    showTrigger?: boolean;
    visible?: boolean;
    onClose?: () => void;
}

const ConnectionDialog = ({
    component,
    connection,
    showTrigger = true,
    visible = false,
    onClose,
}: ConnectionDialogProps) => {
    const [authorizationName, setAuthorizationName] = useState<string>();
    const [isOpen, setIsOpen] = useState(visible);
    const [oAuth2Error, setOAuth2Error] = useState<string>();
    const [wizardStep, setWizardStep] = useState<
        'configuration_step' | 'oauth_step'
    >('configuration_step');

    const [componentDefinition, setComponentDefinition] =
        useState<ComponentDefinitionBasicModel>();

    const [usePredefinedOAuthApp, setUsePredefinedOAuthApp] =
        useState<boolean>(true);

    const {
        control,
        formState,
        handleSubmit,
        getValues,
        register,
        setValue,
        reset: formReset,
    } = useForm<PropertyFormProps>({
        defaultValues: {
            authorizationName: '',
            componentName: undefined,
            name: connection?.name || '',
            tags:
                connection?.tags?.map((tag) => ({
                    ...tag,
                    label: tag.name,
                })) || [],
        },
    });

    const {
        isLoading: componentDefinitionsLoading,
        error: componentDefinitionsError,
        data: componentDefinitions,
    } = useGetComponentDefinitionsQuery({connectionDefinitions: true});

    const {
        isLoading: connectionDefinitionLoading,
        error: connectionDefinitionError,
        data: connectionDefinition,
    } = useGetConnectionDefinitionQuery(
        componentDefinition
            ? {
                  componentName: componentDefinition.name,
                  componentVersion: 1,
              }
            : undefined
    );

    const {
        isLoading: oAuth2AuthorizationParametersLoading,
        error: oAuth2AuthorizationParametersError,
        data: oAuth2AuthorizationParameters,
    } = useGetOAuth2AuthorizationParametersQuery(
        getNewOAuth2AuthorizationParameters(),
        wizardStep === 'oauth_step'
    );

    const {
        isLoading: tagsLoading,
        error: tagsError,
        data: tags,
    } = useGetConnectionTagsQuery();

    const {
        isLoading: oAuth2PropertiesLoading,
        error: oAuth2PropertiesError,
        data: oAuth2Properties,
    } = useGetOAuth2PropertiesQuery();

    const queryClient = useQueryClient();

    const createConnectionMutation = useCreateConnectionMutation({
        onSuccess: () => {
            queryClient.invalidateQueries(
                ComponentDefinitionKeys.componentDefinitions({
                    connectionInstances: true,
                })
            );
            queryClient.invalidateQueries(ConnectionKeys.connections);
            queryClient.invalidateQueries(ConnectionKeys.connectionTags);

            closeDialog();
        },
    });

    const updateConnectionMutation = useUpdateConnectionMutation({
        onSuccess: () => {
            queryClient.invalidateQueries(
                ComponentDefinitionKeys.componentDefinitions({
                    connectionInstances: true,
                })
            );
            queryClient.invalidateQueries(ConnectionKeys.connections);
            queryClient.invalidateQueries(ConnectionKeys.connectionTags);

            closeDialog();
        },
    });

    const authorizationsExists =
        connectionDefinition && !!connectionDefinition?.authorizations?.length;

    const authorizationOptions: ISelectOption[] = useMemo(
        () =>
            connectionDefinition && connectionDefinition.authorizations
                ? [
                      ...(connectionDefinition.authorizationRequired === false
                          ? [{label: 'None', value: ''}]
                          : []),
                      ...connectionDefinition.authorizations.map(
                          (authorization) => ({
                              label: authorization?.title as string,
                              value: authorization.name as string,
                          })
                      ),
                  ]
                : [],
        [connectionDefinition]
    );

    const authorizations = connectionDefinition?.authorizations?.filter(
        (authorization) =>
            authorization.name ===
            (authorizationName || authorizationOptions[0].value)
    );

    const errors = getErrors();

    const isOAuth2AuthorizationType = [
        'OAUTH2_AUTHORIZATION_CODE',
        'OAUTH2_AUTHORIZATION_CODE_PKCE',
    ].includes(getAuthorizationType());

    const isOAuth2ImplicitCodeType =
        'OAUTH2_IMPLICIT_CODE' === getAuthorizationType();

    const scopes = oAuth2AuthorizationParameters?.scopes;

    const showAuthorizations =
        authorizationsExists && authorizationOptions.length > 1;

    const showOAuth2AppPredefined =
        (isOAuth2AuthorizationType || isOAuth2ImplicitCodeType) &&
        !oAuth2PropertiesLoading &&
        oAuth2Properties?.predefinedApps?.includes(
            componentDefinition?.name || ''
        );

    const showAuthorizationProperties =
        !showOAuth2AppPredefined ||
        !(isOAuth2AuthorizationType || isOAuth2ImplicitCodeType) ||
        !usePredefinedOAuthApp;

    const showConnectionProperties =
        !connectionDefinitionLoading &&
        !!connectionDefinition?.properties?.length;

    const showOAuth2Step =
        (isOAuth2AuthorizationType || isOAuth2ImplicitCodeType) &&
        !connection?.id;

    const showRedirectUriInput =
        (isOAuth2AuthorizationType || isOAuth2ImplicitCodeType) &&
        !usePredefinedOAuthApp &&
        oAuth2Properties?.redirectUri;

    useEffect(() => {
        setAuthorizationName(
            authorizationOptions && authorizationOptions.length > 0
                ? authorizationOptions[0].value
                : undefined
        );
    }, [authorizationsExists, authorizationOptions, componentDefinition]);

    const tagNames = connection?.tags?.map((tag) => tag.name);

    const remainingTags = tags?.filter((tag) => !tagNames?.includes(tag.name));

    function closeDialog() {
        setIsOpen(false);

        setTimeout(() => {
            formReset();

            setAuthorizationName(undefined);
            setComponentDefinition(undefined);
            setOAuth2Error(undefined);
            setWizardStep('configuration_step');

            createConnectionMutation.reset();
            updateConnectionMutation.reset();

            if (onClose) {
                onClose();
            }
        }, 300);
    }

    async function handleOnCodeSuccess(code: string) {
        if (code) {
            await saveConnection({code: code});
        }
    }

    function getAuthorizationType(): string {
        let authorizationType = '';

        if (connectionDefinition?.authorizations) {
            const authorization: AuthorizationModel =
                connectionDefinition.authorizations.filter(
                    (authorization) => authorization.name === authorizationName
                )[0];

            if (authorization) {
                authorizationType = authorization.type!;
            }
        }

        return authorizationType;
    }

    function getNewConnection(additionalParameters?: object): ConnectionModel {
        const {componentName, name, parameters, tags} = getValues();

        return {
            authorizationName: authorizationName,
            componentName: componentName?.value,
            connectionVersion: 1,
            name: name,
            parameters: {
                ...parameters,
                ...additionalParameters,
            },
            tags: tags,
        } as ConnectionModel;
    }

    function getNewOAuth2AuthorizationParameters(): GetOAuth2AuthorizationParametersRequestModel {
        const {componentName, parameters} = getValues();

        return {
            authorizationName: authorizationName,
            componentName: componentName?.value,
            connectionVersion: 1,
            parameters: {
                ...parameters,
            },
        } as GetOAuth2AuthorizationParametersRequestModel;
    }

    function getErrors(): string[] {
        const errors: string[] = [];

        if (componentDefinitionsError && !componentDefinitionsLoading) {
            errors.push(componentDefinitionsError.message);
        }

        if (connectionDefinitionError && !connectionDefinitionLoading) {
            errors.push(connectionDefinitionError.message);
        }

        if (
            createConnectionMutation.error &&
            !createConnectionMutation.isLoading
        ) {
            errors.push(createConnectionMutation.error?.message);
        }

        if (tagsError && !tagsLoading) {
            errors.push(tagsError.message);
        }

        if (
            oAuth2AuthorizationParametersError &&
            !oAuth2AuthorizationParametersLoading
        ) {
            errors.push(oAuth2AuthorizationParametersError.message);
        }

        if (oAuth2Error) {
            errors.push(oAuth2Error);
        }

        if (oAuth2PropertiesError && !oAuth2PropertiesLoading) {
            errors.push(oAuth2PropertiesError.message);
        }

        return errors;
    }

    function saveConnection(
        additionalParameters?: object
    ): Promise<ConnectionModel> | undefined {
        if (connection?.id) {
            const {name, tags} = getValues();

            updateConnectionMutation.mutate({
                id: connection?.id,
                name: name,
                tags: tags,
                version: connection?.version,
            } as ConnectionModel);
        } else {
            return createConnectionMutation.mutateAsync(
                getNewConnection(additionalParameters)
            );
        }
    }

    return (
        <Dialog
            description="Create your connection to connect to the chosen service"
            isOpen={isOpen}
            onOpenChange={(isOpen) => {
                if (isOpen) {
                    setIsOpen(isOpen);
                } else {
                    closeDialog();
                }
            }}
            title="Create Connection"
            triggerLabel={
                showTrigger
                    ? `${connection?.id ? 'Edit' : 'Create'} Connection`
                    : undefined
            }
        >
            {!componentDefinitionsLoading && (
                <div>
                    {errors && <Errors errors={errors} />}

                    {(wizardStep === 'configuration_step' ||
                        oAuth2AuthorizationParametersLoading) && (
                        <>
                            {!connection?.id && !component && (
                                <Controller
                                    control={control}
                                    name="componentName"
                                    rules={{required: true}}
                                    render={({field, fieldState: {error}}) => (
                                        <FilterableSelect
                                            error={!!error}
                                            field={field}
                                            label="Component"
                                            options={componentDefinitions!.map(
                                                (
                                                    componentDefinition: ComponentDefinitionBasicModel
                                                ) => {
                                                    const {name} =
                                                        componentDefinition;

                                                    const capitalizedName = `${name
                                                        .charAt(0)
                                                        .toUpperCase()}${name.slice(
                                                        1
                                                    )}`;

                                                    return {
                                                        label: capitalizedName,
                                                        value: name,
                                                        componentDefinition,
                                                    };
                                                }
                                            )}
                                            onChange={(
                                                value: OnChangeValue<
                                                    ISelectOption,
                                                    false
                                                >
                                            ) => {
                                                if (value) {
                                                    setValue(
                                                        'componentName',
                                                        value
                                                    );

                                                    setAuthorizationName(
                                                        undefined
                                                    );

                                                    setComponentDefinition(
                                                        value.componentDefinition
                                                    );

                                                    setUsePredefinedOAuthApp(
                                                        true
                                                    );

                                                    setWizardStep(
                                                        'configuration_step'
                                                    );
                                                }
                                            }}
                                        />
                                    )}
                                />
                            )}

                            {component && (
                                <Input
                                    label="Component"
                                    defaultValue={component.title}
                                    disabled
                                    name="defaultComponentName"
                                />
                            )}

                            <Input
                                error={
                                    formState.touchedFields.name &&
                                    !!formState.errors.name
                                }
                                label="Name"
                                placeholder="My Connection"
                                {...register('name', {required: true})}
                            />

                            {showConnectionProperties &&
                                !!connectionDefinition.properties && (
                                    <Properties
                                        formState={formState}
                                        properties={
                                            connectionDefinition?.properties
                                        }
                                        register={register}
                                    />
                                )}

                            {showAuthorizations && (
                                <NativeSelect
                                    error={
                                        formState.touchedFields
                                            .authorizationName &&
                                        !!formState.errors.authorizationName
                                    }
                                    label="Authorization"
                                    options={authorizationOptions}
                                    placeholder="Select..."
                                    value={authorizationName}
                                    {...register('authorizationName', {
                                        required:
                                            connectionDefinition?.authorizationRequired,
                                        onChange: (event) =>
                                            setAuthorizationName(
                                                event.target.value
                                            ),
                                    })}
                                />
                            )}

                            {showRedirectUriInput &&
                                oAuth2Properties?.redirectUri && (
                                    <RedirectUriInput
                                        redirectUri={
                                            oAuth2Properties.redirectUri
                                        }
                                    />
                                )}

                            {showAuthorizationProperties &&
                                !!authorizations?.length &&
                                authorizations[0]?.properties && (
                                    <Properties
                                        formState={formState}
                                        properties={
                                            authorizations[0]?.properties
                                        }
                                        register={register}
                                    />
                                )}

                            {showOAuth2AppPredefined && (
                                <div className="mb-3">
                                    <a
                                        href="#"
                                        className="text-sm text-blue-600"
                                        onClick={() =>
                                            setUsePredefinedOAuthApp(
                                                !usePredefinedOAuthApp
                                            )
                                        }
                                    >
                                        {usePredefinedOAuthApp && (
                                            <span>
                                                I want to use my own app
                                                credentials
                                            </span>
                                        )}

                                        {!usePredefinedOAuthApp && (
                                            <span>
                                                I want to use predefined app
                                                credentials
                                            </span>
                                        )}
                                    </a>
                                </div>
                            )}

                            {!tagsLoading && (
                                <Controller
                                    control={control}
                                    name="tags"
                                    render={({field}) => (
                                        <CreatableSelect
                                            field={field}
                                            isMulti={true}
                                            label="Tags"
                                            options={remainingTags!.map(
                                                (tag: TagModel) => {
                                                    return {
                                                        label: `${tag.name
                                                            .charAt(0)
                                                            .toUpperCase()}${tag.name.slice(
                                                            1
                                                        )}`,
                                                        value: tag.name
                                                            .toLowerCase()
                                                            .replace(/\W/g, ''),
                                                        ...tag,
                                                    };
                                                }
                                            )}
                                            onCreateOption={(
                                                inputValue: string
                                            ) => {
                                                setValue('tags', [
                                                    ...getValues().tags!,
                                                    {
                                                        label: inputValue,
                                                        value: inputValue,
                                                        name: inputValue,
                                                    },
                                                ]);
                                            }}
                                        />
                                    )}
                                />
                            )}
                        </>
                    )}

                    {!oAuth2AuthorizationParametersLoading &&
                        wizardStep === 'oauth_step' && (
                            <div>
                                <Alert
                                    text={
                                        <p>
                                            Excellent! You can connect and
                                            create the
                                            <span className="font-semibold">
                                                {componentDefinition?.title}
                                            </span>
                                            connection under name
                                            <span className="font-semibold">{`'${
                                                getValues()?.name
                                            }'`}</span>
                                            .
                                        </p>
                                    }
                                />

                                {scopes && <Scopes scopes={scopes} />}
                            </div>
                        )}

                    <div className="mt-8 flex justify-end space-x-1">
                        {wizardStep === 'oauth_step' && (
                            <Button
                                displayType="lightBorder"
                                label="Previous"
                                type="button"
                                onClick={() => {
                                    createConnectionMutation.reset();

                                    setOAuth2Error(undefined);

                                    setWizardStep('configuration_step');
                                }}
                            />
                        )}

                        {wizardStep === 'configuration_step' && (
                            <Button
                                displayType="lightBorder"
                                label="Cancel"
                                type="button"
                                onClick={closeDialog}
                            />
                        )}

                        {showOAuth2Step && (
                            <>
                                {wizardStep === 'configuration_step' && (
                                    <Button
                                        label="Next"
                                        type="submit"
                                        onClick={handleSubmit(() => {
                                            setWizardStep('oauth_step');
                                        })}
                                    />
                                )}

                                {wizardStep === 'oauth_step' &&
                                    oAuth2AuthorizationParameters?.authorizationUrl &&
                                    oAuth2AuthorizationParameters?.clientId && (
                                        <OAuth2Button
                                            authorizationUrl={
                                                oAuth2AuthorizationParameters.authorizationUrl
                                            }
                                            clientId={
                                                oAuth2AuthorizationParameters.clientId
                                            }
                                            redirectUri={
                                                oAuth2Properties?.redirectUri ??
                                                ''
                                            }
                                            responseType={
                                                isOAuth2AuthorizationType
                                                    ? 'code'
                                                    : 'token'
                                            }
                                            scope={oAuth2AuthorizationParameters?.scopes?.join(
                                                '_'
                                            )}
                                            onClick={(getAuth: () => void) => {
                                                getAuth();
                                            }}
                                            onCodeSuccess={handleOnCodeSuccess}
                                            onTokenSuccess={(
                                                payload: AuthTokenPayload
                                            ) => {
                                                if (payload.access_token) {
                                                    return saveConnection(
                                                        payload
                                                    );
                                                }
                                            }}
                                            onError={(error: string) =>
                                                setOAuth2Error(error)
                                            }
                                        />
                                    )}
                            </>
                        )}

                        {!showOAuth2Step && (
                            <Button
                                label="Save"
                                type="submit"
                                onClick={handleSubmit(saveConnection)}
                            />
                        )}
                    </div>
                </div>
            )}
        </Dialog>
    );
};

const Errors = ({errors}: {errors: string[]}) => (
    <ul>
        {errors.map((error, index) => (
            <li
                key={`error_${index}`}
                className="my-4 rounded-md bg-red-50 p-4 text-sm text-red-700"
            >
                An error has occurred: {error}
            </li>
        ))}
    </ul>
);

const RedirectUriInput = ({redirectUri}: {redirectUri: string}) => {
    /* eslint-disable @typescript-eslint/no-unused-vars */
    const [_, copyToClipboard] = useCopyToClipboard();

    return (
        <Input
            label="Redirect URI"
            name="redirectUri"
            readOnly
            value={redirectUri}
            trailing={
                <Button
                    className="-ml-px rounded-l-none rounded-r-md border-gray-300 px-3 py-2 hover:bg-gray-50"
                    displayType="icon"
                    icon={
                        <ClipboardIcon
                            className="h-5 w-5 text-gray-400"
                            aria-hidden="true"
                        />
                    }
                    onClick={() => copyToClipboard(redirectUri ?? '')}
                />
            }
        />
    );
};

const Scopes = ({scopes}: {scopes: string[]}) => (
    <div className="py-2">
        <div className="flex">
            <span className="mb-2 mr-1 text-sm font-semibold">Scopes</span>

            <Tooltip text={'OAuth permission scopes used for this connection.'}>
                <QuestionMarkCircledIcon />
            </Tooltip>
        </div>

        <div className="space-y-1">
            {scopes.map((scope) => (
                <div className="flex items-center" key={scope}>
                    <Checkbox id={scope} disabled />

                    <Label htmlFor={scope} value={scope} />
                </div>
            ))}
        </div>
    </div>
);

export default ConnectionDialog;
