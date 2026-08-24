import Button from '@/components/Button/Button';
import {ComboBoxItemType} from '@/components/ComboBox/ComboBox';
import CreatableSelect from '@/components/CreatableSelect/CreatableSelect';
import {Input} from '@/components/Input/Input';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import Switch from '@/components/Switch/Switch';
import {Alert, AlertDescription, AlertTitle} from '@/components/ui/alert';
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
import {Label} from '@/components/ui/label';
import {PlatformType, usePlatformTypeStore} from '@/pages/home/stores/usePlatformTypeStore';
import Properties from '@/pages/platform/workflow-editor/components/properties/Properties';
import {ConnectionI, WorkflowMockProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {useCommandIntent} from '@/shared/command-bar/useCommandIntent';
import EnvironmentBadge from '@/shared/components/EnvironmentBadge';
import ConnectionParameters from '@/shared/components/connection/ConnectionParameters';
import {CodePayloadI, TokenPayloadI} from '@/shared/components/connection/oauth2/useOAuth2';
import ResourceVisibilityBadge from '@/shared/components/visibility/ResourceVisibilityBadge';
import ResourceVisibilityPicker from '@/shared/components/visibility/ResourceVisibilityPicker';
import {useIsVisibilityEditionEnabled} from '@/shared/hooks/useVisibilityFeatureEnabled';
import {ConnectionCredentialStoreType, useConnectionCredentialStoresQuery} from '@/shared/middleware/graphql';
import {
    Authorization,
    AuthorizationType,
    ComponentDefinition,
    ComponentDefinitionBasic,
    Tag,
} from '@/shared/middleware/platform/configuration';
import {useRegisterExistingConnectionMutation} from '@/shared/mutations/automation/connections.mutations';
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
import {ClipboardIcon, ExternalLinkIcon, KeyRoundIcon, RocketIcon} from 'lucide-react';
import {ReactNode, useCallback, useEffect, useMemo, useState} from 'react';
import {useForm} from 'react-hook-form';
import {Link} from 'react-router-dom';
import {toast} from 'sonner';
import {twMerge} from 'tailwind-merge';

import ComponentSelectionInput from './ComponentSelectionInput';
import OAuth2Button from './OAuth2Button';
import Scopes from './Scopes';
import {connectionCredentialStoreLabels} from './connectionCredentialStoreLabels';

export interface ConnectionDialogFormProps {
    authorizationType: string;
    componentName: string;
    credentialRef?: string;
    credentialStoreType?: ConnectionCredentialStoreType;
    environmentId: number;
    id?: number;
    name: string;
    parameters: {[key: string]: object};
    registeringExisting?: boolean;
    selectedScopes?: {[key: string]: boolean};
    tags: Array<Tag | {label: string; value: string}>;
    visibility: 'PRIVATE' | 'WORKSPACE' | 'ORGANIZATION';
}

interface ConnectionDialogProps {
    /**
     * Opts this instance into claiming the `connection.create` command intent on mount. Only the header and
     * empty-state instances on the connections list page (the page the "Create connection" command navigates to)
     * should pass `true`. `ConnectionDialog` has many other call sites -- edit dialogs, embedded/EE surfaces,
     * workflow-editor connection pickers -- and every one of them must leave this `false`, or it becomes an
     * eligible claimant for a stale intent meant for the automation connections list.
     */
    claimsCreateIntent?: boolean;
    componentDefinition?: ComponentDefinition;
    componentDefinitions: ComponentDefinitionBasic[];
    connection?: ConnectionI | undefined;
    connectionTagsQueryKey: QueryKey;
    connectionsQueryKey: QueryKey;
    /**
     * Overrides the dialog's default description ("Create your connection to connect to the chosen
     * service"). Optional and additive -- every existing caller keeps today's description unchanged.
     * Only rendered in the same place the default description is: while `connection?.id` is falsy.
     */
    description?: string;
    onClose?: () => void;
    onConnectionCreate?: (connectionId: number) => void;
    /**
     * Offers the Organization rung in the visibility picker. Opt-in because ORGANIZATION is not reachable through the
     * ordinary create path -- setConnectionVisibility rejects it outright -- so only a surface whose create mutation
     * writes an organization connection may show it.
     */
    showOrganizationOption?: boolean;
    /**
     * Opens the dialog directly in credential-replacement mode instead of the rename-only edit body. For surfaces
     * whose entire purpose is reconnecting an account -- the embedded hub's Reconnect action -- where making the user
     * find an "Update credentials" button first would be pointless. No Back control is rendered in that case: there
     * is no rename-only body behind it to go back to.
     */
    startInCredentialsMode?: boolean;
    /**
     * Overrides the dialog's default title (`Create Connection` / `Edit Connection`, derived from
     * `connection?.id`). Optional and additive -- every existing caller keeps today's title
     * unchanged. Exists for callers like the hub's reconnect flow, whose `connection` is deliberately
     * `id`-less (see `HubConnectionDialog`) so the create/edit branching itself must stay untouched,
     * yet the dialog still needs to read as something other than "Create Connection".
     */
    title?: string;
    triggerNode?: ReactNode;
    useCreateConnectionMutation?: (mutationProps: {
        onSuccess?: (result: number, variables: ConnectionI) => void;
        onError?: (error: Error, variables: ConnectionI) => void;
    }) => UseMutationResult<number, Error, ConnectionI, unknown>;
    useGetConnectionTagsQuery: () => UseQueryResult<Tag[], Error>;
    /**
     * Supplied only by surfaces that allow an existing connection's credentials to be replaced. Absent means the
     * "Update credentials" affordance is not rendered at all, which is how every caller that has not opted in keeps
     * today's rename-only edit dialog.
     */
    useUpdateConnectionCredentialsMutation?: (mutationProps: {
        onSuccess?: (result: void, variables: ConnectionI) => void;
        onError?: (error: Error, variables: ConnectionI) => void;
    }) => UseMutationResult<void, Error, ConnectionI, unknown>;
    useUpdateConnectionMutation?: (mutationProps: {
        onSuccess?: (result: void, variables: ConnectionI) => void;
        onError?: (error: Error, variables: ConnectionI) => void;
    }) => UseMutationResult<void, Error, ConnectionI, unknown>;
}

const ConnectionDialog = ({
    claimsCreateIntent = false,
    componentDefinition,
    componentDefinitions,
    connection,
    connectionTagsQueryKey,
    connectionsQueryKey,
    description,
    onClose,
    onConnectionCreate,
    showOrganizationOption,
    startInCredentialsMode,
    title,
    triggerNode,
    useCreateConnectionMutation,
    useGetConnectionTagsQuery,
    useUpdateConnectionCredentialsMutation,
    useUpdateConnectionMutation,
}: ConnectionDialogProps) => {
    const [authorizationType, setAuthorizationType] = useState<string>();
    const [connectionVersion, setConnectionVersion] = useState(1);
    const [isOpen, setIsOpen] = useState(!triggerNode);
    const [isUpdatingCredentials, setIsUpdatingCredentials] = useState(false);
    const [oAuth2Error, setOAuth2Error] = useState<string>();
    const [wizardStep, setWizardStep] = useState<'configuration_step' | 'oauth_step'>('configuration_step');
    const [selectedComponentDefinition, setSelectedComponentDefinition] = useState<
        ComponentDefinitionBasic | undefined
    >(componentDefinition);
    const [usePredefinedOAuthApp, setUsePredefinedOAuthApp] = useState(true);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentType = usePlatformTypeStore((state) => state.currentType);
    // No admin check here any more: WORKSPACE is the default every connection is created with, so gating it would
    // fail every ordinary create. ORGANIZATION, which does require admin, is not offered at creation at all.
    // Compose the shared EE-edition primitive with this dialog's platform-type scope. Keeping the
    // edition check in one hook means a future migration away from EditionType.EE updates the
    // list-page gate (useVisibilityFeatureEnabled) and this dialog simultaneously.
    const isEE = useIsVisibilityEditionEnabled();
    const visibilityFeatureEnabled = isEE && currentType === PlatformType.AUTOMATION;

    useCommandIntent('connection.create', () => setIsOpen(true), claimsCreateIntent);

    /* eslint-disable @typescript-eslint/no-unused-vars */
    const [_, copyToClipboard] = useCopyToClipboard();

    const form = useForm<ConnectionDialogFormProps>({
        defaultValues: {
            authorizationType: undefined,
            componentName: componentDefinition?.name,
            credentialRef: '',
            credentialStoreType: ConnectionCredentialStoreType.Database,
            environmentId: connection?.environmentId || currentEnvironmentId,
            id: connection?.id,
            name: connection?.name || componentDefinition?.title || '',
            registeringExisting: false,
            tags:
                connection?.tags?.map((tag) => ({
                    ...tag,
                    label: tag.name,
                })) || [],
            visibility: 'WORKSPACE',
        },
        mode: 'onTouched',
    });

    const {control, formState, getValues, handleSubmit, reset: formReset, setValue, watch} = form;

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

    const {data: storesData} = useConnectionCredentialStoresQuery();
    const stores = storesData?.connectionCredentialStores ?? [];
    const showPicker = stores.length > 1;
    const isEdit = !!connection?.id;

    // A connection whose secret lives in an external store, or that the platform manages on the user's behalf, is not
    // ours to rewrite: ConnectionServiceImpl throws ReadOnlyCredentialStoreException for the former, and the user
    // would have typed a secret before finding out. Hide the affordance rather than fail after the fact.
    const credentialsAreExternallyStored =
        (!!connection?.credentialStoreType && connection.credentialStoreType !== 'DATABASE') || !!connection?.managed;

    const canUpdateCredentials = isEdit && !!useUpdateConnectionCredentialsMutation && !credentialsAreExternallyStored;

    const handleConnectionSuccess = (connectionId: number | void) => {
        queryClient.invalidateQueries({
            queryKey: ComponentDefinitionKeys.componentDefinitions,
        });

        queryClient.invalidateQueries({
            queryKey: connectionsQueryKey,
        });

        queryClient.invalidateQueries({
            queryKey: connectionTagsQueryKey,
        });

        if (!isEdit) {
            toast('Connection created', {description: `${getValues().name} connection was successfully created`});

            if (connectionId && onConnectionCreate) {
                onConnectionCreate(connectionId);
            }
        }

        closeDialog();
    };

    const connectionMutation = (useUpdateConnectionMutation || useCreateConnectionMutation)!({
        onSuccess: handleConnectionSuccess,
    });

    const credentialsMutation = useUpdateConnectionCredentialsMutation?.({
        onSuccess: () => {
            // Deliberately not "credentials verified": there is no test-connection step, so a successful call means
            // the values were stored, not that they work. A wrong credential is re-flagged INVALID by
            // TokenRefreshHandler on the next execution.
            toast('Credentials updated', {
                description: 'The new credentials were saved. They are verified the next time the connection runs.',
            });

            handleConnectionSuccess();
        },
    });

    const registerExistingMutation = useRegisterExistingConnectionMutation({
        onSuccess: handleConnectionSuccess,
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

    // Keyed off the mode, not off `connection?.id`: a credential replacement always has an id, and an OAuth2
    // reconnect still needs the consent step to run.
    const showOAuth2Step =
        (isOAuth2AuthorizationType || isOAuth2ImplicitCodeType) && (!connection?.id || isUpdatingCredentials);

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

            setIsUpdatingCredentials(false);
            setOAuth2Error(undefined);
            setWizardStep('configuration_step');

            if (!componentDefinition) {
                setAuthorizationType(undefined);
                setSelectedComponentDefinition(undefined);
            }

            connectionMutation.reset();
            registerExistingMutation.reset();

            if (onClose) {
                onClose();
            }
        }, 300);
    }

    async function handleCodeSuccess(payload: CodePayloadI) {
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
        const {componentName, name, parameters, tags, visibility} = getValues();

        return {
            authorizationType,
            componentName,
            connectionVersion,
            environmentId: currentEnvironmentId,
            name,
            parameters: {
                ...parameters,
                ...additionalParameters,
            },
            tags: tags,
            ...(visibilityFeatureEnabled ? {visibility} : {}),
        } as ConnectionI;
    }

    function getNewOAuth2AuthorizationParameters() {
        const {componentName, parameters} = getValues();

        return {
            authorizationType: authorizationType as AuthorizationType,
            componentName,
            connectionVersion,
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
        // Tested before the `connection?.id` branch below: in this mode an id IS present, so the rename-only branch
        // would otherwise swallow the submission and silently discard the new credentials.
        if (isUpdatingCredentials && credentialsMutation) {
            const {parameters} = getValues();

            return credentialsMutation.mutateAsync({
                id: connection!.id,
                parameters: {
                    ...parameters,
                    ...additionalParameters,
                },
                version: connection!.version,
            } as ConnectionI);
        }

        if (connection?.id) {
            const {name, tags} = getValues();

            connectionMutation.mutate({
                id: connection?.id,
                name,
                tags,
                version: connection.version,
            } as ConnectionI);
        } else {
            const {componentName, credentialRef, credentialStoreType, registeringExisting} = getValues();

            if (
                registeringExisting &&
                credentialStoreType &&
                credentialStoreType !== ConnectionCredentialStoreType.Database
            ) {
                return registerExistingMutation.mutateAsync({
                    componentName: componentName!,
                    connectionVersion,
                    credentialRef: credentialRef!,
                    credentialStoreType,
                    environmentId: String(currentEnvironmentId),
                    name: getValues('name')!,
                });
            }

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

    // Initialize selectedScopes with required scopes pre-selected when OAuth2 parameters load
    useEffect(() => {
        if (oAuth2AuthorizationParameters?.scopes && wizardStep === 'oauth_step') {
            const currentSelectedScopes = getValues('selectedScopes');

            if (!currentSelectedScopes || Object.keys(currentSelectedScopes).length === 0) {
                setValue('selectedScopes', {...oAuth2AuthorizationParameters.scopes});
            }
        }
    }, [oAuth2AuthorizationParameters?.scopes, wizardStep, getValues, setValue]);

    useEffect(() => {
        const initialAuthorizationType =
            authorizationOptions && authorizationOptions.length > 0 ? authorizationOptions[0].value : undefined;

        setAuthorizationType(initialAuthorizationType);

        if (initialAuthorizationType) {
            setValue('authorizationType', initialAuthorizationType);
        }
    }, [authorizationsExists, authorizationOptions, selectedComponentDefinition, setValue]);

    // Runs after the effect above so it wins: that one defaults to the FIRST authorization option, which is not
    // necessarily the one this connection was created with. The type selector is hidden while replacing credentials,
    // so without this the form would render the wrong component's fields -- or none at all.
    useEffect(() => {
        if (!isEdit) {
            return;
        }

        if (connection?.authorizationType) {
            setAuthorizationType(connection.authorizationType);
            setValue('authorizationType', connection.authorizationType);
        }

        if (startInCredentialsMode || connection?.credentialStatus === 'INVALID') {
            setIsUpdatingCredentials(true);
        }
    }, [connection?.authorizationType, connection?.credentialStatus, isEdit, setValue, startInCredentialsMode]);

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

            <DialogContent
                className={twMerge('gap-0 p-0', wizardStep === 'oauth_step' && 'sm:max-w-xl')}
                onInteractOutside={(event) => event.preventDefault()}
            >
                <Form {...form}>
                    <DialogHeader className="flex flex-row items-center justify-between space-y-0 px-6 pt-6 pb-4">
                        <div className="flex flex-col space-y-1">
                            <DialogTitle>{title || `${connection?.id ? 'Edit' : 'Create'} Connection`}</DialogTitle>

                            {!connection?.id && (
                                <DialogDescription>
                                    {description || 'Create your connection to connect to the chosen service'}
                                </DialogDescription>
                            )}
                        </div>

                        <DialogCloseButton />
                    </DialogHeader>

                    {errors?.length > 0 && <Errors errors={errors} />}

                    <div className="flex max-h-dialog-height min-w-0 flex-col space-y-4 overflow-y-auto px-6">
                        {connection?.id && currentType === PlatformType.EMBEDDED && (
                            <FormField
                                control={control}
                                name="id"
                                render={({field}) => (
                                    <FormControl>
                                        <div className="flex">
                                            <div className="relative flex grow items-stretch focus-within:z-10">
                                                <Input
                                                    {...field}
                                                    className="rounded-r-none bg-surface-neutral-secondary text-content-neutral-secondary"
                                                    readOnly
                                                    value={connection?.id}
                                                />
                                            </div>

                                            <Button
                                                className="-ml-px rounded-l-none rounded-r-md border border-stroke-neutral-secondary bg-surface-neutral-secondary shadow-xs hover:bg-surface-neutral-primary-hover"
                                                icon={
                                                    <ClipboardIcon
                                                        aria-hidden="true"
                                                        className="size-4 text-content-neutral-tertiary"
                                                    />
                                                }
                                                onClick={() => copyToClipboard(connection?.id?.toString() ?? '')}
                                                size="icon"
                                                type="button"
                                                variant="ghost"
                                            />
                                        </div>
                                    </FormControl>
                                )}
                            />
                        )}

                        {(wizardStep === 'configuration_step' || oAuth2AuthorizationParametersLoading) && (
                            <>
                                {isUpdatingCredentials && connection?.credentialStatus === 'INVALID' && (
                                    <Alert variant="destructive">
                                        <AlertTitle>These credentials were rejected</AlertTitle>

                                        <AlertDescription>
                                            Workflows using this connection are blocked until new credentials are saved.
                                        </AlertDescription>
                                    </Alert>
                                )}

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

                                {/* Name and tags belong to the rename-only body; hidden here so the credential
                                    replacement reads as one job with one outcome. */}
                                <FormField
                                    control={form.control}
                                    name="name"
                                    render={({field}) => (
                                        <FormItem className={twMerge(isUpdatingCredentials && 'hidden')}>
                                            <FormLabel>Name</FormLabel>

                                            <FormControl>
                                                <Input placeholder="My Connection" {...field} />
                                            </FormControl>

                                            <FormMessage />
                                        </FormItem>
                                    )}
                                    rules={{required: true}}
                                />

                                {showPicker && !isEdit && (
                                    <FormField
                                        control={form.control}
                                        name="credentialStoreType"
                                        render={({field}) => (
                                            <FormItem>
                                                <FormLabel>Credential storage</FormLabel>

                                                <Select
                                                    onValueChange={(value) => {
                                                        field.onChange(value);

                                                        const selected = stores.find((store) => store.type === value);

                                                        if (selected?.readOnly) {
                                                            form.setValue('registeringExisting', true);
                                                        } else if (value === ConnectionCredentialStoreType.Database) {
                                                            form.setValue('registeringExisting', false);
                                                        }
                                                    }}
                                                    value={field.value}
                                                >
                                                    <FormControl>
                                                        <SelectTrigger>
                                                            <SelectValue />
                                                        </SelectTrigger>
                                                    </FormControl>

                                                    <SelectContent>
                                                        {stores.map((store) => (
                                                            <SelectItem key={store.type} value={store.type}>
                                                                {connectionCredentialStoreLabels[store.type]}
                                                            </SelectItem>
                                                        ))}
                                                    </SelectContent>
                                                </Select>

                                                <FormMessage />
                                            </FormItem>
                                        )}
                                    />
                                )}

                                {showPicker && isEdit && (
                                    <FormItem>
                                        <FormLabel>Credential storage</FormLabel>

                                        <Input
                                            disabled
                                            value={
                                                connectionCredentialStoreLabels[
                                                    form.watch('credentialStoreType') ??
                                                        ConnectionCredentialStoreType.Database
                                                ]
                                            }
                                        />

                                        <p className="text-xs text-muted-foreground">
                                            Credential storage cannot be changed after creation.
                                        </p>
                                    </FormItem>
                                )}

                                {showPicker &&
                                    !isEdit &&
                                    form.watch('credentialStoreType') !== ConnectionCredentialStoreType.Database && (
                                        <>
                                            {stores.find((store) => store.type === form.watch('credentialStoreType'))
                                                ?.readOnly && (
                                                <Alert>
                                                    <AlertDescription>
                                                        This credential store is configured read-only by your
                                                        administrator. Provision the secret externally, then reference
                                                        it here.
                                                    </AlertDescription>
                                                </Alert>
                                            )}

                                            <FormField
                                                control={form.control}
                                                name="registeringExisting"
                                                render={({field}) => (
                                                    <FormItem className="flex items-center gap-2">
                                                        <FormControl>
                                                            <Switch
                                                                checked={field.value}
                                                                disabled={
                                                                    stores.find(
                                                                        (store) =>
                                                                            store.type ===
                                                                            form.watch('credentialStoreType')
                                                                    )?.readOnly
                                                                }
                                                                onCheckedChange={field.onChange}
                                                            />
                                                        </FormControl>

                                                        <FormLabel className="!mt-0">
                                                            Register existing credential
                                                        </FormLabel>
                                                    </FormItem>
                                                )}
                                            />
                                        </>
                                    )}

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

                                {form.watch('registeringExisting') ? (
                                    <FormField
                                        control={form.control}
                                        name="credentialRef"
                                        render={({field}) => (
                                            <FormItem>
                                                <FormLabel>Credential reference</FormLabel>

                                                <FormControl>
                                                    <Input {...field} placeholder="bytechef/connections/..." />
                                                </FormControl>

                                                <p className="text-xs text-muted-foreground">
                                                    The path or UUID where your secret lives in the external store.
                                                    Format depends on your operator&apos;s path template configuration.
                                                </p>

                                                <FormMessage />
                                            </FormItem>
                                        )}
                                        rules={{required: true}}
                                    />
                                ) : (
                                    <>
                                        {!connection?.id && visibilityFeatureEnabled && (
                                            <FormField
                                                control={control}
                                                name="visibility"
                                                render={({field}) => (
                                                    <FormItem>
                                                        <FormLabel>Visibility</FormLabel>

                                                        <FormControl>
                                                            {/* Grants cannot be written before the connection has
                                                                an id, so creation offers reach only; the list-page
                                                                picker adds people afterwards. */}

                                                            <ResourceVisibilityPicker
                                                                grantedUserIds={[]}
                                                                onGrantedUserIdsChange={() => undefined}
                                                                onVisibilityChange={field.onChange}
                                                                showOrganizationOption={showOrganizationOption}
                                                                showSpecificPeopleOption={false}
                                                                visibility={field.value}
                                                            />
                                                        </FormControl>

                                                        <FormMessage />
                                                    </FormItem>
                                                )}
                                            />
                                        )}

                                        {connection?.id && visibilityFeatureEnabled && connection.visibility && (
                                            <FormItem className="space-x-2">
                                                <FormLabel>Visibility</FormLabel>

                                                <FormControl>
                                                    <ResourceVisibilityBadge visibility={connection.visibility} />
                                                </FormControl>

                                                <p className="text-xs text-muted-foreground">
                                                    Change visibility and sharing from the connection list.
                                                </p>
                                            </FormItem>
                                        )}

                                        {!connection?.id &&
                                            showConnectionProperties &&
                                            !!connectionDefinition.properties && (
                                                <WorkflowMockProvider>
                                                    <Properties
                                                        control={control}
                                                        formState={formState}
                                                        hideFromAi={true}
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

                                        {(!connection?.id || isUpdatingCredentials) &&
                                            showAuthorizationProperties &&
                                            !!authorizations?.length &&
                                            authorizations[0]?.properties && (
                                                <WorkflowMockProvider>
                                                    <Properties
                                                        control={control}
                                                        formState={formState}
                                                        hideFromAi={true}
                                                        properties={authorizations[0]?.properties}
                                                    />
                                                </WorkflowMockProvider>
                                            )}
                                    </>
                                )}

                                {showOAuth2AppPredefined && (
                                    <div>
                                        <a
                                            className="text-sm text-content-brand-primary"
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

                                {!tagsLoading && !isUpdatingCredentials && (
                                    <FormField
                                        control={control}
                                        name="tags"
                                        render={({field}) => (
                                            <FormItem className="pb-2">
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
                                                        options={
                                                            remainingTags?.map((tag: Tag) => ({
                                                                label: tag.name,
                                                                value: tag.name.toLowerCase().replace(/\W/g, ''),
                                                                ...tag,
                                                            })) ?? []
                                                        }
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
                                <Alert className="border-blue-50 bg-surface-brand-secondary text-content-brand-primary">
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

                                {scopes && Object.keys(scopes).length > 0 && (
                                    <FormField
                                        control={control}
                                        name="selectedScopes"
                                        render={({field}) => {
                                            const hasSelectedScopes =
                                                field.value &&
                                                Object.keys(field.value).length === Object.keys(scopes).length;

                                            return (
                                                <Scopes
                                                    onSelectedScopesChange={field.onChange}
                                                    scopeDefinitions={scopes}
                                                    selectedScopes={hasSelectedScopes ? field.value : scopes}
                                                />
                                            );
                                        }}
                                    />
                                )}
                            </>
                        )}
                    </div>

                    {connection?.id && connectionDefinition && (
                        <div className="min-w-0 px-6 pt-4">
                            <ConnectionParameters
                                authorizationParameters={connection.authorizationParameters}
                                authorizationType={connection.authorizationType}
                                baseUri={connection.baseUri}
                                connectionDefinition={connectionDefinition}
                                connectionParameters={connection.connectionParameters}
                            />
                        </div>
                    )}

                    <DialogFooter
                        className={twMerge(
                            'flex-row flex-wrap items-center gap-2 px-6 pt-4 pb-6',
                            connectionDefinition?.help?.learnMoreUrl ? 'sm:justify-between' : 'sm:justify-end'
                        )}
                    >
                        {connectionDefinition?.help?.learnMoreUrl && (
                            <Link target="_blank" to={connectionDefinition.help.learnMoreUrl}>
                                <Button size="sm" variant="ghost">
                                    Documentation <ExternalLinkIcon />
                                </Button>
                            </Link>
                        )}

                        <div className="flex flex-col-reverse sm:flex-row sm:items-center sm:space-x-2">
                            <Select
                                defaultValue={String(connectionDefinition?.version ?? 1)}
                                onValueChange={(value) => setConnectionVersion(Number(value))}
                            >
                                <SelectTrigger className="w-auto border-none shadow-none">
                                    <SelectValue placeholder="Choose version..." />
                                </SelectTrigger>

                                <SelectContent>
                                    <SelectItem value={String(connectionDefinition?.version ?? 1)}>
                                        v{connectionDefinition?.version ?? 1}
                                    </SelectItem>
                                </SelectContent>
                            </Select>

                            {wizardStep === 'oauth_step' && (
                                <Button
                                    label="Previous"
                                    onClick={() => {
                                        connectionMutation.reset();

                                        setOAuth2Error(undefined);

                                        setWizardStep('configuration_step');
                                    }}
                                    type="button"
                                    variant="outline"
                                />
                            )}

                            {wizardStep === 'configuration_step' && (
                                <Button label="Cancel" onClick={closeDialog} type="button" variant="outline" />
                            )}

                            {showOAuth2Step && (
                                <>
                                    {wizardStep === 'configuration_step' && (
                                        <Button
                                            disabled={!formState.isValid}
                                            label="Next"
                                            onClick={handleSubmit(() => {
                                                setWizardStep('oauth_step');
                                            })}
                                            type="submit"
                                        />
                                    )}

                                    {wizardStep === 'oauth_step' &&
                                        oAuth2AuthorizationParameters?.authorizationUrl &&
                                        oAuth2AuthorizationParameters?.clientId && (
                                            <OAuth2Button
                                                authorizationUrl={oAuth2AuthorizationParameters.authorizationUrl}
                                                clientId={oAuth2AuthorizationParameters.clientId}
                                                extraQueryParameters={
                                                    oAuth2AuthorizationParameters?.extraQueryParameters
                                                }
                                                onClick={(getAuth: () => void) => {
                                                    getAuth();
                                                }}
                                                onCodeSuccess={handleCodeSuccess}
                                                onError={(error: string) => setOAuth2Error(error)}
                                                onTokenSuccess={handleTokenSuccess}
                                                redirectUri={oAuth2Properties?.redirectUri ?? ''}
                                                responseType={isOAuth2AuthorizationType ? 'code' : 'token'}
                                                scopes={
                                                    watch('selectedScopes') ?? oAuth2AuthorizationParameters?.scopes
                                                }
                                            />
                                        )}
                                </>
                            )}

                            {canUpdateCredentials && !isUpdatingCredentials && (
                                <Button onClick={() => setIsUpdatingCredentials(true)} type="button" variant="outline">
                                    <KeyRoundIcon /> Update credentials
                                </Button>
                            )}

                            {/* No Back when the dialog opened straight into this mode: there is no rename-only body
                                behind it, so Back would strand the user in a state they never chose to enter. */}

                            {isUpdatingCredentials && !startInCredentialsMode && (
                                <Button
                                    label="Back"
                                    onClick={() => setIsUpdatingCredentials(false)}
                                    type="button"
                                    variant="outline"
                                />
                            )}

                            {!showOAuth2Step && (
                                <Button
                                    disabled={!formState.isValid}
                                    label={isUpdatingCredentials ? 'Update credentials' : 'Save'}
                                    onClick={handleSubmit(() => saveConnection())}
                                    type="submit"
                                />
                            )}
                        </div>
                    </DialogFooter>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

const Errors = ({errors}: {errors: string[]}) => (
    <ul>
        {errors.map((error, index) => (
            <li
                className="my-4 rounded-md bg-surface-destructive-secondary p-4 text-sm text-content-destructive"
                key={`error_${index}`}
            >
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
                className="-ml-px rounded-l-none rounded-r-md border border-stroke-neutral-secondary bg-surface-neutral-secondary shadow-xs hover:bg-surface-neutral-primary-hover"
                icon={<ClipboardIcon aria-hidden="true" className="size-4 text-content-neutral-tertiary" />}
                onClick={() => copyToClipboard(redirectUri ?? '')}
                size="icon"
                variant="ghost"
            />
        </div>
    );
};

export default ConnectionDialog;
