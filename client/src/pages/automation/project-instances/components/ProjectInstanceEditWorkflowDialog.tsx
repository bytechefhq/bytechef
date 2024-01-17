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

    const {control, formState, getValues, handleSubmit, register} = form;

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
                                <Button size="icon" variant="ghost">
                                    <Cross2Icon className="h-4 w-4 opacity-70" />
                                </Button>
                            </DialogClose>
                        </div>
                    </DialogHeader>

                    <ProjectInstanceDialogWorkflowsStepItem
                        control={control}
                        formState={formState}
                        key={workflow.id!}
                        label="Enable"
                        register={register}
                        switchHidden={true}
                        workflow={workflow}
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
