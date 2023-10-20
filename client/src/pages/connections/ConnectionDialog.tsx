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
} from '../../queries/componentDefinitions';
import {
    AuthorizationModel,
    ComponentDefinitionBasicModel,
} from '../../middleware/definition-registry';
import {ConnectionModel, TagModel} from '../../middleware/connection';
import FilterableSelect, {
    SelectOption,
} from '../../components/FilterableSelect/FilterableSelect';
import {
    ConnectionKeys,
    useGetConnectionTagsQuery,
} from '../../queries/connections';
import {useCreateConnectionMutation} from '../../mutations/connections.mutations';
import {OnChangeValue} from 'react-select';
import {useGetConnectionDefinitionQuery} from '../../queries/connectionDefinitions';
import NativeSelect from '../../components/NativeSelect/NativeSelect';
import Properties from '../../components/Properties/Properties';
import {timeout} from 'd3-timer';
import OAuth2Button from './components/OAuth2Button';

interface FormProps {
    authorizationName: string;
    componentName: SelectOption;
    name: string;
    parameters: {[key: string]: object};
    tags: TagModel[];
}

const ConnectionDialog = () => {
    const [authorizationName, setAuthorizationName] = useState<string>();
    const [componentDefinition, setComponentDefinition] =
        useState<ComponentDefinitionBasicModel>();
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
                  componentName: componentDefinition.name,
                  componentVersion: componentDefinition.version,
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

    const authorizationOptions: SelectOption[] =
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

    function createConnection() {
        const {authorizationName, componentName, name, parameters, tags} =
            getValues();

        const connectionModel = {
            authorizationName: authorizationName,
            componentName: componentName?.value,
            connectionVersion: 1,
            name: name,
            parameters: parameters,
            tags: tags,
        } as ConnectionModel;

        createConnectionMutation.mutate(connectionModel);
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
            setIsOpen={(isOpen) => {
                if (isOpen) {
                    setIsOpen(isOpen);
                } else {
                    closeDialog();
                }
            }}
            title="Create Connection"
            triggerLabel="Create Connection"
        >
            {componentDefinitionsError &&
                !componentDefinitionsIsLoading &&
                `An error has occurred: ${componentDefinitionsError.message}`}
            {connectionDefinitionError &&
                !connectionDefinitionIsLoading &&
                `An error has occurred: ${connectionDefinitionError.message}`}
            {tagsError &&
                !tagsIsLoading &&
                `An error has occurred: ${tagsError.message}`}

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
                                value: OnChangeValue<SelectOption, false>
                            ) => {
                                if (value) {
                                    setValue('componentName', value);

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
                />
            )}

            {authorizationsExists && authorizationName && (
                <Properties
                    formState={formState}
                    properties={
                        connectionDefinition?.authorizations?.filter(
                            (authorization) =>
                                authorization.name === authorizationName
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

            <div className="mt-4 flex justify-end space-x-1">
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
