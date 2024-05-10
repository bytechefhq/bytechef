import ComboBox, {ComboBoxItemType} from '@/components/ComboBox';
import {Alert, AlertDescription, AlertTitle} from '@/components/ui/alert';
import {Button} from '@/components/ui/button';
import {Checkbox} from '@/components/ui/checkbox';
import {
    Dialog,
    DialogClose,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from '@/components/ui/dialog';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {TokenPayloadI} from '@/pages/platform/connection/components/oauth2/useOAuth2';
import Properties from '@/pages/platform/workflow-editor/components/Properties/Properties';
import {useGetOAuth2AuthorizationParametersQuery, useGetOAuth2PropertiesQuery} from '@/queries/platform/oauth2.queries';
import {Cross2Icon, QuestionMarkCircledIcon, RocketIcon} from '@radix-ui/react-icons';
import {QueryKey, UseMutationResult, UseQueryResult, useQueryClient} from '@tanstack/react-query';
import CreatableSelect from 'components/CreatableSelect/CreatableSelect';
import useCopyToClipboard from 'hooks/useCopyToClipboard';
import {ClipboardIcon} from 'lucide-react';
import {
    AuthorizationModel,
    ComponentDefinitionBasicModel,
    ComponentDefinitionModel,
} from 'middleware/platform/configuration';
import {ConnectionEnvironmentModel, ConnectionModel, TagModel} from 'middleware/platform/connection';
import {ComponentDefinitionKeys, useGetComponentDefinitionsQuery} from 'queries/platform/componentDefinitions.queries';
import {
    useGetConnectionDefinitionQuery,
    useGetConnectionDefinitionsQuery,
} from 'queries/platform/connectionDefinitions.queries';
import {ReactNode, useEffect, useMemo, useState} from 'react';
import {useForm} from 'react-hook-form';

import OAuth2Button from './OAuth2Button';

interface ConnectionDialogProps {
    componentDefinition?: ComponentDefinitionModel;
    connection?: ConnectionModel | undefined;
    connectionTagsQueryKey: QueryKey;
    connectionsQueryKey: QueryKey;
    onClose?: () => void;
    useCreateConnectionMutation?: (mutationProps: {
        onSuccess?: (result: ConnectionModel, variables: ConnectionModel) => void;
        onError?: (error: Error, variables: ConnectionModel) => void;
    }) => UseMutationResult<ConnectionModel, Error, ConnectionModel, unknown>;
    useGetConnectionTagsQuery: () => UseQueryResult<TagModel[], Error>;
    useUpdateConnectionMutation?: (mutationProps: {
        onSuccess?: (result: ConnectionModel, variables: ConnectionModel) => void;
        onError?: (error: Error, variables: ConnectionModel) => void;
    }) => UseMutationResult<ConnectionModel, Error, ConnectionModel, unknown>;
    triggerNode?: ReactNode;
}

export interface ConnectionDialogFormProps {
    authorizationName: string;
    environment: ConnectionEnvironmentModel;
    componentName: string;
    name: string;
    parameters: {[key: string]: object};
    tags: Array<TagModel | {label: string; value: string}>;
}

const ConnectionDialog = ({
    componentDefinition,
    connection,
    connectionTagsQueryKey,
    connectionsQueryKey,
    onClose,
    triggerNode,
    useCreateConnectionMutation,
    useGetConnectionTagsQuery,
    useUpdateConnectionMutation,
}: ConnectionDialogProps) => {
    const [authorizationName, setAuthorizationName] = useState<string>();
    const [isOpen, setIsOpen] = useState(!triggerNode);
    const [oAuth2Error, setOAuth2Error] = useState<string>();
    const [wizardStep, setWizardStep] = useState<'configuration_step' | 'oauth_step'>('configuration_step');
    const [selectedComponentDefinition, setSelectedComponentDefinition] = useState<
        ComponentDefinitionBasicModel | undefined
    >(componentDefinition);
    const [usePredefinedOAuthApp, setUsePredefinedOAuthApp] = useState(true);

    const form = useForm<ConnectionDialogFormProps>({
        defaultValues: {
            authorizationName: '',
            componentName: componentDefinition?.name,
            environment: connection?.environment || ConnectionEnvironmentModel.Development,
            name: connection?.name || '',
            tags:
                connection?.tags?.map((tag) => ({
                    ...tag,
                    label: tag.name,
                })) || [],
        },
    });

    const {control, formState, getValues, handleSubmit, reset: formReset, setValue} = form;

    const {
        data: componentDefinitions,
        error: componentDefinitionsError,
        isLoading: componentDefinitionsLoading,
    } = useGetComponentDefinitionsQuery({connectionDefinitions: true});

    const {
        data: connectionDefinition,
        error: connectionDefinitionError,
        isLoading: connectionDefinitionLoading,
    } = useGetConnectionDefinitionQuery({
        componentName: selectedComponentDefinition?.name as string,
        componentVersion: 1,
    });

    const {data: connectionDefinitions} = useGetConnectionDefinitionsQuery({
        componentName: selectedComponentDefinition?.name as string,
        componentVersion: 1,
    });

    const {
        data: oAuth2AuthorizationParameters,
        error: oAuth2AuthorizationParametersError,
        isLoading: oAuth2AuthorizationParametersLoading,
    } = useGetOAuth2AuthorizationParametersQuery(getNewOAuth2AuthorizationParameters(), wizardStep === 'oauth_step');

    const {data: tags, error: tagsError, isLoading: tagsLoading} = useGetConnectionTagsQuery();

    const {
        data: oAuth2Properties,
        error: oAuth2PropertiesError,
        isLoading: oAuth2PropertiesLoading,
    } = useGetOAuth2PropertiesQuery();

    const queryClient = useQueryClient();

    const connectionMutation = (useUpdateConnectionMutation || useCreateConnectionMutation)!({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ComponentDefinitionKeys.componentDefinitions,
            });
            queryClient.invalidateQueries({
                queryKey: connectionsQueryKey,
            });
            queryClient.invalidateQueries({
                queryKey: connectionTagsQueryKey,
            });

            closeDialog();
        },
    });

    const authorizationsExists = connectionDefinition && !!connectionDefinition?.authorizations?.length;

    const authorizationOptions = useMemo(
        () =>
            connectionDefinition && connectionDefinition.authorizations
                ? [
                      ...(connectionDefinition.authorizationRequired === false ? [{label: 'None', value: 'none'}] : []),
                      ...connectionDefinition.authorizations.map((authorization) => ({
                          label: authorization?.title as string,
                          value: authorization.name as string,
                      })),
                  ]
                : [],
        [connectionDefinition]
    );

    const authorizations = connectionDefinition?.authorizations?.filter(
        (authorization) => authorization.name === (authorizationName || authorizationOptions[0].value)
    );

    const errors = getErrors();

    const isOAuth2AuthorizationType = ['OAUTH2_AUTHORIZATION_CODE', 'OAUTH2_AUTHORIZATION_CODE_PKCE'].includes(
        getAuthorizationType()
    );

    const isOAuth2ImplicitCodeType = 'OAUTH2_IMPLICIT_CODE' === getAuthorizationType();

    const scopes = oAuth2AuthorizationParameters?.scopes;

    const showAuthorizations = authorizationsExists && authorizationOptions.length > 1;

    const showOAuth2AppPredefined =
        (isOAuth2AuthorizationType || isOAuth2ImplicitCodeType) &&
        !oAuth2PropertiesLoading &&
        oAuth2Properties?.predefinedApps?.includes(selectedComponentDefinition?.name || '');

    const showAuthorizationProperties =
        !showOAuth2AppPredefined || !(isOAuth2AuthorizationType || isOAuth2ImplicitCodeType) || !usePredefinedOAuthApp;

    const showConnectionProperties = !connectionDefinitionLoading && !!connectionDefinition?.properties?.length;

    const showOAuth2Step = (isOAuth2AuthorizationType || isOAuth2ImplicitCodeType) && !connection?.id;

    const showRedirectUriInput =
        (isOAuth2AuthorizationType || isOAuth2ImplicitCodeType) &&
        !usePredefinedOAuthApp &&
        oAuth2Properties?.redirectUri;

    const tagNames = connection?.tags?.map((tag) => tag.name);

    const remainingTags = tags?.filter((tag) => !tagNames?.includes(tag.name));

    function closeDialog() {
        setIsOpen(false);

        setTimeout(() => {
            formReset();

            setOAuth2Error(undefined);
            setWizardStep('configuration_step');

            if (!componentDefinition) {
                setAuthorizationName(undefined);
                setSelectedComponentDefinition(undefined);
            }

            connectionMutation.reset();

            if (onClose) {
                onClose();
            }
        }, 300);
    }

    async function handleCodeSuccess(payload: {code: string; [key: string]: string}) {
        if (payload.code) {
            await saveConnection(payload);
        }
    }

    async function handleTokenSuccess(payload: TokenPayloadI) {
        if (payload.access_token) {
            await saveConnection(payload);
        }
    }

    function getAuthorizationType() {
        let authorizationType = '';

        if (connectionDefinition?.authorizations) {
            const authorization: AuthorizationModel = connectionDefinition.authorizations.filter(
                (authorization) => authorization.name === authorizationName
            )[0];

            if (authorization) {
                authorizationType = authorization.type!;
            }
        }

        return authorizationType;
    }

    function getNewConnection(additionalParameters?: object) {
        const {componentName, environment, name, parameters, tags} = getValues();

        return {
            authorizationName: authorizationName,
            componentName,
            connectionVersion: 1,
            environment,
            name,
            parameters: {
                ...parameters,
                ...additionalParameters,
            },
            tags: tags,
        } as ConnectionModel;
    }

    function getNewOAuth2AuthorizationParameters() {
        const {componentName, parameters} = getValues();

        return {
            authorizationName: authorizationName,
            componentName,
            connectionVersion: 1,
            parameters: {
                ...parameters,
            },
        };
    }

    function getErrors() {
        const errors: string[] = [];

        if (componentDefinitionsError && !componentDefinitionsLoading) {
            errors.push(componentDefinitionsError.message);
        }

        if (connectionDefinitionError && !connectionDefinitionLoading) {
            errors.push(connectionDefinitionError.message);
        }

        if (connectionMutation.error && !connectionMutation.isPending) {
            errors.push(connectionMutation.error?.message);
        }

        if (tagsError && !tagsLoading) {
            errors.push(tagsError.message);
        }

        if (oAuth2AuthorizationParametersError && !oAuth2AuthorizationParametersLoading) {
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

    function saveConnection(additionalParameters?: object) {
        if (connection?.id) {
            const {name, tags} = getValues();

            connectionMutation.mutate({
                componentName: connection.componentName,
                id: connection?.id,
                name,
                tags,
                version: connection?.version,
            } as ConnectionModel);
        } else {
            return connectionMutation.mutateAsync(getNewConnection(additionalParameters));
        }
    }

    const handleComponentDefinitionChange = (componentDefinition?: ComponentDefinitionBasicModel) => {
        if (componentDefinition) {
            setValue('componentName', componentDefinition.name);
            setAuthorizationName(undefined);
            setSelectedComponentDefinition(componentDefinition);

            if (oAuth2Properties?.predefinedApps) {
                setUsePredefinedOAuthApp(oAuth2Properties?.predefinedApps?.includes(componentDefinition?.name || ''));
            }

            setWizardStep('configuration_step');
        }
    };

    useEffect(() => {
        setAuthorizationName(
            authorizationOptions && authorizationOptions.length > 0 ? authorizationOptions[0].value : undefined
        );
    }, [authorizationsExists, authorizationOptions, selectedComponentDefinition]);

    return (
        <Dialog
            onOpenChange={(isOpen) => {
                if (isOpen) {
                    setIsOpen(isOpen);
                } else {
                    closeDialog();
                }
            }}
            open={isOpen}
        >
            {triggerNode && <DialogTrigger asChild>{triggerNode}</DialogTrigger>}

            {!componentDefinitionsLoading && (
                <DialogContent onInteractOutside={(event) => event.preventDefault()}>
                    <Form {...form}>
                        <DialogHeader>
                            <div className="flex items-center justify-between">
                                <DialogTitle>{`${connection?.id ? 'Edit' : 'Create'} Connection`}</DialogTitle>

                                <DialogClose asChild>
                                    <Cross2Icon className="size-4 cursor-pointer opacity-70" />
                                </DialogClose>
                            </div>

                            {!connection?.id && (
                                <DialogDescription>
                                    Create your connection to connect to the chosen service
                                </DialogDescription>
                            )}
                        </DialogHeader>

                        {errors?.length > 0 && <Errors errors={errors} />}

                        {(wizardStep === 'configuration_step' || oAuth2AuthorizationParametersLoading) && (
                            <div className="grid max-h-[600px] gap-4 overflow-y-auto">
                                {!connection?.id && (
                                    <FormField
                                        control={control}
                                        name="componentName"
                                        render={({field}) => {
                                            if (!componentDefinition && componentDefinitions) {
                                                return (
                                                    <FormItem>
                                                        <FormLabel>Component</FormLabel>

                                                        <FormControl>
                                                            <ComboBox
                                                                items={componentDefinitions.map(
                                                                    (componentDefinition) => ({
                                                                        componentDefinition,
                                                                        icon: componentDefinition.icon,
                                                                        label: componentDefinition.title!,
                                                                        value: componentDefinition.name,
                                                                    })
                                                                )}
                                                                name="component"
                                                                onBlur={field.onBlur}
                                                                onChange={(item) =>
                                                                    handleComponentDefinitionChange(
                                                                        item?.componentDefinition as ComponentDefinitionBasicModel
                                                                    )
                                                                }
                                                                value={field.value}
                                                            />
                                                        </FormControl>

                                                        <FormMessage />
                                                    </FormItem>
                                                );
                                            } else if (connectionDefinitions && connectionDefinitions?.length > 1) {
                                                return (
                                                    <FormItem>
                                                        <FormLabel>Component</FormLabel>

                                                        <FormControl>
                                                            <ComboBox
                                                                items={connectionDefinitions.map(
                                                                    (connectionDefinition) =>
                                                                        ({
                                                                            componentDefinition:
                                                                                selectedComponentDefinition,
                                                                            icon: selectedComponentDefinition?.icon
                                                                                ? selectedComponentDefinition?.icon
                                                                                : undefined,
                                                                            label: connectionDefinition.componentTitle
                                                                                ? connectionDefinition.componentTitle
                                                                                : undefined,
                                                                            value: connectionDefinition.componentName,
                                                                        }) as ComboBoxItemType
                                                                )}
                                                                onBlur={field.onBlur}
                                                                onChange={(item) =>
                                                                    handleComponentDefinitionChange(
                                                                        item?.componentDefinition as ComponentDefinitionBasicModel
                                                                    )
                                                                }
                                                                value={field.value}
                                                            />
                                                        </FormControl>

                                                        <FormMessage />
                                                    </FormItem>
                                                );
                                            } else {
                                                return (
                                                    <FormItem>
                                                        <FormLabel>Component</FormLabel>

                                                        <FormControl>
                                                            <Input disabled value={componentDefinition?.title} />
                                                        </FormControl>

                                                        <FormMessage />
                                                    </FormItem>
                                                );
                                            }
                                        }}
                                        rules={{required: true}}
                                    />
                                )}

                                <FormField
                                    control={form.control}
                                    name="name"
                                    render={({field}) => (
                                        <FormItem>
                                            <FormLabel>Name</FormLabel>

                                            <FormControl>
                                                <Input placeholder="My Connection" {...field} />
                                            </FormControl>

                                            <FormMessage />
                                        </FormItem>
                                    )}
                                    rules={{required: true}}
                                />

                                {!connection?.id && (
                                    <FormField
                                        control={control}
                                        name="environment"
                                        render={({field}) => (
                                            <FormItem>
                                                <FormLabel>Environment</FormLabel>

                                                <FormControl>
                                                    <Select
                                                        defaultValue={field.value}
                                                        onValueChange={(value) => field.onChange(value)}
                                                    >
                                                        <SelectTrigger className="w-full">
                                                            <SelectValue placeholder="Select environment" />
                                                        </SelectTrigger>

                                                        <SelectContent>
                                                            <SelectItem value="DEVELOPMENT">Development</SelectItem>

                                                            <SelectItem value="TEST">Test</SelectItem>

                                                            <SelectItem value="PRODUCTION">Production</SelectItem>
                                                        </SelectContent>
                                                    </Select>
                                                </FormControl>

                                                <FormMessage />
                                            </FormItem>
                                        )}
                                        rules={{required: true}}
                                        shouldUnregister={false}
                                    />
                                )}

                                {showConnectionProperties && !!connectionDefinition.properties && (
                                    <Properties
                                        control={control}
                                        formState={formState}
                                        properties={connectionDefinition?.properties}
                                    />
                                )}

                                {showAuthorizations && (
                                    <FormField
                                        control={control}
                                        name="authorizationName"
                                        render={({field}) => (
                                            <FormItem>
                                                <FormLabel>Authorization</FormLabel>

                                                <Select
                                                    onValueChange={(value) => {
                                                        setAuthorizationName(value);
                                                        setUsePredefinedOAuthApp(false);
                                                        setValue('authorizationName', value);
                                                    }}
                                                    {...field}
                                                >
                                                    <SelectTrigger className="mt-1">
                                                        <FormControl>
                                                            <SelectValue placeholder="Select..." />
                                                        </FormControl>
                                                    </SelectTrigger>

                                                    <SelectContent>
                                                        {authorizationOptions.map((authorizationOption) => (
                                                            <SelectItem
                                                                key={authorizationOption.value!}
                                                                value={authorizationOption.value!}
                                                            >
                                                                {authorizationOption.label!}
                                                            </SelectItem>
                                                        ))}
                                                    </SelectContent>
                                                </Select>

                                                <FormMessage />
                                            </FormItem>
                                        )}
                                    />
                                )}

                                {showRedirectUriInput && oAuth2Properties?.redirectUri && (
                                    <div>
                                        <Label>Redirect URI</Label>

                                        <RedirectUriInput redirectUri={oAuth2Properties.redirectUri} />
                                    </div>
                                )}

                                {showAuthorizationProperties &&
                                    !!authorizations?.length &&
                                    authorizations[0]?.properties && (
                                        <Properties
                                            control={control}
                                            formState={formState}
                                            properties={authorizations[0]?.properties}
                                        />
                                    )}

                                {showOAuth2AppPredefined && (
                                    <div>
                                        <a
                                            className="text-sm text-blue-600"
                                            href="#"
                                            onClick={() => setUsePredefinedOAuthApp(!usePredefinedOAuthApp)}
                                        >
                                            {usePredefinedOAuthApp && <span>I want to use my own app credentials</span>}

                                            {!usePredefinedOAuthApp && (
                                                <span>I want to use predefined app credentials</span>
                                            )}
                                        </a>
                                    </div>
                                )}

                                {!tagsLoading && (
                                    <FormField
                                        control={control}
                                        name="tags"
                                        render={({field}) => (
                                            <FormItem>
                                                <FormLabel>Tags</FormLabel>

                                                <FormControl>
                                                    <CreatableSelect
                                                        field={field}
                                                        fieldsetClassName="mb-0"
                                                        isMulti={true}
                                                        onCreateOption={(inputValue: string) => {
                                                            setValue('tags', [
                                                                ...getValues().tags!,
                                                                {
                                                                    label: inputValue,
                                                                    name: inputValue,
                                                                    value: inputValue,
                                                                },
                                                            ]);
                                                        }}
                                                        options={remainingTags!.map((tag: TagModel) => {
                                                            return {
                                                                label: tag.name,
                                                                value: tag.name.toLowerCase().replace(/\W/g, ''),
                                                                ...tag,
                                                            };
                                                        })}
                                                    />
                                                </FormControl>

                                                <FormMessage />
                                            </FormItem>
                                        )}
                                    />
                                )}
                            </div>
                        )}

                        {!oAuth2AuthorizationParametersLoading && wizardStep === 'oauth_step' && (
                            <div>
                                <Alert className="border-blue-50 bg-blue-50 text-blue-700">
                                    <RocketIcon className="size-4" />

                                    <AlertTitle>Heads up!</AlertTitle>

                                    <AlertDescription>
                                        Excellent! You can connect and create the
                                        <span className="mx-0.5 font-semibold">
                                            {selectedComponentDefinition?.title}
                                        </span>
                                        connection under name
                                        <span className="mx-0.5 font-semibold">{`'${getValues()?.name}'`}</span>.
                                    </AlertDescription>
                                </Alert>

                                {scopes && scopes.length > 0 && <Scopes scopes={scopes} />}
                            </div>
                        )}

                        <DialogFooter>
                            {wizardStep === 'oauth_step' && (
                                <Button
                                    onClick={() => {
                                        connectionMutation.reset();

                                        setOAuth2Error(undefined);

                                        setWizardStep('configuration_step');
                                    }}
                                    type="button"
                                    variant="outline"
                                >
                                    Previous
                                </Button>
                            )}

                            {wizardStep === 'configuration_step' && (
                                <Button onClick={closeDialog} type="button" variant="outline">
                                    Cancel
                                </Button>
                            )}

                            {showOAuth2Step && (
                                <>
                                    {wizardStep === 'configuration_step' && (
                                        <Button
                                            onClick={handleSubmit(() => {
                                                setWizardStep('oauth_step');
                                            })}
                                            type="submit"
                                        >
                                            Next
                                        </Button>
                                    )}

                                    {wizardStep === 'oauth_step' &&
                                        oAuth2AuthorizationParameters?.authorizationUrl &&
                                        oAuth2AuthorizationParameters?.clientId && (
                                            <OAuth2Button
                                                authorizationUrl={oAuth2AuthorizationParameters.authorizationUrl}
                                                clientId={oAuth2AuthorizationParameters.clientId}
                                                onClick={(getAuth: () => void) => {
                                                    getAuth();
                                                }}
                                                onCodeSuccess={handleCodeSuccess}
                                                onError={(error: string) => setOAuth2Error(error)}
                                                onTokenSuccess={handleTokenSuccess}
                                                redirectUri={oAuth2Properties?.redirectUri ?? ''}
                                                responseType={isOAuth2AuthorizationType ? 'code' : 'token'}
                                                scope={oAuth2AuthorizationParameters?.scopes?.join(' ')}
                                            />
                                        )}
                                </>
                            )}

                            {!showOAuth2Step && (
                                <Button onClick={handleSubmit(() => saveConnection())} type="submit">
                                    Save
                                </Button>
                            )}
                        </DialogFooter>
                    </Form>
                </DialogContent>
            )}
        </Dialog>
    );
};

const Errors = ({errors}: {errors: string[]}) => (
    <ul>
        {errors.map((error, index) => (
            <li className="my-4 rounded-md bg-red-50 p-4 text-sm text-red-700" key={`error_${index}`}>
                An error has occurred: {error}
            </li>
        ))}
    </ul>
);

const RedirectUriInput = ({redirectUri}: {redirectUri?: string}) => {
    /* eslint-disable @typescript-eslint/no-unused-vars */
    const [_, copyToClipboard] = useCopyToClipboard();

    return (
        <div className="flex">
            <div className="relative flex grow items-stretch focus-within:z-10">
                <Input className="rounded-r-none" name="redirectUri" readOnly value={redirectUri} />
            </div>

            <Button
                className="-ml-px rounded-l-none rounded-r-md border border-gray-200 bg-gray-50 shadow-sm hover:bg-gray-100"
                onClick={() => copyToClipboard(redirectUri ?? '')}
                size="icon"
                variant="ghost"
            >
                <ClipboardIcon aria-hidden="true" className="size-4 text-gray-400" />
            </Button>
        </div>
    );
};

const Scopes = ({scopes}: {scopes: string[]}) => (
    <div className="space-y-2 py-2">
        <div className="flex items-center space-x-1">
            <span className="text-sm font-semibold">Scopes</span>

            <Tooltip>
                <TooltipTrigger>
                    <QuestionMarkCircledIcon />
                </TooltipTrigger>

                <TooltipContent>OAuth permission scopes used for this connection.</TooltipContent>
            </Tooltip>
        </div>

        <div className="space-y-1">
            {scopes.map((scope) => (
                <div className="flex items-center space-x-1" key={scope}>
                    <Checkbox checked disabled id={scope} />

                    <Label htmlFor={scope}>{scope}</Label>
                </div>
            ))}
        </div>
    </div>
);

export default ConnectionDialog;
