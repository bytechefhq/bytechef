import {Close} from '@radix-ui/react-dialog';
import {CheckIcon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import Button from 'components/Button/Button';
import Dialog from 'components/Dialog/Dialog';
import {TagModel} from 'middleware/automation/project';
import {ProjectInstanceModel} from 'middleware/automation/project/models/ProjectInstanceModel';
import {
    useCreateProjectInstanceMutation,
    useUpdateProjectInstanceMutation,
} from 'mutations/projects.mutations';
import {ProjectKeys} from 'queries/projects.queries';
import {useState} from 'react';
import {useForm} from 'react-hook-form';
import {twMerge} from 'tailwind-merge';

import InstanceDialogWorkflowList from './InstanceDialogWorkflowList';
import StepBasic from './StepBasic';

interface ProjectDialogProps {
    projectInstance: ProjectInstanceModel | undefined;
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
            project: projectInstance?.project
                ? {
                      label: projectInstance?.project?.name,
                      ...projectInstance?.project,
                  }
                : undefined,
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

    const handleStepClick = (
        event: React.MouseEvent<HTMLAnchorElement, MouseEvent>
    ) => {
        event.preventDefault();
    };

    const steps = [
        {
            content: (
                <StepBasic
                    projectInstance={projectInstance}
                    control={control}
                    touchedFields={touchedFields}
                    register={register}
                    errors={errors}
                    setValue={setValue}
                    getValues={getValues}
                />
            ),
            name: 'Basic',
            onClick: handleStepClick,
        },
        {
            content: <InstanceDialogWorkflowList getValues={getValues} />,
            name: 'Workflows',
            onClick: handleStepClick,
        },
    ];

    function saveProjectInstance() {
        const formData = getValues();

        if (!formData) {
            return;
        }

        const tagValues = formData.tags?.map((tag: TagModel) => {
            return {id: tag.id, name: tag.name, version: tag.version};
        });

        const project = formData?.project?.name ? formData?.project : undefined;

        if (projectInstance?.id) {
            updateProjectInstanceMutation.mutate({
                ...projectInstance,
                ...formData,
                projectId: formData?.project?.id,
            } as ProjectInstanceModel);
        } else {
            createProjectInstanceMutation.mutate({
                ...formData,
                projectId: project?.id,
                tags: tagValues,
            } as ProjectInstanceModel);
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
            large={true}
            wizard
        >
            <div className="flex h-full w-full">
                <div className="w-1/3 bg-gray-100 p-4 font-semibold">
                    <h3>{`${
                        projectInstance?.id ? 'Edit' : 'New'
                    } Instance`}</h3>

                    {steps.map((step, index) => (
                        <div key={step.name} className="relative pb-10">
                            {index !== steps.length - 1 && (
                                <div
                                    className={twMerge(
                                        '' +
                                            'absolute left-4 top-4 -ml-px mt-0.5 h-full w-0.5',
                                        index < activeStepIndex
                                            ? 'bg-gray-900'
                                            : 'bg-gray-300'
                                    )}
                                    aria-hidden="true"
                                ></div>
                            )}

                            <div className="group relative mt-4 flex items-center">
                                <span
                                    className="flex items-center"
                                    aria-hidden="true"
                                >
                                    <span
                                        className={twMerge(
                                            'relative z-10 flex h-8 w-8 items-center justify-center rounded-full border-2 bg-white',
                                            index <= activeStepIndex
                                                ? 'border-gray-900'
                                                : 'border-gray-300'
                                        )}
                                    >
                                        {index < activeStepIndex && (
                                            <span className="relative z-10 flex h-8 w-8 items-center justify-center rounded-full">
                                                <CheckIcon
                                                    className="h-5 w-5 text-gray-900"
                                                    aria-hidden="true"
                                                />
                                            </span>
                                        )}

                                        {index === activeStepIndex && (
                                            <span className="h-2.5 w-2.5 rounded-full bg-gray-900" />
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

                <div className="w-2/3 overflow-auto p-4">
                    <h3 className="pb-3 font-semibold">
                        {steps[activeStepIndex].name}
                    </h3>

                    {steps[activeStepIndex].content}
                </div>
            </div>

            <div className="absolute bottom-4 right-4 mt-8 justify-end space-x-1">
                {activeStepIndex === 0 && (
                    <>
                        <Close asChild>
                            <Button displayType="lightBorder" label="Cancel" />
                        </Close>

                        <Button
                            label="Next"
                            onClick={() =>
                                setActiveStepIndex(activeStepIndex + 1)
                            }
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
            </div>
        </Dialog>
    );
};

export default ProjectInstanceDialog;
