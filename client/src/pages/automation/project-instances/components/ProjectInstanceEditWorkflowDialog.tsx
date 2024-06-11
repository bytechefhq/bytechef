import {Button} from '@/components/ui/button';
import {Dialog, DialogClose, DialogContent, DialogFooter, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {Form} from '@/components/ui/form';
import ProjectInstanceDialogWorkflowsStepItem from '@/pages/automation/project-instances/components/ProjectInstanceDialogWorkflowsStepItem';
import {
    ProjectInstanceModel,
    ProjectInstanceWorkflowConnectionModel,
    ProjectInstanceWorkflowModel,
    WorkflowConnectionModel,
    WorkflowModel,
} from '@/shared/middleware/automation/configuration';
import {useUpdateProjectInstanceWorkflowMutation} from '@/shared/mutations/automation/projectInstanceWorkflows.mutations';
import {ProjectInstanceKeys} from '@/shared/queries/automation/projectInstances.queries';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useState} from 'react';
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
            projectInstanceWorkflows: undefined,
        } as ProjectInstanceModel,
    });

    const {control, formState, getValues, handleSubmit, setValue} = form;

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

    useEffect(() => {
        let newProjectInstanceWorkflowConnections: ProjectInstanceWorkflowConnectionModel[] = [];

        const workflowConnections: WorkflowConnectionModel[] = (workflow?.tasks ?? [])
            .flatMap((task) => task.connections ?? [])
            .concat((workflow?.triggers ?? []).flatMap((trigger) => trigger.connections ?? []));

        for (const workflowConnection of workflowConnections) {
            const projectInstanceWorkflowConnection = projectInstanceWorkflow?.connections?.find(
                (projectInstanceWorkflowConnection) =>
                    projectInstanceWorkflowConnection.workflowNodeName === workflowConnection.workflowNodeName &&
                    projectInstanceWorkflowConnection.key === workflowConnection.key
            );

            newProjectInstanceWorkflowConnections = [
                ...newProjectInstanceWorkflowConnections,
                projectInstanceWorkflowConnection!,
            ];
        }

        setValue(
            'projectInstanceWorkflows',
            [
                {
                    ...projectInstanceWorkflow,
                    connections: newProjectInstanceWorkflowConnections,
                },
            ],
            {shouldValidate: true}
        );

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

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
                        <DialogTitle>{`Edit ${workflow?.label} Workflow`}</DialogTitle>
                    </DialogHeader>

                    <ProjectInstanceDialogWorkflowsStepItem
                        control={control}
                        formState={formState}
                        key={workflow.id!}
                        setValue={setValue}
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
