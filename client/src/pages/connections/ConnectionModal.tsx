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
import {ComponentDefinitionBasicModel} from '../../middleware/definition-registry';
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
        formState: {errors, touchedFields},
        handleSubmit,
        getValues,
        register,
        setValue,
        reset,
    } = useForm<{
        authorizationName: string;
        componentName: SelectOption;
        name: string;
        parameters: {[key: string]: object};
        tags: TagModel[];
    }>({
        defaultValues: {
            authorizationName: '',
            componentName: undefined,
            name: '',
            tags: [],
        },
    });

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

    let authorizationOptions: SelectOption[] = [];

    if (connectionDefinition && connectionDefinition.authorizations) {
        authorizationOptions = [
            ...(!connectionDefinition.authorizationRequired
                ? [{label: 'None', value: ''}]
                : []),
            ...connectionDefinition.authorizations.map((authorization) => ({
                label: authorization?.display?.label as string,
                value: authorization.name as string,
            })),
        ];
    }

    function authorizationsExists() {
        return (
            !connectionDefinitionIsLoading &&
            connectionDefinition &&
            connectionDefinition?.authorizations &&
            connectionDefinition.authorizations.length > 0
        );
    }

    return (
        <Modal
            confirmButtonLabel="Create Connection"
            description="Create your connection to connect to the chosen service"
            form={true}
            isOpen={isOpen}
            setIsOpen={setIsOpen}
            title="Create Connection"
            triggerLabel="Create Connection"
            onCloseClick={() => {
                closeForm();
            }}
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

            {authorizationsExists() && (
                <NativeSelect
                    error={
                        touchedFields.authorizationName &&
                        !!errors.authorizationName
                    }
                    label="Authorization"
                    options={authorizationOptions}
                    placeholder="Select..."
                    {...register('authorizationName')}
                    onValueChange={setAuthorizationName}
                />
            )}

            {authorizationsExists() && authorizationName && (
                <fieldset className="mb-3">
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-400">
                        Authorization
                    </label>

                    <div className="mt-1 pl-3">
                        <Properties
                            properties={
                                connectionDefinition?.authorizations?.filter(
                                    (authorization) =>
                                        authorization.name === authorizationName
                                )[0]?.properties
                            }
                            register={register}
                        />
                    </div>
                </fieldset>
            )}

            {!connectionDefinitionIsLoading &&
                connectionDefinition &&
                connectionDefinition?.properties &&
                connectionDefinition.properties.length > 0 && (
                    <fieldset className="mb-3">
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-400">
                            Properties
                        </label>

                        <div className="mt-1 pl-3">
                            <Properties
                                properties={connectionDefinition.properties}
                                register={register}
                            />
                        </div>
                    </fieldset>
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

            <div className="mt-4 flex justify-end">
                <Button
                    label="Create"
                    onClick={handleSubmit(createConnection)}
                    type="submit"
                />
            </div>
        </Modal>
    );
};

export default ConnectionModal;
