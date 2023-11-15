import Button from '@/components/Button/Button';
import Dialog from '@/components/Dialog/Dialog';
import {Form} from '@/components/ui/form';
import {
    ProjectInstanceModel,
    ProjectInstanceWorkflowModel,
    WorkflowModel,
} from '@/middleware/helios/configuration';
import {useUpdateProjectInstanceWorkflowMutation} from '@/mutations/projectInstanceWorkflows.mutations';
import ProjectInstanceDialogWorkflowListItem from '@/pages/automation/project-instances/ProjectInstanceDialogWorkflowListItem';
import {ProjectInstanceKeys} from '@/queries/projectInstances.queries';
import {Close} from '@radix-ui/react-dialog';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';
import {useForm} from 'react-hook-form';

interface ProjectInstanceEditWorkflowDialogProps {
    onClose?: () => void;
    projectInstanceEnabled: boolean;
    projectInstanceWorkflow: ProjectInstanceWorkflowModel;
    visible?: boolean;
    workflow: WorkflowModel;
}

const ProjectInstanceEditWorkflowDialog = ({
    onClose,
    projectInstanceEnabled,
    projectInstanceWorkflow,
    visible = false,
    workflow,
}: ProjectInstanceEditWorkflowDialogProps) => {
    const [isOpen, setIsOpen] = useState(visible);

    const form = useForm<ProjectInstanceModel>({
        defaultValues: {
            projectInstanceWorkflows: [projectInstanceWorkflow],
        } as ProjectInstanceModel,
        mode: 'onBlur',
    });

    const {control, formState, getValues, handleSubmit, register} = form;

    const queryClient = useQueryClient();

    const updateProjectInstanceWorkflowMutation =
        useUpdateProjectInstanceWorkflowMutation({
            onSuccess: () => {
                queryClient.invalidateQueries({
                    queryKey: ProjectInstanceKeys.projectInstances,
                });

                closeDialog();
            },
        });

    function closeDialog() {
        setIsOpen(false);

        if (onClose) {
            onClose();
        }
    }

    function updateProjectInstanceWorkflow() {
        const formData = getValues();

        if (!formData) {
            return;
        }

        updateProjectInstanceWorkflowMutation.mutate(
            formData.projectInstanceWorkflows![0]
        );
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
            title={`Edit ${workflow?.label} Workflow`}
        >
            <Form {...form}>
                <div className="flex flex-col">
                    <div className="mt-4 flex flex-col ">
                        <ProjectInstanceDialogWorkflowListItem
                            control={control}
                            formState={formState}
                            key={workflow.id!}
                            label="Enable"
                            register={register}
                            switchHidden={true}
                            workflow={workflow}
                            workflowIndex={0}
                        />

                        <div className="mt-4 flex w-full justify-end space-x-2 self-end">
                            <Close asChild>
                                <Button
                                    displayType="lightBorder"
                                    label="Cancel"
                                />
                            </Close>

                            <Button
                                disabled={projectInstanceEnabled}
                                label="Save"
                                onClick={handleSubmit(
                                    updateProjectInstanceWorkflow
                                )}
                            />
                        </div>
                    </div>
                </div>
            </Form>
        </Dialog>
    );
};

export default ProjectInstanceEditWorkflowDialog;
