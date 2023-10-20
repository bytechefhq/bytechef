import React, {useState} from 'react';

import Input from 'components/Input/Input';
import Modal from 'components/Modal/Modal';
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
import {useConnectionCreateMutation} from '../../mutations/connections.mutations';
import {OnChangeValue} from 'react-select';
import {useGetConnectionDefinitionQuery} from '../../queries/connectionDefinitions';
import NativeSelect from '../../components/NativeSelect/NativeSelect';
import Properties from '../../components/Properties/Properties';
import {timeout} from 'd3-timer';
import useOAuth2 from './oauth2/useOAuth2';
import {UseFormHandleSubmit} from 'react-hook-form/dist/types/form';

interface FormProps {
    authorizationName: string;
    componentName: SelectOption;
    name: string;
    parameters: {[key: string]: object};
    tags: TagModel[];
}

interface OAuth2ButtonProps {
    handleSubmit: UseFormHandleSubmit<FormProps>;
}

const ConnectionModal = () => {
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

    const mutation = useConnectionCreateMutation({
        onSuccess: () => {
            queryClient.invalidateQueries(
                ComponentDefinitionKeys.componentDefinitions({
                    connectionInstances: true,
                })
            );
            queryClient.invalidateQueries(ConnectionKeys.connections);
            queryClient.invalidateQueries(ConnectionKeys.connectionTags);

            closeForm();
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

    function closeForm() {
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

        mutation.mutate(connectionModel);
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
        <Modal
            confirmButtonLabel="Create Connection"
            description="Create your connection to connect to the chosen service"
            form
            isOpen={isOpen}
            setIsOpen={setIsOpen}
            title="Create Connection"
            triggerLabel="Create Connection"
            onCloseClick={closeForm}
            onConfirmButtonClick={handleSubmit(createConnection)}
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
                            name="componentName"
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
                            name="tags"
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
                    onClick={closeForm}
                />

                {isOAuth2AuthorizationType ? (
                    <OAuth2Button handleSubmit={handleSubmit} />
                ) : (
                    <Button
                        label="Create"
                        onClick={handleSubmit(createConnection)}
                        type="submit"
                    />
                )}
            </div>
        </Modal>
    );
};

const LoadingIcon = (): JSX.Element => (
    <svg
        className="-ml-1 mr-1 h-4 w-4 animate-spin text-white"
        xmlns="http://www.w3.org/2000/svg"
        fill="none"
        viewBox="0 0 24 24"
    >
        <circle
            className="opacity-25"
            cx="12"
            cy="12"
            r="10"
            stroke="currentColor"
            strokeWidth="4"
        ></circle>
        <path
            className="opacity-75"
            fill="currentColor"
            d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
        ></path>
    </svg>
);

const OAuth2Button = ({handleSubmit}: OAuth2ButtonProps) => {
    const {data, loading, error, getAuth} = useOAuth2({
        authorizeUrl: 'https://login.mailchimp.com/oauth2/authorize',
        clientId: '344111396868',
        redirectUri: `${document.location.origin}/callback`,
        scope: '',
        responseType: 'code',
        exchangeCodeForTokenServerURL: 'http://localhost:5173/token',
        exchangeCodeForTokenMethod: 'POST',
        onSuccess: (payload) => console.log('Success', payload),
        onError: (error_) => console.log('Error', error_),
    });

    const isLoggedIn = Boolean(data?.access_token); // or whatever...

    if (error) {
        return <div>Error</div>;
    }

    if (loading) {
        return (
            <Button
                icon={<LoadingIcon />}
                iconPosition="left"
                label="Creating..."
                type="button"
            />
        );
    }

    if (isLoggedIn) {
        return <pre>{JSON.stringify(data)}</pre>;
    }

    return (
        <Button
            label="Create"
            onClick={handleSubmit(() => getAuth())}
            type="submit"
        />
    );
};

export default ConnectionModal;
