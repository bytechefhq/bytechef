import {Close} from '@radix-ui/react-dialog';
import {useQueryClient} from '@tanstack/react-query';
import Button from 'components/Button/Button';
import Dialog from 'components/Dialog/Dialog';
import {ProjectInstanceModel} from 'middleware/helios/configuration';
import {
    useCreateProjectInstanceMutation,
    useUpdateProjectInstanceMutation,
} from 'mutations/projects.mutations';
import {ProjectKeys} from 'queries/projects.queries';
import {useState} from 'react';
import {useForm} from 'react-hook-form';
import {twMerge} from 'tailwind-merge';

import ProjectInstanceDialogBasicStep from './ProjectInstanceDialogBasicStep';
import ProjectInstanceDialogWorkflowsStep from './ProjectInstanceDialogWorkflowsStep';

interface ProjectDialogProps {
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
}: ProjectDialogProps) => {
    const [activeStepIndex, setActiveStepIndex] = useState(0);
    const [isOpen, setIsOpen] = useState(visible);

    const {
        control,
        formState: {errors, isValid, touchedFields},
        getValues,
        handleSubmit,
        register,
        reset,
        setValue,
    } = useForm<ProjectInstanceModel>({
        defaultValues: {
            description: projectInstance?.description || '',
            name: projectInstance?.name || '',
            project: projectInstance?.project || null,
            projectId: projectInstance?.id || 0,
            tags:
                projectInstance?.tags?.map((tag) => ({
                    ...tag,
                    label: tag.name,
                })) || [],
        } as ProjectInstanceModel,
        mode: 'onChange',
    });

    const queryClient = useQueryClient();

    const createProjectInstanceMutation = useCreateProjectInstanceMutation({
        onSuccess: () => {
            queryClient.invalidateQueries(ProjectKeys.projectInstances);
            queryClient.invalidateQueries(ProjectKeys.projectInstanceTags);
            queryClient.invalidateQueries(ProjectKeys.projectList({}));

            closeDialog();
        },
    });

    const updateProjectInstanceMutation = useUpdateProjectInstanceMutation({
        onSuccess: () => {
            queryClient.invalidateQueries(ProjectKeys.projectInstances);
            queryClient.invalidateQueries(ProjectKeys.projectInstanceTags);
            queryClient.invalidateQueries(ProjectKeys.projectList({}));

            closeDialog();
        },
    });

    function closeDialog() {
        reset();

        setIsOpen(false);

        if (onClose) {
            onClose();
        }
    }

    const projectInstanceDialogSteps = [
        {
            content: (
                <ProjectInstanceDialogBasicStep
                    projectInstance={projectInstance}
                    control={control}
                    touchedFields={touchedFields}
                    register={register}
                    setValue={setValue}
                    getValues={getValues}
                    errors={errors}
                />
            ),
            name: 'Basic',
        },
        {
            content: (
                <ProjectInstanceDialogWorkflowsStep getValues={getValues} />
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

        setActiveStepIndex(0);
    }

    return (
        <Dialog
            className={twMerge(activeStepIndex === 1 && 'h-[800px]')}
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
            <div className="flex h-full w-full rounded-l-lg">
                <div className="flex w-full flex-col">
                    <header className="flex items-center py-2">
                        <h2 className="font-semibold">
                            {`${
                                projectInstance?.id ? 'Edit' : 'New'
                            } Instance - ${
                                projectInstanceDialogSteps[activeStepIndex].name
                            }`}
                        </h2>
                    </header>

                    <nav aria-label="Progress">
                        <ol
                            role="list"
                            className="space-y-4 md:flex md:space-y-0"
                        >
                            {projectInstanceDialogSteps.map((step, index) => (
                                <li key={step.name} className="md:flex-1">
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

                    <main
                        className={twMerge(
                            'h-full',
                            activeStepIndex === 1 && 'overflow-y-scroll px-1'
                        )}
                    >
                        {projectInstanceDialogSteps[activeStepIndex].content}
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

                                <Button
                                    disabled={!isValid}
                                    label="Next"
                                    onClick={() => {
                                        handleSubmit(saveProjectInstance);

                                        setActiveStepIndex(activeStepIndex + 1);
                                    }}
                                />
                            </>
                        )}

                        {activeStepIndex === 1 && (
                            <>
                                <Button
                                    displayType="lightBorder"
                                    label="Previous"
                                    onClick={() =>
                                        setActiveStepIndex(activeStepIndex - 1)
                                    }
                                />

                                <Button
                                    label="Save"
                                    onClick={handleSubmit(saveProjectInstance)}
                                />
                            </>
                        )}
                    </footer>
                </div>
            </div>
        </Dialog>
    );
};

export default ProjectInstanceDialog;
