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
import {ProjectInstanceTagKeys} from '@/queries/automation/projectInstanceTags.queries';
import {ProjectInstanceKeys} from '@/queries/automation/projectInstances.queries';
import {ProjectKeys} from '@/queries/automation/projects.queries';
import {Cross2Icon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {ProjectInstanceModel} from 'middleware/automation/configuration';
import {
    useCreateProjectInstanceMutation,
    useUpdateProjectInstanceMutation,
} from 'mutations/automation/projectInstances.mutations';
import {MouseEvent, ReactNode, useState} from 'react';
import {useForm} from 'react-hook-form';
import {twMerge} from 'tailwind-merge';

import ProjectInstanceDialogBasicStep from './ProjectInstanceDialogBasicStep';
import ProjectInstanceDialogWorkflowsStep from './ProjectInstanceDialogWorkflowsStep';

interface ProjectInstanceDialogProps {
    onClose?: () => void;
    projectInstance?: ProjectInstanceModel;
    triggerNode?: ReactNode;
}

const ProjectInstanceDialog = ({onClose, projectInstance, triggerNode}: ProjectInstanceDialogProps) => {
    const [activeStepIndex, setActiveStepIndex] = useState(0);
    const [isOpen, setIsOpen] = useState(!triggerNode);

    const form = useForm<ProjectInstanceModel>({
        defaultValues: {
            description: projectInstance?.description || '',
            enabled: projectInstance?.enabled || false,
            name: projectInstance?.name || '',
            project: projectInstance?.project || null,
            projectId: projectInstance?.id || undefined,
            projectInstanceWorkflows: [],
            tags:
                projectInstance?.tags?.map((tag) => ({
                    ...tag,
                    label: tag.name,
                })) || [],
        } as ProjectInstanceModel,
    });

    const {control, formState, getValues, handleSubmit, register, reset, setValue, trigger} = form;

    const queryClient = useQueryClient();

    const onSuccess = () => {
        queryClient.invalidateQueries({
            queryKey: ProjectInstanceKeys.projectInstances,
        });
        queryClient.invalidateQueries({
            queryKey: ProjectInstanceTagKeys.projectInstanceTags,
        });
        queryClient.invalidateQueries({
            queryKey: ProjectKeys.filteredProjects({}),
        });

        closeDialog();
        setActiveStepIndex(0);
    };

    const createProjectInstanceMutation = useCreateProjectInstanceMutation({
        onSuccess,
    });

    const updateProjectInstanceMutation = useUpdateProjectInstanceMutation({
        onSuccess,
    });

    const projectInstanceDialogSteps = [
        {
            content: (
                <ProjectInstanceDialogBasicStep
                    control={control}
                    errors={formState.errors}
                    getValues={getValues}
                    projectInstance={projectInstance}
                    register={register}
                    setValue={setValue}
                    touchedFields={formState.touchedFields}
                    trigger={trigger}
                />
            ),
            name: 'Basic',
        },
        {
            content: (
                <ProjectInstanceDialogWorkflowsStep
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
        reset({});

        setActiveStepIndex(0);
        setIsOpen(false);

        if (onClose) {
            onClose();
        }
    }

    function saveProjectInstance() {
        let projectInstanceFormData = getValues();

        if (!projectInstanceFormData) {
            return;
        }

        projectInstanceFormData = {
            ...projectInstanceFormData,
            projectInstanceWorkflows: projectInstanceFormData.projectInstanceWorkflows?.map(
                (projectInstanceWorkflow) => {
                    return {
                        ...projectInstanceWorkflow,
                        connections: projectInstanceWorkflow.enabled ? projectInstanceWorkflow.connections : [],
                        inputs: projectInstanceWorkflow.enabled ? projectInstanceWorkflow.inputs : {},
                    };
                }
            ),
        };

        if (projectInstance?.id) {
            updateProjectInstanceMutation.mutate({
                ...projectInstance,
                ...projectInstanceFormData,
                projectId: projectInstanceFormData?.project?.id || 0,
            } as ProjectInstanceModel);
        } else {
            createProjectInstanceMutation.mutate(projectInstanceFormData);
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
                                {`${projectInstance?.id ? 'Edit' : 'New'} Instance ${!projectInstance?.id ? '-' : ''} ${
                                    !projectInstance?.id ? projectInstanceDialogSteps[activeStepIndex].name : ''
                                }`}
                            </DialogTitle>

                            <DialogClose asChild>
                                <Cross2Icon className="size-4 cursor-pointer opacity-70" />
                            </DialogClose>
                        </div>

                        {!projectInstance?.id && (
                            <nav aria-label="Progress">
                                <ol className="space-y-4 md:flex md:space-y-0" role="list">
                                    {projectInstanceDialogSteps.map((step, index) => (
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
                        {projectInstanceDialogSteps[activeStepIndex].content}
                    </div>

                    <DialogFooter>
                        {activeStepIndex === 0 && (
                            <>
                                <DialogClose asChild>
                                    <Button variant="outline">Cancel</Button>
                                </DialogClose>

                                {!projectInstance?.id && <Button onClick={handleNextClick}>Next</Button>}
                            </>
                        )}

                        {(activeStepIndex === 1 || projectInstance?.id) && (
                            <>
                                {!projectInstance?.id && (
                                    <Button onClick={() => setActiveStepIndex(activeStepIndex - 1)} variant="outline">
                                        Previous
                                    </Button>
                                )}

                                <Button
                                    disabled={projectInstance?.enabled && !projectInstance?.id}
                                    onClick={handleSubmit(saveProjectInstance)}
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

export default ProjectInstanceDialog;
