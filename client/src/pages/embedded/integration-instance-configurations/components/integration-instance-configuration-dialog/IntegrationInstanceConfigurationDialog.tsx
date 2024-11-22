import LoadingIcon from '@/components/LoadingIcon';
import {Button} from '@/components/ui/button';
import {
    Dialog,
    DialogClose,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from '@/components/ui/dialog';
import {Form} from '@/components/ui/form';
import IntegrationInstanceConfigurationDialogOauth2Step from '@/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialogOauth2Step';
import {useWorkflowsEnabledStore} from '@/pages/embedded/integration-instance-configurations/stores/useWorkflowsEnabledStore';
import ConnectionParameters from '@/pages/platform/connection/components/ConnectionParameters';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {
    Environment,
    IntegrationInstanceConfiguration,
    IntegrationInstanceConfigurationWorkflow,
    IntegrationInstanceConfigurationWorkflowConnection,
    WorkflowConnection,
} from '@/shared/middleware/embedded/configuration';
import {AuthorizationType} from '@/shared/middleware/platform/configuration';
import {
    useCreateIntegrationInstanceConfigurationMutation,
    useUpdateIntegrationInstanceConfigurationMutation,
} from '@/shared/mutations/embedded/integrationInstanceConfigurations.mutations';
import {IntegrationInstanceConfigurationTagKeys} from '@/shared/queries/embedded/integrationInstanceConfigurationTags.queries';
import {IntegrationInstanceConfigurationKeys} from '@/shared/queries/embedded/integrationInstanceConfigurations.queries';
import {useGetIntegrationVersionWorkflowsQuery} from '@/shared/queries/embedded/integrationWorkflows.queries';
import {IntegrationKeys, useGetIntegrationQuery} from '@/shared/queries/embedded/integrations.queries';
import {useGetConnectionDefinitionQuery} from '@/shared/queries/platform/connectionDefinitions.queries';
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

    const [resetWorkflowsEnabledStore, setWorkflowEnabled] = useWorkflowsEnabledStore(
        useShallow(({reset, setWorkflowEnabled}) => [reset, setWorkflowEnabled])
    );

    const {captureIntegrationInstanceConfigurationCreated} = useAnalytics();

    const form = useForm<IntegrationInstanceConfiguration>({
        defaultValues: {
            description: integrationInstanceConfiguration?.description || undefined,
            enabled: integrationInstanceConfiguration?.enabled || false,
            environment: integrationInstanceConfiguration?.environment || Environment.Test,
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
        !!curIntegrationId && !!curIntegrationVersion
    );

    const {data: integration} = useGetIntegrationQuery(curIntegrationId!, undefined, !!curIntegrationId);

    const {data: connectionDefinition} = useGetConnectionDefinitionQuery({
        componentName: integration?.componentName as string,
        componentVersion: 1,
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
                environment: Environment.Test,
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

    useEffect(() => {
        if (workflows) {
            let integrationInstanceConfigurationWorkflows: IntegrationInstanceConfigurationWorkflow[] = [];

            for (let i = 0; i < workflows.length; i++) {
                const workflow = workflows[i];

                const integrationInstanceConfigurationWorkflow =
                    integrationInstanceConfiguration?.integrationInstanceConfigurationWorkflows?.find(
                        (integrationInstanceConfigurationWorkflow) =>
                            integrationInstanceConfigurationWorkflow.workflowReferenceCode ===
                            workflow.workflowReferenceCode
                    );

                if (integrationInstanceConfigurationWorkflow && integrationInstanceConfigurationWorkflow.enabled) {
                    setWorkflowEnabled(workflow.id!, true);
                } else {
                    setWorkflowEnabled(workflow.id!, false);
                }

                let newIntegrationInstanceConfigurationWorkflowConnections: IntegrationInstanceConfigurationWorkflowConnection[] =
                    [];

                const workflowConnections: WorkflowConnection[] = (workflow?.tasks ?? [])
                    .flatMap((task) => task.connections ?? [])
                    .concat((workflow?.triggers ?? []).flatMap((trigger) => trigger.connections ?? []))
                    .filter((connection) => connection.componentName !== integration?.componentName);

                for (const workflowConnection of workflowConnections) {
                    const integrationInstanceConfigurationWorkflowConnection =
                        integrationInstanceConfigurationWorkflow?.connections?.find(
                            (integrationInstanceConfigurationWorkflowConnection) =>
                                integrationInstanceConfigurationWorkflowConnection.workflowNodeName ===
                                    workflowConnection.workflowNodeName &&
                                integrationInstanceConfigurationWorkflowConnection.key === workflowConnection.key
                        );

                    newIntegrationInstanceConfigurationWorkflowConnections = [
                        ...newIntegrationInstanceConfigurationWorkflowConnections,
                        integrationInstanceConfigurationWorkflowConnection ??
                            ({
                                key: workflowConnection.key,
                                workflowNodeName: workflowConnection.workflowNodeName,
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
                    <DialogHeader>
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
                                                        'group flex flex-col border-l-4 py-2 pl-4 md:border-l-0 md:border-t-4 md:pb-0 md:pl-0 md:pt-4',
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
                    </DialogHeader>

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
                                    <Button variant="outline">Cancel</Button>
                                </DialogClose>

                                {(!integrationInstanceConfiguration?.id ||
                                    (workflows && workflows.length > 0 && updateIntegrationVersion)) &&
                                    oAuth2Authorization && (
                                        <Button onClick={handleSubmit(handleNextClick)}>Next</Button>
                                    )}

                                {(((!workflows || workflows?.length == 0) && !oAuth2Authorization) ||
                                    (integrationInstanceConfiguration?.id && !updateIntegrationVersion) ||
                                    (workflows && workflows.length === 0 && updateIntegrationVersion)) && (
                                    <Button
                                        disabled={
                                            createIntegrationInstanceConfigurationMutation.isPending ||
                                            updateIntegrationInstanceConfigurationMutation.isPending
                                        }
                                        onClick={handleSubmit(handleSaveClick)}
                                    >
                                        {createIntegrationInstanceConfigurationMutation.isPending ||
                                            (updateIntegrationInstanceConfigurationMutation.isPending && (
                                                <LoadingIcon />
                                            ))}
                                        Save
                                    </Button>
                                )}
                            </>
                        )}

                        {activeStepIndex === 1 && oAuth2Authorization && !updateIntegrationVersion && (
                            <>
                                <Button onClick={() => setActiveStepIndex(activeStepIndex - 1)} variant="outline">
                                    Previous
                                </Button>

                                {workflows && workflows?.length > 0 && (
                                    <Button onClick={handleSubmit(handleNextClick)}>Next</Button>
                                )}

                                {!workflows ||
                                    (workflows?.length === 0 && (
                                        <Button
                                            disabled={
                                                createIntegrationInstanceConfigurationMutation.isPending ||
                                                updateIntegrationInstanceConfigurationMutation.isPending
                                            }
                                            onClick={handleSubmit(handleSaveClick)}
                                        >
                                            {createIntegrationInstanceConfigurationMutation.isPending ||
                                                (updateIntegrationInstanceConfigurationMutation.isPending && (
                                                    <LoadingIcon />
                                                ))}
                                            Save
                                        </Button>
                                    ))}
                            </>
                        )}

                        {((activeStepIndex === 1 && !oAuth2Authorization) ||
                            (activeStepIndex === 1 && oAuth2Authorization && updateIntegrationVersion) ||
                            (activeStepIndex === 2 && oAuth2Authorization)) &&
                            workflows &&
                            workflows?.length > 0 && (
                                <>
                                    <Button onClick={() => setActiveStepIndex(activeStepIndex - 1)} variant="outline">
                                        Previous
                                    </Button>

                                    <Button
                                        disabled={
                                            createIntegrationInstanceConfigurationMutation.isPending ||
                                            updateIntegrationInstanceConfigurationMutation.isPending
                                        }
                                        onClick={handleSubmit(handleSaveClick)}
                                    >
                                        {createIntegrationInstanceConfigurationMutation.isPending ||
                                            (updateIntegrationInstanceConfigurationMutation.isPending && (
                                                <LoadingIcon />
                                            ))}
                                        Save
                                    </Button>
                                </>
                            )}
                    </DialogFooter>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default IntegrationInstanceConfigurationDialog;
