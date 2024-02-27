import {Button} from '@/components/ui/button';
import {Dialog, DialogClose, DialogContent, DialogFooter, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {Form} from '@/components/ui/form';
import {ProjectInstanceModel, ProjectInstanceWorkflowModel, WorkflowModel} from '@/middleware/automation/configuration';
import {useUpdateProjectInstanceWorkflowMutation} from '@/mutations/automation/projectInstanceWorkflows.mutations';
import ProjectInstanceDialogWorkflowsStepItem from '@/pages/automation/project-instances/components/ProjectInstanceDialogWorkflowsStepItem';
import {ProjectInstanceKeys} from '@/queries/automation/projectInstances.queries';
import {Cross2Icon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';
import {useForm} from 'react-hook-form';

interface ProjectInstanceEditWorkflowDialogProps {
    onClose?: () => void;
    projectInstanceEnabled: boolean;
    projectInstanceWorkflow: ProjectInstanceWorkflowModel;
    workflow: WorkflowModel;
}

const ProjectInstanceEditWorkflowDialog = ({
    onClose,
    projectInstanceEnabled,
    projectInstanceWorkflow,
    workflow,
}: ProjectInstanceEditWorkflowDialogProps) => {
    const [isOpen, setIsOpen] = useState(true);

    const form = useForm<ProjectInstanceModel>({
        defaultValues: {
            projectInstanceWorkflows: [projectInstanceWorkflow],
        } as ProjectInstanceModel,
    });

    const {control, formState, getValues, handleSubmit, register, setValue} = form;

    const queryClient = useQueryClient();

    const updateProjectInstanceWorkflowMutation = useUpdateProjectInstanceWorkflowMutation({
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

        updateProjectInstanceWorkflowMutation.mutate(formData.projectInstanceWorkflows![0]);
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
            <DialogContent onInteractOutside={(event) => event.preventDefault()}>
                <Form {...form}>
                    <DialogHeader>
                        <div className="flex items-center justify-between">
                            <DialogTitle>{`Edit ${workflow?.label} Workflow`}</DialogTitle>

                            <DialogClose asChild>
                                <Cross2Icon className="size-4 cursor-pointer opacity-70" />
                            </DialogClose>
                        </div>
                    </DialogHeader>

                    <ProjectInstanceDialogWorkflowsStepItem
                        control={control}
                        formState={formState}
                        key={workflow.id!}
                        label="Enable"
                        register={register}
                        setValue={setValue}
                        switchHidden={true}
                        workflowId={workflow.id!}
                        workflowIndex={0}
                    />

                    <DialogFooter>
                        <DialogClose asChild>
                            <Button variant="outline">Cancel</Button>
                        </DialogClose>

                        <Button disabled={projectInstanceEnabled} onClick={handleSubmit(updateProjectInstanceWorkflow)}>
                            Save
                        </Button>
                    </DialogFooter>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default ProjectInstanceEditWorkflowDialog;
