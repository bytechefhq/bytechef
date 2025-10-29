import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import {
    Dialog,
    DialogClose,
    DialogCloseButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from '@/components/ui/dialog';
import {Form} from '@/components/ui/form';
import IntegrationInstanceConfigurationDialogOauth2Step from '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialogOauth2Step';
import {useWorkflowsEnabledStore} from '@/ee/pages/embedded/integration-instance-configurations/stores/useWorkflowsEnabledStore';
import {
    ComponentConnection,
    IntegrationInstanceConfiguration,
    IntegrationInstanceConfigurationWorkflow,
    IntegrationInstanceConfigurationWorkflowConnection,
} from '@/ee/shared/middleware/embedded/configuration';
import {
    useCreateIntegrationInstanceConfigurationMutation,
    useUpdateIntegrationInstanceConfigurationMutation,
} from '@/ee/shared/mutations/embedded/integrationInstanceConfigurations.mutations';
import {IntegrationInstanceConfigurationTagKeys} from '@/ee/shared/queries/embedded/integrationInstanceConfigurationTags.queries';
import {IntegrationInstanceConfigurationKeys} from '@/ee/shared/queries/embedded/integrationInstanceConfigurations.queries';
import {useGetIntegrationVersionWorkflowsQuery} from '@/ee/shared/queries/embedded/integrationWorkflows.queries';
import {IntegrationKeys, useGetIntegrationQuery} from '@/ee/shared/queries/embedded/integrations.queries';
import {WorkflowMockProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import ConnectionParameters from '@/shared/components/connection/ConnectionParameters';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {AuthorizationType} from '@/shared/middleware/platform/configuration';
import {useGetConnectionDefinitionQuery} from '@/shared/queries/platform/connectionDefinitions.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {ReactNode, useEffect, useState} from 'react';
import {useForm} from 'react-hook-form';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

import IntegrationInstanceConfigurationDialogBasicStep from './IntegrationInstanceConfigurationDialogBasicStep';
import IntegrationInstanceConfigurationDialogWorkflowsStep from './IntegrationInstanceConfigurationDialogWorkflowsStep';

interface IntegrationInstanceConfigurationDialogProps {
    onClose?: () => void;
    integrationInstanceConfiguration?: IntegrationInstanceConfiguration;
    triggerNode?: ReactNode;
    updateIntegrationVersion?: boolean;
}

const IntegrationInstanceConfigurationDialog = ({
    integrationInstanceConfiguration,
    onClose,
    triggerNode,
    updateIntegrationVersion = false,
}: IntegrationInstanceConfigurationDialogProps) => {
    const [activeStepIndex, setActiveStepIndex] = useState(0);
    const [curIntegrationId, setCurIntegrationId] = useState(integrationInstanceConfiguration?.integrationId);
    const [curIntegrationVersion, setCurIntegrationVersion] = useState<number | undefined>(
        integrationInstanceConfiguration?.integrationVersion
    );
    const [isOpen, setIsOpen] = useState(!triggerNode);
    const [usePredefinedOAuthApp, setUsePredefinedOAuthApp] = useState(true);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const [resetWorkflowsEnabledStore, setWorkflowEnabled] = useWorkflowsEnabledStore(
        useShallow(({reset, setWorkflowEnabled}) => [reset, setWorkflowEnabled])
    );

    const {captureIntegrationInstanceConfigurationCreated} = useAnalytics();

    const form = useForm<IntegrationInstanceConfiguration>({
        defaultValues: {
            description: integrationInstanceConfiguration?.description || undefined,
            enabled: integrationInstanceConfiguration?.enabled || false,
            integrationId: integrationInstanceConfiguration?.integrationId || undefined,
            integrationInstanceConfigurationWorkflows: [],
            integrationVersion: integrationInstanceConfiguration?.integrationVersion || undefined,
            name: integrationInstanceConfiguration?.name || undefined,
            tags:
                integrationInstanceConfiguration?.tags?.map((tag) => ({
                    ...tag,
                    label: tag.name,
                })) || [],
        },
    });

    const {control, formState, getValues, handleSubmit, reset, setValue} = form;

    const {data: workflows} = useGetIntegrationVersionWorkflowsQuery(
        curIntegrationId!,
        curIntegrationVersion!,
        true,
        !!curIntegrationId && !!curIntegrationVersion
    );

    const {data: integration} = useGetIntegrationQuery(curIntegrationId!, undefined, !!curIntegrationId);

    const {data: connectionDefinition} = useGetConnectionDefinitionQuery({
        componentName: integration?.componentName as string,
    });

    const oAuth2Authorization = connectionDefinition?.authorizations?.find(
        (authorization) =>
            authorization.type === AuthorizationType.Oauth2AuthorizationCode ||
            authorization.type === AuthorizationType.Oauth2AuthorizationCodePkce ||
            authorization.type === AuthorizationType.Oauth2ImplicitCode
    );

    const queryClient = useQueryClient();

    const onSuccess = () => {
        if (!integrationInstanceConfiguration?.id) {
            captureIntegrationInstanceConfigurationCreated();
        }

        queryClient.invalidateQueries({
            queryKey: IntegrationInstanceConfigurationKeys.integrationInstanceConfigurations,
        });
        queryClient.invalidateQueries({
            queryKey: IntegrationInstanceConfigurationTagKeys.integrationInstanceConfigurationTags,
        });
        queryClient.invalidateQueries({
            queryKey: IntegrationKeys.filteredIntegrations({}),
        });

        closeDialog();
        setActiveStepIndex(0);
    };

    const createIntegrationInstanceConfigurationMutation = useCreateIntegrationInstanceConfigurationMutation({
        onSuccess,
    });

    const updateIntegrationInstanceConfigurationMutation = useUpdateIntegrationInstanceConfigurationMutation({
        onSuccess,
    });

    let integrationInstanceConfigurationDialogSteps = [
        {
            content: (
                <IntegrationInstanceConfigurationDialogBasicStep
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
            ),
            name: 'Basic',
        },
    ];

    if (oAuth2Authorization && integration && !updateIntegrationVersion) {
        integrationInstanceConfigurationDialogSteps = [
            ...integrationInstanceConfigurationDialogSteps,
            {
                content: (
                    <IntegrationInstanceConfigurationDialogOauth2Step
                        componentName={integration.componentName}
                        control={control}
                        formState={formState}
                        oAuth2Authorization={oAuth2Authorization}
                        setUsePredefinedOAuthApp={setUsePredefinedOAuthApp}
                        usePredefinedOAuthApp={usePredefinedOAuthApp}
                    />
                ),
                name: 'OAuth2 Connection',
            },
        ];
    }

    if (integration && workflows && workflows.length > 0) {
        integrationInstanceConfigurationDialogSteps = [
            ...integrationInstanceConfigurationDialogSteps,
            {
                content: (
                    <IntegrationInstanceConfigurationDialogWorkflowsStep
                        componentName={integration.componentName}
                        control={control}
                        formState={formState}
                        setValue={setValue}
                        workflows={workflows}
                    />
                ),
                name: 'Workflows',
            },
        ];
    }

    const closeDialog = () => {
        setIsOpen(false);
        setUsePredefinedOAuthApp(true);

        setTimeout(() => {
            reset({
                integrationInstanceConfigurationWorkflows: [],
            });

            setActiveStepIndex(0);

            if (onClose) {
                onClose();
            }

            resetWorkflowsEnabledStore();
        }, 300);
    };

    const handleNextClick = () => {
        setActiveStepIndex(activeStepIndex + 1);
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
                authorizationType: oAuth2Authorization?.type || connectionDefinition?.authorizations?.[0].type,
                environmentId: currentEnvironmentId,
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

    const isSaving =
        !!createIntegrationInstanceConfigurationMutation.isPending ||
        !!updateIntegrationInstanceConfigurationMutation.isPending;

    useEffect(() => {
        if (workflows) {
            let integrationInstanceConfigurationWorkflows: IntegrationInstanceConfigurationWorkflow[] = [];

            for (let i = 0; i < workflows.length; i++) {
                const workflow = workflows[i];

                const integrationInstanceConfigurationWorkflow =
                    integrationInstanceConfiguration?.integrationInstanceConfigurationWorkflows?.find(
                        (integrationInstanceConfigurationWorkflow) =>
                            integrationInstanceConfigurationWorkflow.workflowUuid === workflow.workflowUuid
                    );

                if (integrationInstanceConfigurationWorkflow && integrationInstanceConfigurationWorkflow.enabled) {
                    setWorkflowEnabled(workflow.id!, true);
                } else {
                    setWorkflowEnabled(workflow.id!, false);
                }

                let newIntegrationInstanceConfigurationWorkflowConnections: IntegrationInstanceConfigurationWorkflowConnection[] =
                    [];

                const componentConnections: ComponentConnection[] = (workflow?.tasks ?? [])
                    .flatMap((task) => task.connections ?? [])
                    .concat((workflow?.triggers ?? []).flatMap((trigger) => trigger.connections ?? []))
                    .filter((connection) => connection.componentName !== integration?.componentName);

                for (const componentConnection of componentConnections) {
                    const integrationInstanceConfigurationWorkflowConnection =
                        integrationInstanceConfigurationWorkflow?.connections?.find(
                            (integrationInstanceConfigurationWorkflowConnection) =>
                                integrationInstanceConfigurationWorkflowConnection.workflowNodeName ===
                                    componentConnection.workflowNodeName &&
                                integrationInstanceConfigurationWorkflowConnection.workflowConnectionKey ===
                                    componentConnection.key
                        );

                    newIntegrationInstanceConfigurationWorkflowConnections = [
                        ...newIntegrationInstanceConfigurationWorkflowConnections,
                        integrationInstanceConfigurationWorkflowConnection ??
                            ({
                                key: componentConnection.key,
                                workflowNodeName: componentConnection.workflowNodeName,
                            } as IntegrationInstanceConfigurationWorkflowConnection),
                    ];
                }

                integrationInstanceConfigurationWorkflows = [
                    ...integrationInstanceConfigurationWorkflows,
                    {
                        ...(integrationInstanceConfigurationWorkflow ?? {}),
                        connections: newIntegrationInstanceConfigurationWorkflowConnections,
                        version: undefined,
                        workflowId: workflow.id!,
                    },
                ];
            }

            setValue('integrationInstanceConfigurationWorkflows', integrationInstanceConfigurationWorkflows);
        }

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [getValues().integrationId, getValues().integrationVersion, workflows]);

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

            <DialogContent className={twMerge('flex flex-col')} onInteractOutside={(event) => event.preventDefault()}>
                <Form {...form}>
                    <DialogHeader className="flex flex-row items-center justify-between gap-1 space-y-0">
                        <div className="flex w-full flex-col space-y-1">
                            <DialogTitle>
                                {updateIntegrationVersion
                                    ? 'Upgrade Integration Version'
                                    : `${integrationInstanceConfiguration?.id ? 'Edit' : 'New'} Instance Configuration ${!integrationInstanceConfiguration?.id ? '-' : ''} ${
                                          !integrationInstanceConfiguration?.id
                                              ? integrationInstanceConfigurationDialogSteps[activeStepIndex].name
                                              : ''
                                      }`}
                            </DialogTitle>

                            {!integrationInstanceConfiguration?.id &&
                                ((workflows && workflows.length > 0) || oAuth2Authorization) && (
                                    <nav aria-label="Progress">
                                        <ol className="space-y-4 md:flex md:space-y-0" role="list">
                                            {integrationInstanceConfigurationDialogSteps.map((step, index) => (
                                                <li className="md:flex-1" key={step.name}>
                                                    <div
                                                        className={twMerge(
                                                            'group flex flex-col border-l-4 py-2 pl-4 md:border-l-0 md:border-t-4 md:pb-0 md:pl-0',
                                                            index <= activeStepIndex
                                                                ? 'border-gray-900 hover:border-gray-800'
                                                                : 'hover:border-gray-30 border-gray-200'
                                                        )}
                                                    ></div>
                                                </li>
                                            ))}
                                        </ol>
                                    </nav>
                                )}
                        </div>

                        <DialogCloseButton />
                    </DialogHeader>

                    <WorkflowMockProvider>
                        <div
                            className={twMerge(
                                ((activeStepIndex === 1 && !oAuth2Authorization) ||
                                    (activeStepIndex === 1 && oAuth2Authorization && updateIntegrationVersion) ||
                                    (activeStepIndex === 2 && oAuth2Authorization)) &&
                                    'max-h-integration-instance-configuration-dialog-height overflow-y-auto'
                            )}
                        >
                            {integrationInstanceConfigurationDialogSteps[activeStepIndex].content}
                        </div>
                    </WorkflowMockProvider>

                    {integrationInstanceConfiguration?.id && connectionDefinition && !updateIntegrationVersion && (
                        <div className="py-4">
                            <ConnectionParameters
                                authorizationParameters={
                                    integrationInstanceConfiguration.connectionAuthorizationParameters
                                }
                                connectionDefinition={connectionDefinition}
                                connectionParameters={integrationInstanceConfiguration.connectionConnectionParameters}
                            />
                        </div>
                    )}

                    <DialogFooter>
                        {activeStepIndex === 0 && (
                            <>
                                <DialogClose asChild>
                                    <Button label="Cancel" variant="outline" />
                                </DialogClose>

                                {(((!integrationInstanceConfiguration?.id ||
                                    (workflows && workflows.length > 0 && updateIntegrationVersion)) &&
                                    oAuth2Authorization) ||
                                    (workflows && workflows.length > 0 && !oAuth2Authorization)) && (
                                    <Button label="Next" onClick={handleSubmit(handleNextClick)} />
                                )}

                                {(((!workflows || workflows?.length == 0) && !oAuth2Authorization) ||
                                    (integrationInstanceConfiguration?.id && !updateIntegrationVersion) ||
                                    (workflows && workflows.length === 0 && updateIntegrationVersion)) && (
                                    <Button
                                        disabled={isSaving}
                                        icon={isSaving ? <LoadingIcon /> : undefined}
                                        label="Save"
                                        onClick={handleSubmit(handleSaveClick)}
                                    />
                                )}
                            </>
                        )}

                        {activeStepIndex === 1 && oAuth2Authorization && !updateIntegrationVersion && (
                            <>
                                <Button
                                    label="Previous"
                                    onClick={() => setActiveStepIndex(activeStepIndex - 1)}
                                    variant="outline"
                                />

                                {workflows && workflows?.length > 0 && (
                                    <Button label="Next" onClick={handleSubmit(handleNextClick)} />
                                )}

                                {!workflows ||
                                    (workflows?.length === 0 && (
                                        <Button
                                            disabled={isSaving}
                                            icon={isSaving ? <LoadingIcon /> : undefined}
                                            label="Save"
                                            onClick={handleSubmit(handleSaveClick)}
                                        />
                                    ))}
                            </>
                        )}

                        {((activeStepIndex === 1 && !oAuth2Authorization) ||
                            (activeStepIndex === 1 && oAuth2Authorization && updateIntegrationVersion) ||
                            (activeStepIndex === 2 && oAuth2Authorization)) &&
                            workflows &&
                            workflows?.length > 0 && (
                                <>
                                    <Button
                                        label="Previous"
                                        onClick={() => setActiveStepIndex(activeStepIndex - 1)}
                                        variant="outline"
                                    />

                                    <Button
                                        disabled={isSaving}
                                        icon={isSaving ? <LoadingIcon /> : undefined}
                                        label="Save"
                                        onClick={handleSubmit(handleSaveClick)}
                                    />
                                </>
                            )}
                    </DialogFooter>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default IntegrationInstanceConfigurationDialog;
