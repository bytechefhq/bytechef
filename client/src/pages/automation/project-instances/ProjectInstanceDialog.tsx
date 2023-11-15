import {Form} from '@/components/ui/form';
import {ProjectInstanceTagKeys} from '@/queries/projectInstanceTags.queries';
import {ProjectInstanceKeys} from '@/queries/projectInstances.queries';
import {ProjectKeys} from '@/queries/projects.queries';
import {Close} from '@radix-ui/react-dialog';
import {useQueryClient} from '@tanstack/react-query';
import Button from 'components/Button/Button';
import Dialog from 'components/Dialog/Dialog';
import {ProjectInstanceModel} from 'middleware/helios/configuration';
import {
    useCreateProjectInstanceMutation,
    useUpdateProjectInstanceMutation,
} from 'mutations/projectInstances.mutations';
import {useState} from 'react';
import {useForm} from 'react-hook-form';
import {twMerge} from 'tailwind-merge';

import ProjectInstanceDialogBasicStep from './ProjectInstanceDialogBasicStep';
import ProjectInstanceDialogWorkflowsStep from './ProjectInstanceDialogWorkflowsStep';

interface ProjectInstanceDialogProps {
    projectInstance?: ProjectInstanceModel;
    showTrigger?: boolean;
    visible?: boolean;
    onClose?: () => void;
}

const ProjectInstanceDialog = ({
    onClose,
    projectInstance,
    showTrigger = true,
    visible = false,
}: ProjectInstanceDialogProps) => {
    const [activeStepIndex, setActiveStepIndex] = useState(0);
    const [isOpen, setIsOpen] = useState(visible);

    const form = useForm<ProjectInstanceModel>({
        defaultValues: {
            description: projectInstance?.description || '',
            enabled: projectInstance?.enabled || false,
            name: projectInstance?.name || '',
            project: projectInstance?.project || null,
            projectId: projectInstance?.id || 0,
            projectInstanceWorkflows: [],
            tags:
                projectInstance?.tags?.map((tag) => ({
                    ...tag,
                    label: tag.name,
                })) || [],
        } as ProjectInstanceModel,
        mode: 'onBlur',
    });

    const {
        control,
        formState,
        getValues,
        handleSubmit,
        register,
        reset,
        setValue,
    } = form;

    const queryClient = useQueryClient();

    const createProjectInstanceMutation = useCreateProjectInstanceMutation({
        onSuccess: () => {
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
        },
    });

    const updateProjectInstanceMutation = useUpdateProjectInstanceMutation({
        onSuccess: () => {
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
        },
    });

    function closeDialog() {
        reset();

        setActiveStepIndex(0);
        setIsOpen(false);

        if (onClose) {
            onClose();
        }
    }

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
                />
            ),
            name: 'Workflows',
        },
    ];

    function saveProjectInstance() {
        const formData = getValues();

        if (!formData) {
            return;
        }

        if (projectInstance?.id) {
            updateProjectInstanceMutation.mutate({
                ...projectInstance,
                ...formData,
                projectId: formData?.project?.id || 0,
            } as ProjectInstanceModel);
        } else {
            createProjectInstanceMutation.mutate(formData);
        }
    }

    return (
        <Dialog
            isOpen={isOpen}
            onOpenChange={(isOpen) => {
                if (isOpen) {
                    setIsOpen(isOpen);
                } else {
                    closeDialog();
                }
            }}
            triggerLabel={
                showTrigger
                    ? `${projectInstance?.id ? 'Edit' : 'Create'} Instance`
                    : undefined
            }
        >
            <Form {...form}>
                <div
                    className={twMerge(
                        'flex h-full w-full rounded-l-lg',
                        activeStepIndex === 1 && 'h-[500px] max-h-[800px]'
                    )}
                >
                    <div className="flex w-full flex-col">
                        <header className="flex items-center py-2">
                            <h2 className="font-semibold">
                                {`${
                                    projectInstance?.id ? 'Edit' : 'New'
                                } Instance ${!projectInstance?.id ? '-' : ''} ${
                                    !projectInstance?.id
                                        ? projectInstanceDialogSteps[
                                              activeStepIndex
                                          ].name
                                        : ''
                                }`}
                            </h2>
                        </header>

                        {!projectInstance?.id && (
                            <nav aria-label="Progress">
                                <ol
                                    className="space-y-4 md:flex md:space-y-0"
                                    role="list"
                                >
                                    {projectInstanceDialogSteps.map(
                                        (step, index) => (
                                            <li
                                                className="md:flex-1"
                                                key={step.name}
                                            >
                                                <div
                                                    className={twMerge(
                                                        'group flex flex-col border-l-4 py-2 pl-4 md:border-l-0 md:border-t-4 md:pb-0 md:pl-0 md:pt-4',
                                                        index <= activeStepIndex
                                                            ? 'border-gray-900 hover:border-gray-800'
                                                            : 'border-gray-200 hover:border-gray-30'
                                                    )}
                                                ></div>
                                            </li>
                                        )
                                    )}
                                </ol>
                            </nav>
                        )}

                        <main
                            className={twMerge(
                                'h-full',
                                activeStepIndex === 1 &&
                                    'overflow-y-scroll px-1'
                            )}
                        >
                            {
                                projectInstanceDialogSteps[activeStepIndex]
                                    .content
                            }
                        </main>

                        <footer className="flex w-full justify-end space-x-2 self-end pt-4">
                            {activeStepIndex === 0 && (
                                <>
                                    <Close asChild>
                                        <Button
                                            displayType="lightBorder"
                                            label="Cancel"
                                        />
                                    </Close>

                                    {!projectInstance?.id && (
                                        <Button
                                            disabled={!formState.isValid}
                                            label="Next"
                                            onClick={() => {
                                                setActiveStepIndex(
                                                    activeStepIndex + 1
                                                );
                                            }}
                                        />
                                    )}
                                </>
                            )}

                            {(activeStepIndex === 1 || projectInstance?.id) && (
                                <>
                                    {!projectInstance?.id && (
                                        <Button
                                            displayType="lightBorder"
                                            label="Previous"
                                            onClick={() =>
                                                setActiveStepIndex(
                                                    activeStepIndex - 1
                                                )
                                            }
                                        />
                                    )}

                                    <Button
                                        disabled={
                                            projectInstance?.enabled &&
                                            !projectInstance?.id
                                        }
                                        label="Save"
                                        onClick={handleSubmit(
                                            saveProjectInstance
                                        )}
                                    />
                                </>
                            )}
                        </footer>
                    </div>
                </div>
            </Form>
        </Dialog>
    );
};

export default ProjectInstanceDialog;
