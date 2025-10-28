import {ComboBoxItemType} from '@/components/ComboBox/ComboBox';
import CreatableSelect from '@/components/CreatableSelect/CreatableSelect';
import {Alert, AlertDescription, AlertTitle} from '@/components/ui/alert';
import {Button} from '@/components/ui/button';
import {Checkbox} from '@/components/ui/checkbox';
import {
    Dialog,
    DialogCloseButton,
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
import {useToast} from '@/hooks/use-toast';
import Properties from '@/pages/platform/workflow-editor/components/properties/Properties';
import {ConnectionI, WorkflowMockProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import EnvironmentBadge from '@/shared/components/EnvironmentBadge';
import ConnectionParameters from '@/shared/components/connection/ConnectionParameters';
import {TokenPayloadI} from '@/shared/components/connection/oauth2/useOAuth2';
import {
    Authorization,
    AuthorizationType,
    ComponentDefinition,
    ComponentDefinitionBasic,
    Tag,
} from '@/shared/middleware/platform/configuration';
import {ComponentDefinitionKeys} from '@/shared/queries/platform/componentDefinitions.queries';
import {
    useGetConnectionDefinitionQuery,
    useGetConnectionDefinitionsQuery,
} from '@/shared/queries/platform/connectionDefinitions.queries';
import {
    useGetOAuth2AuthorizationParametersQuery,
    useGetOAuth2PropertiesQuery,
} from '@/shared/queries/platform/oauth2.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {QueryKey, UseMutationResult, UseQueryResult, useQueryClient} from '@tanstack/react-query';
import {useCopyToClipboard} from '@uidotdev/usehooks';
import {CircleQuestionMarkIcon, ClipboardIcon, RocketIcon} from 'lucide-react';
import {ReactNode, useCallback, useEffect, useMemo, useState} from 'react';
import {useForm} from 'react-hook-form';

import ComponentSelectionInput from './ComponentSelectionInput';
import OAuth2Button from './OAuth2Button';

export interface ConnectionDialogFormProps {
    authorizationType: string;
    componentName: string;
    environmentId: number;
    id?: number;
    name: string;
    parameters: {[key: string]: object};
    tags: Array<Tag | {label: string; value: string}>;
}

interface ConnectionDialogProps {
    componentDefinition?: ComponentDefinition;
    componentDefinitions: ComponentDefinitionBasic[];
    connection?: ConnectionI | undefined;
    connectionTagsQueryKey: QueryKey;
    connectionsQueryKey: QueryKey;
    onClose?: () => void;
    onConnectionCreate?: (connectionId: number) => void;
    triggerNode?: ReactNode;
    useCreateConnectionMutation?: (mutationProps: {
        onSuccess?: (result: number, variables: ConnectionI) => void;
        onError?: (error: Error, variables: ConnectionI) => void;
    }) => UseMutationResult<number, Error, ConnectionI, unknown>;
    useGetConnectionTagsQuery: () => UseQueryResult<Tag[], Error>;
    useUpdateConnectionMutation?: (mutationProps: {
        onSuccess?: (result: void, variables: ConnectionI) => void;
        onError?: (error: Error, variables: ConnectionI) => void;
    }) => UseMutationResult<void, Error, ConnectionI, unknown>;
}

const ConnectionDialog = ({
    componentDefinition,
    componentDefinitions,
    connection,
    connectionTagsQueryKey,
    connectionsQueryKey,
    onClose,
    onConnectionCreate,
    triggerNode,
    useCreateConnectionMutation,
    useGetConnectionTagsQuery,
    useUpdateConnectionMutation,
}: ConnectionDialogProps) => {
    const [authorizationType, setAuthorizationType] = useState<string>();
    const [isOpen, setIsOpen] = useState(!triggerNode);
    const [oAuth2Error, setOAuth2Error] = useState<string>();
    const [wizardStep, setWizardStep] = useState<'configuration_step' | 'oauth_step'>('configuration_step');
    const [selectedComponentDefinition, setSelectedComponentDefinition] = useState<
        ComponentDefinitionBasic | undefined
    >(componentDefinition);
    const [usePredefinedOAuthApp, setUsePredefinedOAuthApp] = useState(true);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {toast} = useToast();

    /* eslint-disable @typescript-eslint/no-unused-vars */
    const [_, copyToClipboard] = useCopyToClipboard();

    const form = useForm<ConnectionDialogFormProps>({
        defaultValues: {
            authorizationType: undefined,
            componentName: componentDefinition?.name,
            environmentId: connection?.environmentId || currentEnvironmentId,
            id: connection?.id,
            name: connection?.name || componentDefinition?.title || '',
            tags:
                connection?.tags?.map((tag) => ({
                    ...tag,
                    label: tag.name,
                })) || [],
        },
    });

    const {control, formState, getValues, handleSubmit, reset: formReset, setValue} = form;

    const {
        data: connectionDefinition,
        error: connectionDefinitionError,
        isLoading: connectionDefinitionLoading,
    } = useGetConnectionDefinitionQuery(
        {
            componentName: (selectedComponentDefinition?.name as string) || (connection?.componentName as string),
        },
        !!selectedComponentDefinition?.name || !!connection?.componentName
    );

    const {data: connectionDefinitions} = useGetConnectionDefinitionsQuery(
        {
            componentName: selectedComponentDefinition?.name as string,
        },
        !!selectedComponentDefinition?.name
    );

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
        onSuccess: (connectionId) => {
            queryClient.invalidateQueries({
                queryKey: ComponentDefinitionKeys.componentDefinitions,
            });

            queryClient.invalidateQueries({
                queryKey: connectionsQueryKey,
            });

            queryClient.invalidateQueries({
                queryKey: connectionTagsQueryKey,
            });

            if (!connection?.id) {
                toast({
                    description: `${getValues().name} connection was successfully created`,
                    title: 'Connection created',
                });

                if (connectionId && onConnectionCreate) {
                    onConnectionCreate(connectionId);
                }
            }

            closeDialog();
        },
    });

    const authorizationsExists = connectionDefinition && !!connectionDefinition?.authorizations?.length;

    const authorizationOptions = useMemo(
        () =>
            connectionDefinition && connectionDefinition.authorizations
                ? [
                      ...(connectionDefinition.authorizationRequired === false
                          ? [{label: 'None', value: undefined}]
                          : []),
                      ...connectionDefinition.authorizations.map((authorization) => ({
                          label: authorization?.title as string,
                          value: authorization.type as string,
                      })),
                  ]
                : [],
        [connectionDefinition]
    );

    const authorizations = connectionDefinition?.authorizations?.filter(
        (authorization) => authorization.type === (authorizationType || authorizationOptions[0].value)
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
                setAuthorizationType(undefined);
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
        let curAuthorizationType = '';

        if (connectionDefinition?.authorizations) {
            const authorization: Authorization = connectionDefinition.authorizations.filter(
                (authorization) => authorization.type === authorizationType
            )[0];

            if (authorization) {
                curAuthorizationType = authorization.type!;
            }
        }

        return curAuthorizationType;
    }

    function getNewConnection(additionalParameters?: object) {
        const {componentName, name, parameters, tags} = getValues();

        return {
            authorizationType,
            componentName,
            connectionVersion: 1,
            environmentId: currentEnvironmentId,
            name,
            parameters: {
                ...parameters,
                ...additionalParameters,
            },
            tags: tags,
        } as ConnectionI;
    }

    function getNewOAuth2AuthorizationParameters() {
        const {componentName, parameters} = getValues();

        return {
            authorizationType: authorizationType as AuthorizationType,
            componentName,
            connectionVersion: 1,
            parameters: {
                ...parameters,
            },
        };
    }

    function getErrors() {
        const errors: string[] = [];

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
                id: connection?.id,
                name,
                tags,
                version: connection.version,
            } as ConnectionI);
        } else {
            return connectionMutation.mutateAsync(getNewConnection(additionalParameters));
        }
    }

    const handleComponentDefinitionChange = useCallback(
        (componentDefinition?: ComponentDefinitionBasic) => {
            if (componentDefinition) {
                setValue('componentName', componentDefinition.name);
                setAuthorizationType(undefined);
                setSelectedComponentDefinition(componentDefinition);

                if (oAuth2Properties?.predefinedApps) {
                    setUsePredefinedOAuthApp(
                        oAuth2Properties?.predefinedApps?.includes(componentDefinition?.name || '')
                    );
                }

                if (!getValues('name') && componentDefinition.title) {
                    setValue('name', componentDefinition.title);
                }

                setWizardStep('configuration_step');
            }
        },
        [getValues, oAuth2Properties?.predefinedApps, setValue]
    );

    useEffect(() => {
        setAuthorizationType(
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

            <DialogContent className="gap-0 p-0" onInteractOutside={(event) => event.preventDefault()}>
                <Form {...form}>
                    <DialogHeader className="flex flex-row items-center justify-between space-y-0 px-6 pb-4 pt-6">
                        <div className="flex flex-col space-y-1">
                            <DialogTitle>{`${connection?.id ? 'Edit' : 'Create'} Connection`}</DialogTitle>

                            {!connection?.id && (
                                <DialogDescription>
                                    Create your connection to connect to the chosen service
                                </DialogDescription>
                            )}
                        </div>

                        <DialogCloseButton />
                    </DialogHeader>

                    {errors?.length > 0 && <Errors errors={errors} />}

                    <div className="flex max-h-dialog-height flex-col space-y-4 overflow-y-auto px-6">
                        {connection?.id && (
                            <FormField
                                control={control}
                                name="id"
                                render={({field}) => (
                                    <FormControl>
                                        <div className="flex">
                                            <div className="relative flex grow items-stretch focus-within:z-10">
                                                <Input
                                                    {...field}
                                                    className="rounded-r-none bg-gray-50 text-gray-700"
                                                    readOnly
                                                    value={connection?.id}
                                                />
                                            </div>

                                            <Button
                                                className="-ml-px rounded-l-none rounded-r-md border border-gray-200 bg-gray-50 shadow-sm hover:bg-gray-100"
                                                onClick={() => copyToClipboard(connection?.id?.toString() ?? '')}
                                                size="icon"
                                                type="button"
                                                variant="ghost"
                                            >
                                                <ClipboardIcon aria-hidden="true" className="size-4 text-gray-400" />
                                            </Button>
                                        </div>
                                    </FormControl>
                                )}
                            />
                        )}

                        {(wizardStep === 'configuration_step' || oAuth2AuthorizationParametersLoading) && (
                            <>
                                {!connection?.id && (
                                    <FormField
                                        control={control}
                                        name="componentName"
                                        render={({field}) => {
                                            let items: Array<ComboBoxItemType> | undefined;

                                            if (!componentDefinition && componentDefinitions) {
                                                items = componentDefinitions.map((componentDefinitionItem) => ({
                                                    ...componentDefinitionItem,
                                                    componentDefinition: componentDefinitionItem,
                                                    icon: componentDefinitionItem.icon,
                                                    label: componentDefinitionItem.title,
                                                    value: componentDefinitionItem.name,
                                                }));
                                            } else if (connectionDefinitions?.length) {
                                                items = connectionDefinitions.map((connectionDefinitionItem) => ({
                                                    ...connectionDefinitionItem,
                                                    componentDefinition: selectedComponentDefinition,
                                                    icon: selectedComponentDefinition?.icon,
                                                    label: connectionDefinitionItem.componentTitle,
                                                    value: connectionDefinitionItem.componentName,
                                                }));
                                            }

                                            return (
                                                <ComponentSelectionInput
                                                    componentDefinition={componentDefinition}
                                                    field={field}
                                                    handleComponentDefinitionChange={handleComponentDefinitionChange}
                                                    items={items}
                                                    selectedComponentDefinition={selectedComponentDefinition}
                                                />
                                            );
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

                                <FormField
                                    control={control}
                                    name="environmentId"
                                    render={() => (
                                        <FormItem className="space-x-2">
                                            <FormLabel>Environment</FormLabel>

                                            <FormControl>
                                                <EnvironmentBadge environmentId={currentEnvironmentId} />
                                            </FormControl>

                                            <FormMessage />
                                        </FormItem>
                                    )}
                                />

                                {!connection?.id && showConnectionProperties && !!connectionDefinition.properties && (
                                    <WorkflowMockProvider>
                                        <Properties
                                            control={control}
                                            formState={formState}
                                            properties={connectionDefinition?.properties}
                                        />
                                    </WorkflowMockProvider>
                                )}

                                {!connection?.id && showAuthorizations && (
                                    <FormField
                                        control={control}
                                        name="authorizationType"
                                        render={({field}) => (
                                            <FormItem>
                                                <FormLabel>Authorization</FormLabel>

                                                <Select
                                                    onValueChange={(value) => {
                                                        setAuthorizationType(value);
                                                        setUsePredefinedOAuthApp(false);
                                                        setValue('authorizationType', value);
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

                                {!connection?.id &&
                                    showAuthorizationProperties &&
                                    !!authorizations?.length &&
                                    authorizations[0]?.properties && (
                                        <WorkflowMockProvider>
                                            <Properties
                                                control={control}
                                                formState={formState}
                                                properties={authorizations[0]?.properties}
                                            />
                                        </WorkflowMockProvider>
                                    )}

                                {showOAuth2AppPredefined && (
                                    <div>
                                        <a
                                            className="text-sm text-blue-600"
                                            href="#"
                                            onClick={() => setUsePredefinedOAuthApp(!usePredefinedOAuthApp)}
                                        >
                                            <span>
                                                I want to use {usePredefinedOAuthApp ? 'predefined' : 'my own'} app
                                                credentials
                                            </span>
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
                                                        isMulti
                                                        menuPlacement="top"
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
                                                        options={remainingTags?.map((tag: Tag) => ({
                                                            label: tag.name,
                                                            value: tag.name.toLowerCase().replace(/\W/g, ''),
                                                            ...tag,
                                                        }))}
                                                    />
                                                </FormControl>

                                                <FormMessage />
                                            </FormItem>
                                        )}
                                    />
                                )}
                            </>
                        )}

                        {!oAuth2AuthorizationParametersLoading && wizardStep === 'oauth_step' && (
                            <>
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
                            </>
                        )}
                    </div>

                    {connection?.id && connectionDefinition && (
                        <div className="px-6 pt-4">
                            <ConnectionParameters
                                authorizationParameters={connection.authorizationParameters}
                                connectionDefinition={connectionDefinition}
                                connectionParameters={connection.connectionParameters}
                            />
                        </div>
                    )}

                    <DialogFooter className="px-6 pb-6 pt-4">
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
                                            extraQueryParameters={oAuth2AuthorizationParameters?.extraQueryParameters}
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
                    <CircleQuestionMarkIcon className="size-4 text-muted-foreground" />
                </TooltipTrigger>

                <TooltipContent>OAuth permission scopes used for this connection.</TooltipContent>
            </Tooltip>
        </div>

        <div className="space-y-1">
            {scopes.map((scope) => (
                <div className="flex items-center space-x-1" key={scope}>
                    <Checkbox checked disabled key={scope} />

                    <Label htmlFor={scope}>{scope}</Label>
                </div>
            ))}
        </div>
    </div>
);

export default ConnectionDialog;
