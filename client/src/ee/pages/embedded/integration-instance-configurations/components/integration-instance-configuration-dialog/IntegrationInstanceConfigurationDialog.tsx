import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogBody,
    DialogCancelButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogMain,
    DialogNextButton,
    DialogPreviousButton,
    type DialogStepI,
    DialogTrigger,
    DialogWizard,
    useDialogSteps,
} from '@/components/Dialog';
import LoadingDots from '@/components/LoadingDots';
import LoadingIcon from '@/components/LoadingIcon';
import {Form} from '@/components/ui/form';
import IntegrationInstanceConfigurationDialogOauth2Step from '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialogOauth2Step';
import {useWorkflowsEnabledStore} from '@/ee/pages/embedded/integration-instance-configurations/stores/useWorkflowsEnabledStore';
import ConfigurationDialogTabs, {ConfigurationDialogTabType} from '@/ee/pages/embedded/shared/ConfigurationDialogTabs';
import {
    ComponentConnection,
    IntegrationInstanceConfiguration,
    IntegrationInstanceConfigurationWorkflow,
    IntegrationInstanceConfigurationWorkflowConnection,
} from '@/ee/shared/middleware/embedded/configuration';
import {
    useCreateIntegrationInstanceConfigurationMutation,
    useUpdateIntegrationInstanceConfigurationCredentialsMutation,
    useUpdateIntegrationInstanceConfigurationMutation,
} from '@/ee/shared/mutations/embedded/integrationInstanceConfigurations.mutations';
import {IntegrationInstanceConfigurationTagKeys} from '@/ee/shared/queries/embedded/integrationInstanceConfigurationTags.queries';
import {
    IntegrationInstanceConfigurationKeys,
    useGetIntegrationInstanceConfigurationsQuery,
} from '@/ee/shared/queries/embedded/integrationInstanceConfigurations.queries';
import {
    useGetIntegrationVersionWorkflowsQuery,
    useGetIntegrationWorkflowsQuery,
} from '@/ee/shared/queries/embedded/integrationWorkflows.queries';
import {IntegrationKeys, useGetIntegrationQuery} from '@/ee/shared/queries/embedded/integrations.queries';
import {WorkflowMockProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import ConnectionParameters from '@/shared/components/connection/ConnectionParameters';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import useWorkflowTestConfigurationConnections from '@/shared/hooks/useWorkflowTestConfigurationConnections';
import {Authorization, AuthorizationType} from '@/shared/middleware/platform/configuration';
import {useGetConnectionDefinitionQuery} from '@/shared/queries/platform/connectionDefinitions.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {KeyRoundIcon, Settings2Icon} from 'lucide-react';
import {ReactNode, useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {Control, DefaultValues, FieldValues, FormState, useForm, useWatch} from 'react-hook-form';
import {useShallow} from 'zustand/react/shallow';

import IntegrationInstanceConfigurationDialogBasicStep from './IntegrationInstanceConfigurationDialogBasicStep';
import IntegrationInstanceConfigurationDialogCredentials from './IntegrationInstanceConfigurationDialogCredentials';
import IntegrationInstanceConfigurationDialogWorkflowsStep from './IntegrationInstanceConfigurationDialogWorkflowsStep';
import {NO_AUTHORIZATION_TYPE} from './noAuthorizationType';

const OAUTH2_AUTHORIZATION_TYPES = new Set<string>([
    AuthorizationType.Oauth2AuthorizationCode,
    AuthorizationType.Oauth2AuthorizationCodePkce,
    AuthorizationType.Oauth2ImplicitCode,
]);

const getDefaultAuthorizationType = (authorizations: Authorization[] | undefined, authorizationRequired: boolean) => {
    if (!authorizationRequired) {
        return authorizations?.length ? undefined : NO_AUTHORIZATION_TYPE;
    }

    return (authorizations?.length === 1 ? authorizations[0].type : undefined) as
        IntegrationInstanceConfiguration['authorizationType'] | undefined;
};

const getDefaultValues = (
    integrationInstanceConfiguration: IntegrationInstanceConfiguration | undefined,
    updateIntegrationVersion: boolean
): DefaultValues<IntegrationInstanceConfiguration> => ({
    authorizationType: integrationInstanceConfiguration?.authorizationType,
    description: integrationInstanceConfiguration?.description || undefined,
    enabled: integrationInstanceConfiguration?.enabled || false,
    integrationId: integrationInstanceConfiguration?.integrationId || undefined,
    integrationInstanceConfigurationWorkflows: [],
    integrationVersion: updateIntegrationVersion
        ? undefined
        : integrationInstanceConfiguration?.integrationVersion || undefined,
    name: integrationInstanceConfiguration?.name || undefined,
    permissionExpression: integrationInstanceConfiguration?.permissionExpression || undefined,
    tags:
        integrationInstanceConfiguration?.tags?.map((tag) => ({
            ...tag,
            label: tag.name,
        })) || [],
});

const toSavedAuthorizationType = (authorizationType: IntegrationInstanceConfiguration['authorizationType']) =>
    authorizationType === NO_AUTHORIZATION_TYPE ? undefined : authorizationType;

interface IntegrationInstanceConfigurationDialogStepContentProps {
    basicStepContent: ReactNode;
    isWorkflowsPending: boolean;
    oauth2StepContent: ReactNode;
    workflowsStepContent: ReactNode;
}

const IntegrationInstanceConfigurationDialogStepContent = ({
    basicStepContent,
    isWorkflowsPending,
    oauth2StepContent,
    workflowsStepContent,
}: IntegrationInstanceConfigurationDialogStepContentProps) => {
    const {currentStep} = useDialogSteps();

    if (currentStep.id === 'oauth2') {
        return <>{oauth2StepContent}</>;
    }

    if (currentStep.id !== 'workflows') {
        return <>{basicStepContent}</>;
    }

    if (isWorkflowsPending) {
        return (
            <div className="flex justify-center py-12">
                <LoadingDots />
            </div>
        );
    }

    return <>{workflowsStepContent}</>;
};

IntegrationInstanceConfigurationDialogStepContent.displayName = 'IntegrationInstanceConfigurationDialogStepContent';

interface CredentialsFormI {
    parameters: Record<string, unknown>;
}

interface IntegrationInstanceConfigurationDialogProps {
    excludeWorkflowIds?: Set<string>;
    filterWorkflowUuids?: string[];
    integrationInstanceConfiguration?: IntegrationInstanceConfiguration;
    onClose?: () => void;
    onSuccess?: (integrationInstanceConfigurationId: number) => void;
    showTabs?: boolean;
    triggerNode?: ReactNode;
    updateIntegrationVersion?: boolean;
}

const IntegrationInstanceConfigurationDialog = ({
    excludeWorkflowIds,
    filterWorkflowUuids,
    integrationInstanceConfiguration: integrationInstanceConfigurationProp,
    onClose,
    onSuccess,
    showTabs = false,
    triggerNode,
    updateIntegrationVersion: updateIntegrationVersionProp = false,
}: IntegrationInstanceConfigurationDialogProps) => {
    const [basicStepTab, setBasicStepTab] = useState<ConfigurationDialogTabType | undefined>();
    const [curIntegrationId, setCurIntegrationId] = useState(integrationInstanceConfigurationProp?.integrationId);
    const [curIntegrationVersion, setCurIntegrationVersion] = useState<number | undefined>(
        updateIntegrationVersionProp ? undefined : integrationInstanceConfigurationProp?.integrationVersion
    );
    const [isOpen, setIsOpen] = useState(!triggerNode);
    const [isUpdatingCredentials, setIsUpdatingCredentials] = useState(false);
    const [selectedExistingConfiguration, setSelectedExistingConfiguration] = useState<
        IntegrationInstanceConfiguration | undefined
    >();
    const [usePredefinedOAuthApp, setUsePredefinedOAuthApp] = useState(true);

    const defaultedAuthorizationsRef = useRef<Authorization[]>(undefined);
    const initializedConfigKeyRef = useRef<string>('');
    const prefilledConfigKeyRef = useRef<string>('');

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const [resetWorkflowsEnabledStore, setWorkflowEnabled] = useWorkflowsEnabledStore(
        useShallow(({reset, setWorkflowEnabled}) => [reset, setWorkflowEnabled])
    );

    const {captureIntegrationInstanceConfigurationCreated} = useAnalytics();

    const integrationInstanceConfiguration = selectedExistingConfiguration ?? integrationInstanceConfigurationProp;

    const updateIntegrationVersion = updateIntegrationVersionProp || !!selectedExistingConfiguration;

    const isChangeVersionTab = showTabs && basicStepTab === 'change-version';

    const defaultValues = getDefaultValues(integrationInstanceConfigurationProp, updateIntegrationVersionProp);

    const form = useForm<IntegrationInstanceConfiguration>({defaultValues});

    const {control, formState, getValues, handleSubmit, reset, setValue} = form;

    const credentialsForm = useForm<CredentialsFormI>({defaultValues: {parameters: {}}});

    const credentialsParameters = useWatch({control: credentialsForm.control, name: 'parameters'});

    const {data: integrationInstanceConfigurations, isFetching: integrationInstanceConfigurationsIsFetching} =
        useGetIntegrationInstanceConfigurationsQuery(
            {
                environmentId: DEVELOPMENT_ENVIRONMENT,
                integrationId: integrationInstanceConfigurationProp?.integrationId,
            },
            showTabs && isOpen && !!integrationInstanceConfigurationProp?.integrationId
        );

    const {data: allWorkflows, isPending: isWorkflowsPending} = useGetIntegrationVersionWorkflowsQuery(
        curIntegrationId!,
        curIntegrationVersion!,
        true,
        !!curIntegrationId && !!curIntegrationVersion
    );

    const workflows = useMemo(() => {
        if (!allWorkflows) {
            return undefined;
        }

        let filteredWorkflows = allWorkflows;

        if (excludeWorkflowIds?.size) {
            filteredWorkflows = filteredWorkflows.filter((workflow) => !excludeWorkflowIds.has(workflow.id!));
        }

        if (filterWorkflowUuids === undefined) {
            return filteredWorkflows;
        }

        if (filterWorkflowUuids.length === 0) {
            return [];
        }

        return filteredWorkflows.filter(
            (workflow) => workflow.workflowUuid && filterWorkflowUuids.includes(workflow.workflowUuid)
        );
    }, [allWorkflows, excludeWorkflowIds, filterWorkflowUuids]);

    const {data: integration} = useGetIntegrationQuery(curIntegrationId!, undefined, !!curIntegrationId);

    const {data: latestIntegrationWorkflows} = useGetIntegrationWorkflowsQuery(
        curIntegrationId!,
        isOpen && !!curIntegrationId
    );

    const workflowTestConfigurationConnections = useWorkflowTestConfigurationConnections(
        latestIntegrationWorkflows,
        currentEnvironmentId,
        isOpen && !!latestIntegrationWorkflows
    );

    const {data: connectionDefinition} = useGetConnectionDefinitionQuery({
        componentName: integration?.componentName as string,
    });

    const authorizations = connectionDefinition?.authorizations;
    const authorizationRequired = connectionDefinition?.authorizationRequired !== false;

    const selectedAuthorizationType = useWatch({control, name: 'authorizationType'});

    const selectedAuthorization = authorizations?.find(
        (authorization) => authorization.type === selectedAuthorizationType
    );

    const oAuth2Authorization =
        selectedAuthorization && OAUTH2_AUTHORIZATION_TYPES.has(selectedAuthorization.type!)
            ? selectedAuthorization
            : undefined;

    const authorizationTypePending =
        !integrationInstanceConfiguration?.id && !!curIntegrationId && !selectedAuthorizationType;

    const queryClient = useQueryClient();

    const handleSaveSuccess = (savedIntegrationInstanceConfigurationId?: number) => {
        if (!integrationInstanceConfiguration?.id) {
            captureIntegrationInstanceConfigurationCreated();
        }

        void queryClient.invalidateQueries({
            queryKey: IntegrationInstanceConfigurationKeys.integrationInstanceConfigurations,
        });
        void queryClient.invalidateQueries({
            queryKey: IntegrationInstanceConfigurationTagKeys.integrationInstanceConfigurationTags,
        });
        void queryClient.invalidateQueries({
            queryKey: IntegrationKeys.filteredIntegrations({}),
        });

        closeDialog();

        if (onSuccess && savedIntegrationInstanceConfigurationId !== undefined) {
            onSuccess(savedIntegrationInstanceConfigurationId);
        }
    };

    const createIntegrationInstanceConfigurationMutation = useCreateIntegrationInstanceConfigurationMutation({
        onSuccess: (createdIntegrationInstanceConfigurationId) =>
            handleSaveSuccess(createdIntegrationInstanceConfigurationId),
    });

    const updateIntegrationInstanceConfigurationMutation = useUpdateIntegrationInstanceConfigurationMutation({
        onSuccess: () => handleSaveSuccess(integrationInstanceConfiguration?.id),
    });

    const updateIntegrationInstanceConfigurationCredentialsMutation =
        useUpdateIntegrationInstanceConfigurationCredentialsMutation({
            onSuccess: () => handleSaveSuccess(integrationInstanceConfiguration?.id),
        });

    const sharedParameterNames = useMemo(
        () => [
            ...Object.keys(integrationInstanceConfiguration?.connectionAuthorizationParameters ?? {}),
            ...Object.keys(integrationInstanceConfiguration?.connectionConnectionParameters ?? {}),
        ],
        [integrationInstanceConfiguration]
    );

    const enteredCredentialsParameters = useMemo(
        () =>
            Object.fromEntries(
                Object.entries(credentialsParameters ?? {}).filter(
                    ([, parameterValue]) =>
                        parameterValue != null && !(typeof parameterValue === 'string' && parameterValue.trim() === '')
                )
            ),
        [credentialsParameters]
    );

    const canUpdateCredentials =
        !!integrationInstanceConfiguration?.id &&
        integrationInstanceConfiguration.version != null &&
        sharedParameterNames.length > 0;

    const resetToConfiguration = (
        configuration: IntegrationInstanceConfiguration | undefined,
        updateVersion: boolean
    ) => {
        reset(getDefaultValues(configuration, updateVersion));

        resetWorkflowsEnabledStore();

        setCurIntegrationVersion(updateVersion ? undefined : configuration?.integrationVersion);

        initializedConfigKeyRef.current = '';
    };

    const handleBasicStepTabChange = (tab: ConfigurationDialogTabType) => {
        setBasicStepTab(tab);

        if (tab === 'new-configuration' && selectedExistingConfiguration) {
            setSelectedExistingConfiguration(undefined);

            resetToConfiguration(integrationInstanceConfigurationProp, updateIntegrationVersionProp);
        }
    };

    const handleExistingConfigurationSelect = (value: string) => {
        const existingConfiguration = integrationInstanceConfigurations?.find(
            (configuration) => String(configuration.id) === value
        );

        if (!existingConfiguration) {
            return;
        }

        setSelectedExistingConfiguration(existingConfiguration);

        resetToConfiguration(existingConfiguration, true);
    };

    const basicStepFields = (
        <IntegrationInstanceConfigurationDialogBasicStep
            authorizationRequired={authorizationRequired}
            authorizations={authorizations}
            control={control}
            curIntegrationId={curIntegrationId}
            curIntegrationVersion={curIntegrationVersion}
            getValues={getValues}
            integrationInstanceConfiguration={integrationInstanceConfiguration}
            setCurIntegrationId={setCurIntegrationId}
            setCurIntegrationVersion={setCurIntegrationVersion}
            setValue={setValue}
            updateIntegrationVersion={updateIntegrationVersion}
        />
    );

    const configurationItems = useMemo(
        () =>
            (integrationInstanceConfigurations ?? []).map((configuration) => ({
                label: `${configuration.name} - V${configuration.integrationVersion}`,
                value: String(configuration.id),
            })),
        [integrationInstanceConfigurations]
    );

    const tabbedBasicStepContent = basicStepTab ? (
        <ConfigurationDialogTabs
            activeTab={basicStepTab}
            changeVersionContent={basicStepFields}
            configurationItems={configurationItems}
            emptyTitle="No configurations for this integration yet"
            newConfigurationContent={basicStepFields}
            onConfigurationSelect={handleExistingConfigurationSelect}
            onTabChange={handleBasicStepTabChange}
            selectedConfigurationValue={
                selectedExistingConfiguration ? String(selectedExistingConfiguration.id) : undefined
            }
        />
    ) : (
        <div className="flex justify-center py-12">
            <LoadingDots />
        </div>
    );

    const basicStepContent = showTabs ? tabbedBasicStepContent : basicStepFields;

    const oauth2StepContent = oAuth2Authorization && integration && (
        <IntegrationInstanceConfigurationDialogOauth2Step
            componentName={integration.componentName}
            control={control}
            formState={formState}
            oAuth2Authorization={oAuth2Authorization}
            setUsePredefinedOAuthApp={setUsePredefinedOAuthApp}
            usePredefinedOAuthApp={usePredefinedOAuthApp}
        />
    );

    const workflowsStepContent = integration && workflows && (
        <IntegrationInstanceConfigurationDialogWorkflowsStep
            componentName={integration.componentName}
            control={control}
            formState={formState}
            setValue={setValue}
            workflows={workflows}
        />
    );

    const hasOauth2Step = !!oAuth2Authorization && !!integration && !updateIntegrationVersion;

    const hasWorkflowsStep = workflows === undefined || workflows.length > 0;

    const isBasicStepBlocked = showTabs && (!basicStepTab || (isChangeVersionTab && !selectedExistingConfiguration));

    const basicStepCanProceed =
        !isBasicStepBlocked &&
        !authorizationTypePending &&
        (hasOauth2Step || hasWorkflowsStep || !!connectionDefinition);

    const steps = useMemo<DialogStepI[]>(() => {
        const dialogSteps: DialogStepI[] = [{canProceed: basicStepCanProceed, id: 'basic', label: 'Basic'}];

        if (hasOauth2Step) {
            dialogSteps.push({id: 'oauth2', label: 'OAuth2 Connection'});
        }

        if (hasWorkflowsStep) {
            dialogSteps.push({canProceed: !isWorkflowsPending, id: 'workflows', label: 'Workflows'});
        }

        return dialogSteps;
    }, [basicStepCanProceed, hasOauth2Step, hasWorkflowsStep, isWorkflowsPending]);

    const closeDialog = () => {
        setIsOpen(false);
        setUsePredefinedOAuthApp(true);

        setTimeout(() => {
            reset(defaultValues);

            credentialsForm.reset({parameters: {}});

            setIsUpdatingCredentials(false);

            setBasicStepTab(undefined);
            setSelectedExistingConfiguration(undefined);

            if (onClose) {
                onClose();
            }

            resetWorkflowsEnabledStore();
        }, 300);
    };

    const handleSaveClick = (formData: IntegrationInstanceConfiguration) => {
        if (!formData) {
            return;
        }

        if (integrationInstanceConfiguration?.id) {
            updateIntegrationInstanceConfigurationMutation.mutate({
                ...integrationInstanceConfiguration,
                ...formData,
                integrationInstanceConfigurationWorkflows: formData.integrationInstanceConfigurationWorkflows?.map(
                    (integrationInstanceConfigurationWorkflow) => {
                        return {
                            ...integrationInstanceConfigurationWorkflow,
                            connections: integrationInstanceConfigurationWorkflow.enabled
                                ? integrationInstanceConfigurationWorkflow.connections
                                : [],
                            inputs: integrationInstanceConfigurationWorkflow.enabled
                                ? integrationInstanceConfigurationWorkflow.inputs
                                : {},
                        };
                    }
                ),
            } as IntegrationInstanceConfiguration);
        } else {
            createIntegrationInstanceConfigurationMutation.mutate({
                ...formData,
                authorizationType: toSavedAuthorizationType(
                    formData.authorizationType || getDefaultAuthorizationType(authorizations, authorizationRequired)
                ),
                environmentId: integrationInstanceConfigurationProp?.environmentId ?? currentEnvironmentId,
                integrationInstanceConfigurationWorkflows: formData.integrationInstanceConfigurationWorkflows?.map(
                    (integrationInstanceConfigurationWorkflow) => {
                        return {
                            ...integrationInstanceConfigurationWorkflow,
                            connections: integrationInstanceConfigurationWorkflow.enabled
                                ? integrationInstanceConfigurationWorkflow.connections
                                : [],
                            inputs: integrationInstanceConfigurationWorkflow.enabled
                                ? integrationInstanceConfigurationWorkflow.inputs
                                : {},
                        };
                    }
                ),
            });
        }
    };

    const handleSaveCredentialsClick = () => {
        updateIntegrationInstanceConfigurationCredentialsMutation.mutate({
            id: integrationInstanceConfiguration!.id!,
            updateConnectionCredentialsRequest: {
                parameters: enteredCredentialsParameters,
                version: integrationInstanceConfiguration!.version!,
            },
        });
    };

    const isSaving =
        !!createIntegrationInstanceConfigurationMutation.isPending ||
        !!updateIntegrationInstanceConfigurationMutation.isPending ||
        !!updateIntegrationInstanceConfigurationCredentialsMutation.isPending;

    const isWizard = !integrationInstanceConfiguration?.id || updateIntegrationVersion;

    const showsVersionUpdate = updateIntegrationVersion || isChangeVersionTab;

    const wizardTitle = showsVersionUpdate ? 'Change Integration Version' : 'New Configuration';

    const wizardDescription = showsVersionUpdate
        ? 'Move this configuration to another integration version.'
        : 'Run an integration version for connected users.';

    const validateStep = useCallback(() => form.trigger(undefined, {shouldFocus: true}), [form]);

    const handleCompleteClick = () => handleSubmit(handleSaveClick)();

    useEffect(() => {
        if (
            !showTabs ||
            !isOpen ||
            basicStepTab ||
            integrationInstanceConfigurationsIsFetching ||
            integrationInstanceConfigurations === undefined
        ) {
            return;
        }

        setBasicStepTab(integrationInstanceConfigurations.length > 0 ? 'change-version' : 'new-configuration');
    }, [
        basicStepTab,
        integrationInstanceConfigurations,
        integrationInstanceConfigurationsIsFetching,
        isOpen,
        showTabs,
    ]);

    useEffect(() => {
        if (!isChangeVersionTab || selectedExistingConfiguration || integrationInstanceConfigurations?.length !== 1) {
            return;
        }

        handleExistingConfigurationSelect(String(integrationInstanceConfigurations[0].id));

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [integrationInstanceConfigurations, isChangeVersionTab, selectedExistingConfiguration]);

    useEffect(() => {
        if (!isOpen || integrationInstanceConfiguration?.id || !integration?.name || getValues('name')) {
            return;
        }

        setValue('name', integration.name);
    }, [getValues, integration?.name, integrationInstanceConfiguration?.id, isOpen, setValue]);

    useEffect(() => {
        if (integrationInstanceConfiguration?.id || (authorizationRequired && !authorizations?.length)) {
            return;
        }

        const selectedAuthorizationTypeOffered =
            selectedAuthorizationType === NO_AUTHORIZATION_TYPE
                ? !authorizationRequired
                : !!authorizations?.some((authorization) => authorization.type === selectedAuthorizationType);

        if (
            defaultedAuthorizationsRef.current !== authorizations ||
            (!!selectedAuthorizationType && !selectedAuthorizationTypeOffered)
        ) {
            defaultedAuthorizationsRef.current = authorizations;

            setValue('authorizationType', getDefaultAuthorizationType(authorizations, authorizationRequired));
        }
    }, [
        authorizationRequired,
        authorizations,
        integrationInstanceConfiguration?.id,
        selectedAuthorizationType,
        setValue,
    ]);

    useEffect(() => {
        if (!workflows?.length) {
            return;
        }

        const filterKey = filterWorkflowUuids ? filterWorkflowUuids.join(',') : '';

        const configKey = `${curIntegrationId}-${curIntegrationVersion}-${filterKey}`;

        if (initializedConfigKeyRef.current === configKey) {
            return;
        }

        initializedConfigKeyRef.current = configKey;

        const integrationInstanceConfigurationWorkflows: IntegrationInstanceConfigurationWorkflow[] = workflows.map(
            (workflow) => {
                const integrationInstanceConfigurationWorkflow =
                    integrationInstanceConfiguration?.integrationInstanceConfigurationWorkflows?.find(
                        (configWorkflow) => configWorkflow.workflowUuid === workflow.workflowUuid
                    );

                setWorkflowEnabled(
                    workflow.id!,
                    !!(integrationInstanceConfigurationWorkflow && integrationInstanceConfigurationWorkflow.enabled)
                );

                const componentConnections: ComponentConnection[] = (workflow?.tasks ?? [])
                    .flatMap((task) => task.connections ?? [])
                    .concat((workflow?.triggers ?? []).flatMap((trigger) => trigger.connections ?? []))
                    .filter((connection) => connection.componentName !== integration?.componentName);

                const newIntegrationInstanceConfigurationWorkflowConnections: IntegrationInstanceConfigurationWorkflowConnection[] =
                    componentConnections.map((componentConnection) => {
                        const existingConnection = integrationInstanceConfigurationWorkflow?.connections?.find(
                            (workflowConnection) =>
                                workflowConnection.workflowNodeName === componentConnection.workflowNodeName &&
                                workflowConnection.workflowConnectionKey === componentConnection.key
                        );

                        return (
                            existingConnection ??
                            ({
                                key: componentConnection.key,
                                workflowNodeName: componentConnection.workflowNodeName,
                            } as IntegrationInstanceConfigurationWorkflowConnection)
                        );
                    });

                return {
                    ...(integrationInstanceConfigurationWorkflow ?? {}),
                    connections: newIntegrationInstanceConfigurationWorkflowConnections,
                    version: undefined,
                    workflowId: workflow.id!,
                    workflowUuid: workflow.workflowUuid,
                };
            }
        );

        setValue('integrationInstanceConfigurationWorkflows', integrationInstanceConfigurationWorkflows);

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [curIntegrationId, curIntegrationVersion, workflows]);

    useEffect(() => {
        const configKey = initializedConfigKeyRef.current;

        if (
            !configKey ||
            prefilledConfigKeyRef.current === configKey ||
            !workflows?.length ||
            !integration ||
            !workflowTestConfigurationConnections.isFetched
        ) {
            return;
        }

        prefilledConfigKeyRef.current = configKey;

        const integrationInstanceConfigurationWorkflows = getValues('integrationInstanceConfigurationWorkflows') ?? [];

        integrationInstanceConfigurationWorkflows.forEach((integrationInstanceConfigurationWorkflow, workflowIndex) => {
            const workflow = workflows.find(
                (workflow) => workflow.id === integrationInstanceConfigurationWorkflow.workflowId
            );

            if (!workflow) {
                return;
            }

            const componentConnections: ComponentConnection[] = (workflow.tasks ?? [])
                .flatMap((task) => task.connections ?? [])
                .concat((workflow.triggers ?? []).flatMap((trigger) => trigger.connections ?? []))
                .filter((connection) => connection.componentName !== integration.componentName);

            componentConnections.forEach((componentConnection, connectionIndex) => {
                if (integrationInstanceConfigurationWorkflow.connections?.[connectionIndex]?.connectionId != null) {
                    return;
                }

                const testConnectionId = workflowTestConfigurationConnections.findConnectionId(
                    workflow.workflowUuid,
                    componentConnection.workflowNodeName,
                    componentConnection.key
                );

                if (testConnectionId == null) {
                    return;
                }

                setValue(
                    `integrationInstanceConfigurationWorkflows.${workflowIndex}.connections.${connectionIndex}.connectionId`,
                    testConnectionId
                );
            });
        });
    }, [getValues, integration, setValue, workflowTestConfigurationConnections, workflows]);

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

            <DialogContent hasSidebar={isWizard} onInteractOutside={(event) => event.preventDefault()}>
                {isWizard ? (
                    <DialogWizard
                        description={wizardDescription}
                        footer={
                            <DialogFooter startContent={<DialogCancelButton />}>
                                <DialogPreviousButton className="lg:hidden" />

                                <DialogNextButton isPending={isSaving} label="Next" lastStepLabel="Save" />
                            </DialogFooter>
                        }
                        icon={<Settings2Icon />}
                        onComplete={handleCompleteClick}
                        steps={steps}
                        title={wizardTitle}
                        validateStep={validateStep}
                    >
                        <WorkflowMockProvider>
                            <Form {...form}>
                                <IntegrationInstanceConfigurationDialogStepContent
                                    basicStepContent={basicStepContent}
                                    isWorkflowsPending={isWorkflowsPending}
                                    oauth2StepContent={oauth2StepContent}
                                    workflowsStepContent={workflowsStepContent}
                                />
                            </Form>
                        </WorkflowMockProvider>
                    </DialogWizard>
                ) : (
                    <DialogMain>
                        <Form {...form}>
                            <DialogHeader title="Edit Configuration" />

                            <DialogBody>
                                {!isUpdatingCredentials && (
                                    <WorkflowMockProvider>{basicStepContent}</WorkflowMockProvider>
                                )}

                                {connectionDefinition && (
                                    <div className="py-4">
                                        <ConnectionParameters
                                            authorizationParameters={
                                                integrationInstanceConfiguration?.connectionAuthorizationParameters
                                            }
                                            authorizationType={integrationInstanceConfiguration?.authorizationType}
                                            connectionDefinition={connectionDefinition}
                                            connectionParameters={
                                                integrationInstanceConfiguration?.connectionConnectionParameters
                                            }
                                        />
                                    </div>
                                )}

                                {isUpdatingCredentials && connectionDefinition && (
                                    <IntegrationInstanceConfigurationDialogCredentials
                                        authorization={selectedAuthorization}
                                        connectionDefinition={connectionDefinition}
                                        control={credentialsForm.control as unknown as Control<FieldValues>}
                                        formState={credentialsForm.formState as unknown as FormState<FieldValues>}
                                        sharedParameterNames={sharedParameterNames}
                                    />
                                )}
                            </DialogBody>

                            <DialogFooter>
                                <DialogCancelButton />

                                {canUpdateCredentials && !isUpdatingCredentials && (
                                    <Button
                                        onClick={() => setIsUpdatingCredentials(true)}
                                        type="button"
                                        variant="outline"
                                    >
                                        <KeyRoundIcon /> Update credentials
                                    </Button>
                                )}

                                {isUpdatingCredentials && (
                                    <Button
                                        label="Back"
                                        onClick={() => setIsUpdatingCredentials(false)}
                                        type="button"
                                        variant="outline"
                                    />
                                )}

                                <Button
                                    disabled={
                                        isSaving ||
                                        authorizationTypePending ||
                                        (isUpdatingCredentials &&
                                            Object.keys(enteredCredentialsParameters).length === 0)
                                    }
                                    icon={isSaving ? <LoadingIcon /> : undefined}
                                    label="Save"
                                    onClick={
                                        isUpdatingCredentials
                                            ? credentialsForm.handleSubmit(handleSaveCredentialsClick)
                                            : handleSubmit(handleSaveClick)
                                    }
                                />
                            </DialogFooter>
                        </Form>
                    </DialogMain>
                )}
            </DialogContent>
        </Dialog>
    );
};

export default IntegrationInstanceConfigurationDialog;
