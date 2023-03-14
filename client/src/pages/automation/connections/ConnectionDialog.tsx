import React, {useState} from 'react';

import Input from 'components/Input/Input';
import Dialog from 'components/Dialog/Dialog';
import CreatableSelect from 'components/CreatableSelect/CreatableSelect';
import {Controller, useForm} from 'react-hook-form';
import Button from 'components/Button/Button';
import {useQueryClient} from '@tanstack/react-query';
import {
    ComponentDefinitionKeys,
    useGetComponentDefinitionsQuery,
} from '../../../queries/componentDefinitions.queries';
import {
    AuthorizationModel,
    ComponentDefinitionBasicModel,
} from '../../../middleware/definition-registry';
import {ConnectionModel, TagModel} from '../../../middleware/connection';
import FilterableSelect, {
    ISelectOption,
} from '../../../components/FilterableSelect/FilterableSelect';
import {
    ConnectionKeys,
    useGetConnectionTagsQuery,
} from '../../../queries/connections.queries';
import {useCreateConnectionMutation} from '../../../mutations/connections.mutations';
import {OnChangeValue} from 'react-select';
import {useGetConnectionDefinitionQuery} from '../../../queries/connectionDefinitions.queries';
import NativeSelect from '../../../components/NativeSelect/NativeSelect';
import Properties from '../../../components/Properties/Properties';
import {timeout} from 'd3-timer';
import OAuth2Button from './components/OAuth2Button';
import {AuthTokenPayload, AuthorizationCodePayload} from './oauth2/useOAuth2';

interface FormProps {
    authorizationName: string;
    componentName: ISelectOption;
    name: string;
    parameters: {[key: string]: object};
    tags: TagModel[];
    /* eslint-disable @typescript-eslint/no-explicit-any */
    [key: string]: any;
}

const ConnectionDialog = () => {
    const [authorizationName, setAuthorizationName] = useState<string>();
    const [componentDefinition, setComponentDefinition] =
        useState<ComponentDefinitionBasicModel>();
    const [oAuth2Error, setOAuth2Error] = useState<string>();
    const [isOpen, setIsOpen] = useState(false);

    const {
        isLoading: componentDefinitionsIsLoading,
        error: componentDefinitionsError,
        data: componentDefinitions,
    } = useGetComponentDefinitionsQuery({connectionDefinitions: true});

    const {
        isLoading: tagsIsLoading,
        error: tagsError,
        data: tags,
    } = useGetConnectionTagsQuery();

    const {
        isLoading: connectionDefinitionIsLoading,
        error: connectionDefinitionError,
        data: connectionDefinition,
    } = useGetConnectionDefinitionQuery(
        componentDefinition
            ? {
                  name: componentDefinition.name,
              }
            : undefined
    );

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

    const {
        control,
        formState,
        formState: {errors, touchedFields},
        handleSubmit,
        getValues,
        register,
        setValue,
        reset,
    } = useForm<FormProps>({
        defaultValues: {
            authorizationName: '',
            componentName: undefined,
            name: '',
            tags: [],
        },
    });

    const authorizationsExists =
        !connectionDefinitionIsLoading &&
        connectionDefinition &&
        connectionDefinition?.authorizations &&
        connectionDefinition.authorizations.length > 0;

    const authorizationOptions: ISelectOption[] =
        connectionDefinition && connectionDefinition.authorizations
            ? [
                  ...(connectionDefinition.authorizationRequired === false
                      ? [{label: 'None', value: ''}]
                      : []),
                  ...connectionDefinition.authorizations.map(
                      (authorization) => ({
                          label: authorization?.display?.label as string,
                          value: authorization.name as string,
                      })
                  ),
              ]
            : [];

    const propertiesExists =
        !connectionDefinitionIsLoading &&
        connectionDefinition &&
        connectionDefinition?.properties &&
        connectionDefinition.properties.length > 0;

    const isOAuth2AuthorizationType =
        connectionDefinition &&
        connectionDefinition.authorizations &&
        authorizationName &&
        [
            'OAUTH2_AUTHORIZATION_CODE',
            'OAUTH2_AUTHORIZATION_CODE_PKCE',
            'OAUTH2_IMPLICIT_CODE',
        ].includes(getAuthorizationType());

    function closeDialog() {
        setIsOpen(false);

        timeout(() => {
            reset();

            setAuthorizationName(undefined);
            setComponentDefinition(undefined);
        }, 1000);
    }

    function createConnection(additionalParameters?: {[key: string]: object}) {
        const {authorizationName, componentName, name, parameters, tags} =
            getValues();

        const connectionModel = {
            authorizationName: authorizationName,
            componentName: componentName?.value,
            name: name,
            parameters: {
                ...parameters,
                ...additionalParameters,
            },
            tags: tags,
        } as ConnectionModel;

        createConnectionMutation.mutate(connectionModel);
    }

    function handleOnAuth2Success(
        payload: AuthTokenPayload & AuthorizationCodePayload
    ) {
        if (payload.access_token || payload.code) {
            /* eslint-disable @typescript-eslint/no-explicit-any */
            createConnection(payload as any);
        }
    }

    function handleOnOAuth2Error(error: string) {
        setOAuth2Error(error);
    }

    function getAuthorizationType(): string {
        let authorizationType = '';

        if (connectionDefinition && connectionDefinition.authorizations) {
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
            triggerLabel="Create Connection"
        >
            {componentDefinitionsError && !componentDefinitionsIsLoading && (
                <div className="my-4 rounded-md bg-red-50 p-4 text-sm text-red-700">
                    `An error has occurred: ${componentDefinitionsError.message}
                    `
                </div>
            )}
            {connectionDefinitionError && !connectionDefinitionIsLoading && (
                <div className="my-4 rounded-md bg-red-50 p-4 text-sm text-red-700">
                    `An error has occurred: ${connectionDefinitionError.message}
                    `
                </div>
            )}
            {tagsError && !tagsIsLoading && (
                <div className="my-4 rounded-md bg-red-50 p-4 text-sm text-red-700">
                    `An error has occurred: ${tagsError.message}`
                </div>
            )}
            {oAuth2Error && (
                <div className="my-4 rounded-md bg-red-50 p-4 text-sm text-red-700">
                    `An OAuth2 error has occurred: ${oAuth2Error}`
                </div>
            )}

            {!componentDefinitionsIsLoading && (
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
                                ) => ({
                                    label: `${componentDefinition.name
                                        .charAt(0)
                                        .toUpperCase()}${componentDefinition.name.slice(
                                        1
                                    )}`,
                                    value: componentDefinition.name,
                                    componentDefinition,
                                })
                            )}
                            onChange={(
                                value: OnChangeValue<ISelectOption, false>
                            ) => {
                                if (value) {
                                    setValue('componentName', value);

                                    setAuthorizationName(undefined);
                                    setComponentDefinition(
                                        value.componentDefinition
                                    );
                                }
                            }}
                        />
                    )}
                />
            )}

            <Input
                error={touchedFields.name && !!errors.name}
                label="Name"
                placeholder="My Connection"
                {...register('name', {required: true})}
            />

            {propertiesExists && (
                <Properties
                    formState={formState}
                    properties={connectionDefinition?.properties}
                    register={register}
                />
            )}

            {authorizationsExists && (
                <NativeSelect
                    error={
                        touchedFields.authorizationName &&
                        !!errors.authorizationName
                    }
                    label="Authorization"
                    options={authorizationOptions}
                    placeholder="Select..."
                    {...register('authorizationName', {
                        required:
                            connectionDefinition?.authorizationRequired ===
                                true ||
                            connectionDefinition?.authorizationRequired ===
                                undefined,
                        onChange: (event) =>
                            setAuthorizationName(event.target.value),
                    })}
                    value={authorizationName || authorizationOptions[0].value}
                />
            )}

            {authorizationsExists &&
                (authorizationName || authorizationOptions[0].value) && (
                    <Properties
                        formState={formState}
                        properties={
                            connectionDefinition?.authorizations?.filter(
                                (authorization) =>
                                    authorization.name ===
                                    (authorizationName ||
                                        authorizationOptions[0].value)
                            )[0]?.properties
                        }
                        register={register}
                    />
                )}

            {!tagsIsLoading && (
                <Controller
                    control={control}
                    name="tags"
                    render={({field}) => (
                        <CreatableSelect
                            field={field}
                            isMulti={true}
                            label="Tags"
                            options={tags!.map((tag: TagModel) => ({
                                label: `${tag.name
                                    .charAt(0)
                                    .toUpperCase()}${tag.name.slice(1)}`,
                                value: tag.name
                                    .toLowerCase()
                                    .replace(/\W/g, ''),
                                ...tag,
                            }))}
                            onCreateOption={(inputValue: string) => {
                                setValue('tags', [
                                    ...getValues().tags!,
                                    {
                                        label: inputValue,
                                        value: inputValue,
                                        name: inputValue,
                                    },
                                ] as never[]);
                            }}
                        />
                    )}
                />
            )}

            <div className="mt-8 flex justify-end space-x-1">
                <Button
                    displayType="lightBorder"
                    label="Cancel"
                    type="button"
                    onClick={closeDialog}
                />

                {isOAuth2AuthorizationType ? (
                    <OAuth2Button
                        onClick={(getAuth: () => void) =>
                            handleSubmit(() => getAuth())()
                        }
                        onSuccess={handleOnAuth2Success}
                        onError={handleOnOAuth2Error}
                    />
                ) : (
                    <Button
                        label="Save"
                        type="submit"
                        onClick={handleSubmit(createConnection)}
                    />
                )}
            </div>
        </Dialog>
    );
};

export default ConnectionDialog;
