import Button from '@/components/Button/Button';
import Dialog from '@/components/Dialog/Dialog';
import {
    ProjectInstanceModel,
    ProjectInstanceWorkflowModel,
    WorkflowModel,
} from '@/middleware/helios/configuration';
import {useUpdateProjectInstanceWorkflowMutation} from '@/mutations/projects.mutations';
import {ProjectKeys} from '@/queries/projects.queries';
import {Close} from '@radix-ui/react-dialog';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';
import {useForm} from 'react-hook-form';

import {ProjectInstanceDialogWorkflowListItem} from './ProjectInstanceDialogWorkflowsStep';

interface ProjectInstanceEditWorkflowDialogProps {
    onClose?: () => void;
    visible?: boolean;
    projectInstanceWorkflow: ProjectInstanceWorkflowModel;
    workflow: WorkflowModel;
}

const ProjectInstanceEditWorkflowDialog = ({
    onClose,
    projectInstanceWorkflow,
    visible = false,
    workflow,
}: ProjectInstanceEditWorkflowDialogProps) => {
    const [isOpen, setIsOpen] = useState(visible);

    const {formState, getValues, handleSubmit, register} =
        useForm<ProjectInstanceModel>({
            defaultValues: {
                projectInstanceWorkflows: [projectInstanceWorkflow],
            } as ProjectInstanceModel,
            mode: 'onBlur',
        });

    const queryClient = useQueryClient();

    const updateProjectInstanceWorkflowMutation =
        useUpdateProjectInstanceWorkflowMutation({
            onSuccess: () => {
                queryClient.invalidateQueries(ProjectKeys.projectInstances);

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
            <div className="flex flex-col">
                <div className="mt-4 flex flex-col ">
                    <ProjectInstanceDialogWorkflowListItem
                        formState={formState}
                        label="Enable"
                        key={workflow.id!}
                        register={register}
                        switchHidden={true}
                        workflow={workflow}
                        workflowIndex={0}
                    />

                    <div className="mt-4 flex w-full justify-end space-x-2 self-end">
                        <Close asChild>
                            <Button displayType="lightBorder" label="Cancel" />
                        </Close>

                        <Button
                            disabled={projectInstanceWorkflow.enabled}
                            label="Save"
                            onClick={handleSubmit(
                                updateProjectInstanceWorkflow
                            )}
                        />
                    </div>
                </div>
            </div>
        </Dialog>
    );
};

export default ProjectInstanceEditWorkflowDialog;
