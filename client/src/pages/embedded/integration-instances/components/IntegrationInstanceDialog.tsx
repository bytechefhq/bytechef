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
import {IntegrationInstanceModel} from '@/middleware/embedded/configuration';
import {Cross2Icon} from '@radix-ui/react-icons';
import {MouseEvent, ReactNode, useState} from 'react';
import {useForm} from 'react-hook-form';
import {twMerge} from 'tailwind-merge';

interface IntegrationInstanceDialogProps {
    onClose?: () => void;
    integrationInstance?: IntegrationInstanceModel;
    triggerNode?: ReactNode;
}

const IntegrationInstanceDialog = ({integrationInstance, onClose, triggerNode}: IntegrationInstanceDialogProps) => {
    const [activeStepIndex, setActiveStepIndex] = useState(0);
    const [isOpen, setIsOpen] = useState(!triggerNode);

    const form = useForm<IntegrationInstanceModel>({
        defaultValues: {
            description: integrationInstance?.description || '',
            enabled: integrationInstance?.enabled || false,
            integration: integrationInstance?.integration || null,
            integrationId: integrationInstance?.id || undefined,
            integrationInstanceWorkflows: [],
            name: integrationInstance?.name || '',
            tags:
                integrationInstance?.tags?.map((tag) => ({
                    ...tag,
                    label: tag.name,
                })) || [],
        } as IntegrationInstanceModel,
    });

    const {
        // control,
        formState,
        getValues,
        handleSubmit,
        // register,
        reset,
        // setValue,
        trigger,
    } = form;

    const integrationInstanceDialogSteps = [
        {
            content: <></>,
            name: 'Basic',
        },
        {
            content: <></>,
            name: 'Workflows',
        },
    ];

    function closeDialog() {
        reset({});

        setActiveStepIndex(0);
        setIsOpen(false);

        if (onClose) {
            onClose();
        }
    }

    function saveIntegrationInstance() {
        const formData = getValues();

        if (!formData) {
            return;
        }

        // if (integrationInstance?.id) {
        //     updateIntegrationInstanceMutation.mutate({
        //         ...integrationInstance,
        //         ...formData,
        //         integrationId: formData?.integration?.id || 0,
        //     } as IntegrationInstanceModel);
        // } else {
        //     createIntegrationInstanceMutation.mutate(formData);
        // }
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

            <DialogContent className={twMerge('flex flex-col', activeStepIndex === 1 && 'h-[500px] max-h-[800px]')}>
                <Form {...form}>
                    <DialogHeader>
                        <div className="flex items-center justify-between">
                            <DialogTitle>
                                {`${integrationInstance?.id ? 'Edit' : 'New'} Instance ${
                                    !integrationInstance?.id ? '-' : ''
                                } ${
                                    !integrationInstance?.id ? integrationInstanceDialogSteps[activeStepIndex].name : ''
                                }`}
                            </DialogTitle>

                            <DialogClose asChild>
                                <Cross2Icon className="size-4 cursor-pointer opacity-70" />
                            </DialogClose>
                        </div>

                        {!integrationInstance?.id && (
                            <nav aria-label="Progress">
                                <ol className="space-y-4 md:flex md:space-y-0" role="list">
                                    {integrationInstanceDialogSteps.map((step, index) => (
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

                    {integrationInstance?.name}

                    <DialogFooter>
                        {activeStepIndex === 0 && (
                            <>
                                <DialogClose asChild>
                                    <Button variant="outline">Cancel</Button>
                                </DialogClose>

                                {!integrationInstance?.id && <Button onClick={handleNextClick}>Next</Button>}
                            </>
                        )}

                        {(activeStepIndex === 1 || integrationInstance?.id) && (
                            <>
                                {!integrationInstance?.id && (
                                    <Button onClick={() => setActiveStepIndex(activeStepIndex - 1)} variant="outline">
                                        Previous
                                    </Button>
                                )}

                                <Button
                                    disabled={integrationInstance?.enabled && !integrationInstance?.id}
                                    onClick={handleSubmit(saveIntegrationInstance)}
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

export default IntegrationInstanceDialog;
