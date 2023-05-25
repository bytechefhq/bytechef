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

    const remainingSteps = steps.slice(1);

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
        >
            <div className="flex h-full w-full space-x-6 overflow-auto p-2">
                <div className="w-1/2 bg-gray-100 p-2 font-semibold">
                    {`${projectInstance?.id ? 'Edit' : 'New'} Instance`}

                    <div className="relative pb-10">
                        <div
                            className="absolute left-4 top-4 -ml-px mt-0.5 h-full w-0.5 bg-blue-600"
                            aria-hidden="true"
                        ></div>

                        <div className="group relative mt-4 flex items-start">
                            <span className="relative z-10 flex h-8 w-8 items-center justify-center rounded-full bg-blue-600 group-hover:bg-indigo-800">
                                <CheckIcon
                                    className="h-5 w-5 text-white"
                                    aria-hidden="true"
                                />
                            </span>

                            <span className="ml-4 flex min-w-0 flex-col text-sm font-medium">
                                {steps[0].name}
                            </span>
                        </div>
                    </div>

                    {remainingSteps.map((step, index) => (
                        <div key={step.name} className="relative pb-10">
                            {index !== remainingSteps.length - 1 && (
                                <div
                                    className="absolute left-4 top-4 -ml-px mt-0.5 h-full w-0.5 bg-gray-300"
                                    aria-hidden="true"
                                ></div>
                            )}

                            <div className="group relative mt-4 flex items-start">
                                <span
                                    className="flex items-center"
                                    aria-hidden="true"
                                >
                                    <span
                                        className={twMerge(
                                            'relative z-10 flex h-8 w-8 items-center justify-center rounded-full border-2 bg-white',
                                            index < activeStepIndex
                                                ? 'border-blue-600'
                                                : 'border-gray-300 group-hover:border-gray-400'
                                        )}
                                    >
                                        {index < activeStepIndex && (
                                            <CheckIcon
                                                className="h-5 w-5 text-blue-600"
                                                aria-hidden="true"
                                            />
                                        )}

                                        {index === activeStepIndex && (
                                            <span className="h-2.5 w-2.5 rounded-full bg-blue-600" />
                                        )}
                                    </span>
                                </span>

                                <span
                                    className={twMerge(
                                        'ml-4 flex min-w-0 flex-col text-sm font-medium',
                                        index < activeStepIndex
                                            ? 'text-blue-600'
                                            : 'text-gray-500'
                                    )}
                                >
                                    {step.name}
                                </span>
                            </div>
                        </div>
                    ))}
                </div>

                <div className="w-1/2">
                    <h3 className="pb-3 font-semibold">
                        {steps[activeStepIndex].name}
                    </h3>

                    {steps[activeStepIndex].content}
                </div>
            </div>

            <div className="absolute bottom-2 right-2 mt-8 justify-end space-x-1">
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
