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
import {useWorkflowsEnabledStore} from '@/pages/embedded/integration-instance-configurations/stores/useWorkflowsEnabledStore';
import {EnvironmentModel, IntegrationInstanceConfigurationModel} from '@/shared/middleware/embedded/configuration';
import {
    useCreateIntegrationInstanceConfigurationMutation,
    useUpdateIntegrationInstanceConfigurationMutation,
} from '@/shared/mutations/embedded/integrationInstanceConfigurations.mutations';
import {IntegrationInstanceConfigurationTagKeys} from '@/shared/queries/embedded/integrationInstanceConfigurationTags.queries';
import {IntegrationInstanceConfigurationKeys} from '@/shared/queries/embedded/integrationInstanceConfigurations.queries';
import {useGetIntegrationVersionWorkflowsQuery} from '@/shared/queries/embedded/integrationWorkflows.queries';
import {IntegrationKeys} from '@/shared/queries/embedded/integrations.queries';
import {useQueryClient} from '@tanstack/react-query';
import {ReactNode, useState} from 'react';
import {useForm} from 'react-hook-form';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

import IntegrationInstanceConfigurationDialogBasicStep from './IntegrationInstanceConfigurationDialogBasicStep';
import IntegrationInstanceConfigurationDialogWorkflowsStep from './IntegrationInstanceConfigurationDialogWorkflowsStep';

interface IntegrationInstanceConfigurationDialogProps {
    onClose?: () => void;
    integrationInstanceConfiguration?: IntegrationInstanceConfigurationModel;
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
    const [isOpen, setIsOpen] = useState(!triggerNode);

    const [resetWorkflowsEnabledStore] = useWorkflowsEnabledStore(useShallow(({reset}) => [reset]));

    const form = useForm<IntegrationInstanceConfigurationModel>({
        defaultValues: {
            description: integrationInstanceConfiguration?.description || undefined,
            enabled: integrationInstanceConfiguration?.enabled || false,
            environment: integrationInstanceConfiguration?.environment || EnvironmentModel.Test,
            integrationId: integrationInstanceConfiguration?.integration?.id || undefined,
            integrationInstanceConfigurationWorkflows: [],
            integrationVersion: integrationInstanceConfiguration?.integrationVersion || undefined,
            tags:
                integrationInstanceConfiguration?.tags?.map((tag) => ({
                    ...tag,
                    label: tag.name,
                })) || [],
        },
    });

    const {control, formState, getValues, handleSubmit, reset, setValue} = form;

    const {data: workflows} = useGetIntegrationVersionWorkflowsQuery(
        getValues().integrationId!,
        getValues().integrationVersion!,
        !!getValues().integrationId && !!getValues().integrationVersion
    );

    const queryClient = useQueryClient();

    const onSuccess = () => {
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

    const integrationInstanceConfigurationDialogSteps = [
        {
            content: (
                <IntegrationInstanceConfigurationDialogBasicStep
                    control={control}
                    getValues={getValues}
                    integrationInstanceConfiguration={integrationInstanceConfiguration}
                    setValue={setValue}
                />
            ),
            name: 'Basic',
        },
        {
            content: workflows && (
                <IntegrationInstanceConfigurationDialogWorkflowsStep
                    control={control}
                    formState={formState}
                    setValue={setValue}
                    workflows={workflows}
                />
            ),
            name: 'Workflows',
        },
    ];

    function closeDialog() {
        setIsOpen(false);

        setTimeout(() => {
            reset({
                environment: EnvironmentModel.Test,
                integrationInstanceConfigurationWorkflows: [],
            });

            setActiveStepIndex(0);

            if (onClose) {
                onClose();
            }

            resetWorkflowsEnabledStore();
        }, 300);
    }

    function saveIntegrationInstanceConfiguration(formData: IntegrationInstanceConfigurationModel) {
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
            } as IntegrationInstanceConfigurationModel);
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
    }

    function handleNextClick() {
        setActiveStepIndex(activeStepIndex + 1);
    }

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
                                ? 'Upgrade Integration Configuration Version'
                                : `${integrationInstanceConfiguration?.id ? 'Edit' : 'New'} Instance Configuration ${!integrationInstanceConfiguration?.id ? '-' : ''} ${
                                      !integrationInstanceConfiguration?.id
                                          ? integrationInstanceConfigurationDialogSteps[activeStepIndex].name
                                          : ''
                                  }`}
                        </DialogTitle>

                        {!integrationInstanceConfiguration?.id && workflows && workflows.length > 0 && (
                            <nav aria-label="Progress">
                                <ol className="space-y-4 md:flex md:space-y-0" role="list">
                                    {integrationInstanceConfigurationDialogSteps.map((step, index) => (
                                        <li className="md:flex-1" key={step.name}>
                                            <div
                                                className={twMerge(
                                                    'group flex flex-col border-l-4 py-2 pl-4 md:border-l-0 md:border-t-4 md:pb-0 md:pl-0 md:pt-4',
                                                    index <= activeStepIndex
                                                        ? 'border-gray-900 hover:border-gray-800'
                                                        : 'border-gray-200 hover:border-gray-30'
                                                )}
                                            ></div>
                                        </li>
                                    ))}
                                </ol>
                            </nav>
                        )}
                    </DialogHeader>

                    <div className={twMerge(activeStepIndex === 1 && 'max-h-[600px] overflow-y-auto')}>
                        {integrationInstanceConfigurationDialogSteps[activeStepIndex].content}
                    </div>

                    <DialogFooter>
                        {activeStepIndex === 0 && (
                            <>
                                <DialogClose asChild>
                                    <Button variant="outline">Cancel</Button>
                                </DialogClose>

                                {(!integrationInstanceConfiguration?.id || updateIntegrationVersion) &&
                                    workflows &&
                                    workflows.length > 0 && (
                                        <Button onClick={handleSubmit(handleNextClick)}>Next</Button>
                                    )}
                            </>
                        )}

                        {(activeStepIndex === 1 ||
                            !workflows ||
                            workflows?.length == 0 ||
                            (integrationInstanceConfiguration?.id && !updateIntegrationVersion)) && (
                            <>
                                {activeStepIndex === 1 && (
                                    <Button onClick={() => setActiveStepIndex(activeStepIndex - 1)} variant="outline">
                                        Previous
                                    </Button>
                                )}

                                <Button onClick={handleSubmit(saveIntegrationInstanceConfiguration)}>Save</Button>
                            </>
                        )}
                    </DialogFooter>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default IntegrationInstanceConfigurationDialog;
