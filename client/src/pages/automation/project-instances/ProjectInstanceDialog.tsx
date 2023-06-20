import {Close} from '@radix-ui/react-dialog';
import {CheckIcon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import Button from 'components/Button/Button';
import Dialog from 'components/Dialog/Dialog';
import {ProjectInstanceModel} from 'middleware/automation/configuration';
import {
    useCreateProjectInstanceMutation,
    useUpdateProjectInstanceMutation,
} from 'mutations/projects.mutations';
import {ProjectKeys} from 'queries/projects.queries';
import {useState} from 'react';
import {useForm} from 'react-hook-form';
import {twMerge} from 'tailwind-merge';

import InstanceDialogBasicStep from './InstanceDialogBasicStep';
import InstanceDialogWorkflowsStep from './InstanceDialogWorkflowsStep';

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
        formState: {errors, touchedFields},
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
                <InstanceDialogBasicStep
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
            content: <InstanceDialogWorkflowsStep getValues={getValues} />,
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
                projectId: formData?.project?.id,
            });
        } else {
            createProjectInstanceMutation.mutate(formData);
        }
    }

    return (
        <Dialog
            isOpen={isOpen}
            onOpenChange={(isOpen) =>
                isOpen ? setIsOpen(isOpen) : closeDialog()
            }
            triggerLabel={
                showTrigger
                    ? `${projectInstance?.id ? 'Edit' : 'Create'} Instance`
                    : undefined
            }
            large={true}
            wizard
        >
            <div className="flex h-full w-full rounded-l-lg">
                <div className="w-1/3 rounded-l-lg bg-gray-100 p-4 font-semibold">
                    <h2>{`${
                        projectInstance?.id ? 'Edit' : 'New'
                    } Instance`}</h2>

                    {projectInstanceDialogSteps.map((step, index) => (
                        <div key={step.name} className="relative pb-10">
                            {index !==
                                projectInstanceDialogSteps.length - 1 && (
                                <div
                                    className={twMerge(
                                        'absolute left-4 top-4 -ml-px mt-0.5 h-full w-0.5 bg-gray-300',
                                        index < activeStepIndex && 'bg-blue-600'
                                    )}
                                    aria-hidden="true"
                                />
                            )}

                            <div className="group relative mt-4 flex items-center">
                                <span
                                    className="flex items-center"
                                    aria-hidden="true"
                                >
                                    <span
                                        className={twMerge(
                                            'relative z-10 flex h-8 w-8 items-center justify-center rounded-full border-2 border-gray-300 bg-white',
                                            index <= activeStepIndex &&
                                                'border-blue-600'
                                        )}
                                    >
                                        {index < activeStepIndex && (
                                            <span className="relative z-10 flex h-8 w-8 items-center justify-center rounded-full bg-blue-600">
                                                <CheckIcon
                                                    className="h-5 w-5 text-white"
                                                    aria-hidden="true"
                                                />
                                            </span>
                                        )}

                                        {index === activeStepIndex && (
                                            <span className="h-2.5 w-2.5 rounded-full bg-blue-600" />
                                        )}

                                        {index > activeStepIndex && (
                                            <span className="h-2.5 w-2.5 rounded-full" />
                                        )}
                                    </span>
                                </span>

                                <span
                                    className={twMerge(
                                        'ml-4 flex min-w-0 flex-col text-sm font-medium',
                                        index < activeStepIndex
                                            ? 'text-gray-900'
                                            : 'text-gray-500'
                                    )}
                                >
                                    {step.name}
                                </span>
                            </div>
                        </div>
                    ))}
                </div>

                <div className="flex w-2/3 flex-col">
                    <header className="flex items-center border-b border-gray-200 p-4">
                        <h2 className="font-semibold">
                            {projectInstanceDialogSteps[activeStepIndex].name}
                        </h2>
                    </header>

                    <main className="h-full overflow-y-scroll p-4">
                        {projectInstanceDialogSteps[activeStepIndex].content}
                    </main>

                    <footer className="flex w-full justify-end space-x-2 self-end border-t border-gray-200 p-4">
                        {activeStepIndex === 0 && (
                            <>
                                <Close asChild>
                                    <Button
                                        displayType="lightBorder"
                                        label="Cancel"
                                    />
                                </Close>

                                <Button
                                    disabled={
                                        !getValues('project') ||
                                        !getValues('name')
                                    }
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
                                    onClick={() => {
                                        setActiveStepIndex(activeStepIndex - 1);
                                    }}
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
