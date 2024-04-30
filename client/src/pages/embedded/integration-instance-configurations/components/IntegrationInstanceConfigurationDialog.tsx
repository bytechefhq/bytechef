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
import {EnvironmentModel, IntegrationInstanceConfigurationModel} from '@/middleware/embedded/configuration';
import {
    useCreateIntegrationInstanceConfigurationMutation,
    useUpdateIntegrationInstanceConfigurationMutation,
} from '@/mutations/embedded/integrationInstanceConfigurations.mutations';
import {IntegrationInstanceConfigurationTagKeys} from '@/queries/embedded/integrationInstanceConfigurationTags.queries';
import {IntegrationInstanceConfigurationKeys} from '@/queries/embedded/integrationInstanceConfigurations.queries';
import {IntegrationKeys} from '@/queries/embedded/integrations.queries';
import {Cross2Icon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {MouseEvent, ReactNode, useState} from 'react';
import {useForm} from 'react-hook-form';
import {twMerge} from 'tailwind-merge';

import IntegrationInstanceConfigurationDialogBasicStep from './IntegrationInstanceConfigurationDialogBasicStep';
import IntegrationInstanceConfigurationDialogWorkflowsStep from './IntegrationInstanceConfigurationDialogWorkflowsStep';

interface IntegrationInstanceConfigurationDialogProps {
    onClose?: () => void;
    integrationInstanceConfiguration?: IntegrationInstanceConfigurationModel;
    triggerNode?: ReactNode;
}

const IntegrationInstanceConfigurationDialog = ({
    integrationInstanceConfiguration,
    onClose,
    triggerNode,
}: IntegrationInstanceConfigurationDialogProps) => {
    const [activeStepIndex, setActiveStepIndex] = useState(0);
    const [isOpen, setIsOpen] = useState(!triggerNode);
    const [integrationId, setIntegrationId] = useState<number | undefined>(
        integrationInstanceConfiguration?.integrationId
    );

    const form = useForm<IntegrationInstanceConfigurationModel>({
        defaultValues: {
            enabled: integrationInstanceConfiguration?.enabled || false,
            environment: integrationInstanceConfiguration?.environment || EnvironmentModel.Test,
            integration: integrationInstanceConfiguration?.integration || null,
            integrationId: integrationInstanceConfiguration?.id || undefined,
            integrationInstanceConfigurationWorkflows: [],
            tags:
                integrationInstanceConfiguration?.tags?.map((tag) => ({
                    ...tag,
                    label: tag.name,
                })) || [],
        } as IntegrationInstanceConfigurationModel,
    });

    const {control, formState, getValues, handleSubmit, register, reset, setValue, trigger} = form;

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
                    errors={formState.errors}
                    getValues={getValues}
                    integrationId={integrationId}
                    integrationInstanceConfiguration={integrationInstanceConfiguration}
                    register={register}
                    setIntegrationId={setIntegrationId}
                    setValue={setValue}
                    touchedFields={formState.touchedFields}
                />
            ),
            name: 'Basic',
        },
        {
            content: (
                <IntegrationInstanceConfigurationDialogWorkflowsStep
                    control={control}
                    formState={formState}
                    getValues={getValues}
                    register={register}
                    setValue={setValue}
                />
            ),
            name: 'Workflows',
        },
    ];

    function closeDialog() {
        setIsOpen(false);

        setTimeout(() => {
            reset();

            setActiveStepIndex(0);

            if (onClose) {
                onClose();
            }
        }, 300);
    }

    function saveIntegrationInstanceConfiguration() {
        let formData = getValues();

        if (!formData) {
            return;
        }

        formData = {
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
        };

        if (integrationInstanceConfiguration?.id) {
            updateIntegrationInstanceConfigurationMutation.mutate({
                ...integrationInstanceConfiguration,
                ...formData,
                integrationId: formData?.integration?.id || 0,
            } as IntegrationInstanceConfigurationModel);
        } else {
            createIntegrationInstanceConfigurationMutation.mutate({
                integrationInstanceConfigurationModel: formData,
            });
        }
    }

    function handleNextClick(event: MouseEvent) {
        trigger();

        if (!formState.isValid) {
            event.preventDefault();
        } else {
            setActiveStepIndex(activeStepIndex + 1);
        }
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
                        <div className="flex items-center justify-between">
                            <DialogTitle>
                                {`${integrationInstanceConfiguration?.id ? 'Edit' : 'New'} Instance Configuration ${!integrationInstanceConfiguration?.id ? '-' : ''} ${
                                    !integrationInstanceConfiguration?.id
                                        ? integrationInstanceConfigurationDialogSteps[activeStepIndex].name
                                        : ''
                                }`}
                            </DialogTitle>

                            <DialogClose asChild>
                                <Cross2Icon className="size-4 cursor-pointer opacity-70" />
                            </DialogClose>
                        </div>

                        {!integrationInstanceConfiguration?.id && (
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

                                {!integrationInstanceConfiguration?.id &&
                                    integrationInstanceConfiguration?.integrationInstanceConfigurationWorkflows &&
                                    integrationInstanceConfiguration?.integrationInstanceConfigurationWorkflows.length >
                                        0 && <Button onClick={handleNextClick}>Next</Button>}

                                {!integrationInstanceConfiguration?.integrationInstanceConfigurationWorkflows && (
                                    <Button
                                        disabled={
                                            integrationInstanceConfiguration?.enabled &&
                                            !integrationInstanceConfiguration?.id
                                        }
                                        onClick={handleSubmit(saveIntegrationInstanceConfiguration)}
                                    >
                                        Save
                                    </Button>
                                )}
                            </>
                        )}

                        {(activeStepIndex === 1 || integrationInstanceConfiguration?.id) && (
                            <>
                                {!integrationInstanceConfiguration?.id && (
                                    <Button onClick={() => setActiveStepIndex(activeStepIndex - 1)} variant="outline">
                                        Previous
                                    </Button>
                                )}

                                <Button
                                    disabled={
                                        integrationInstanceConfiguration?.enabled &&
                                        !integrationInstanceConfiguration?.id
                                    }
                                    onClick={handleSubmit(saveIntegrationInstanceConfiguration)}
                                >
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
