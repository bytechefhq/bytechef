import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogClose,
    DialogCloseButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {Form} from '@/components/ui/form';
import ProjectDeploymentDialogWorkflowsStepItem from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogWorkflowsStepItem';
import {
    ComponentConnection,
    ProjectDeployment,
    ProjectDeploymentWorkflow,
    ProjectDeploymentWorkflowConnection,
    Workflow,
} from '@/shared/middleware/automation/configuration';
import {useUpdateProjectDeploymentWorkflowMutation} from '@/shared/mutations/automation/projectDeploymentWorkflows.mutations';
import {ProjectDeploymentKeys} from '@/shared/queries/automation/projectDeployments.queries';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useState} from 'react';
import {useForm} from 'react-hook-form';

interface ProjectDeploymentEditWorkflowDialogProps {
    onClose?: () => void;
    projectDeploymentWorkflow: ProjectDeploymentWorkflow;
    workflow: Workflow;
}

const ProjectDeploymentEditWorkflowDialog = ({
    onClose,
    projectDeploymentWorkflow,
    workflow,
}: ProjectDeploymentEditWorkflowDialogProps) => {
    const [isOpen, setIsOpen] = useState(true);

    const form = useForm<ProjectDeployment>({
        defaultValues: {
            projectDeploymentWorkflows: undefined,
        } as ProjectDeployment,
    });

    const {control, formState, getValues, handleSubmit, setValue} = form;

    const queryClient = useQueryClient();

    const updateProjectDeploymentWorkflowMutation = useUpdateProjectDeploymentWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ProjectDeploymentKeys.projectDeployments,
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

    function updateProjectDeploymentWorkflow() {
        const formData = getValues();

        if (!formData) {
            return;
        }

        formData.projectDeploymentWorkflows![0].connections =
            formData.projectDeploymentWorkflows![0].connections?.filter((connection) => connection.connectionId);

        updateProjectDeploymentWorkflowMutation.mutate(formData.projectDeploymentWorkflows![0]);
    }

    useEffect(() => {
        let newProjectDeploymentWorkflowConnections: ProjectDeploymentWorkflowConnection[] = [];

        const componentConnections: ComponentConnection[] = (workflow?.tasks ?? [])
            .flatMap((task) => task.connections ?? [])
            .concat((workflow?.triggers ?? []).flatMap((trigger) => trigger.connections ?? []));

        for (const workflowConnection of componentConnections) {
            let projectDeploymentWorkflowConnection = projectDeploymentWorkflow?.connections?.find(
                (projectDeploymentWorkflowConnection) =>
                    projectDeploymentWorkflowConnection.workflowNodeName === workflowConnection.workflowNodeName &&
                    projectDeploymentWorkflowConnection.workflowConnectionKey === workflowConnection.key
            );

            if (!projectDeploymentWorkflowConnection) {
                projectDeploymentWorkflowConnection = {
                    /* eslint-disable @typescript-eslint/no-explicit-any */
                    connectionId: undefined as any,
                    workflowConnectionKey: workflowConnection.key,
                    workflowNodeName: workflowConnection.workflowNodeName,
                };
            }

            newProjectDeploymentWorkflowConnections = [
                ...newProjectDeploymentWorkflowConnections,
                projectDeploymentWorkflowConnection!,
            ];
        }

        setValue(
            'projectDeploymentWorkflows',
            [
                {
                    ...projectDeploymentWorkflow,
                    connections: newProjectDeploymentWorkflowConnections,
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
            <DialogContent className="gap-0 p-0" onInteractOutside={(event) => event.preventDefault()}>
                <Form {...form}>
                    <DialogHeader className="flex flex-row items-center justify-between space-y-0 px-6 pb-4 pt-6">
                        <DialogTitle>{`Edit ${workflow?.label} Workflow`}</DialogTitle>

                        <DialogCloseButton />
                    </DialogHeader>

                    <div className="max-h-dialog-height overflow-y-auto px-6">
                        <ProjectDeploymentDialogWorkflowsStepItem
                            control={control}
                            formState={formState}
                            key={workflow.id!}
                            setValue={setValue}
                            switchHidden={true}
                            workflow={workflow}
                            workflowIndex={0}
                        />
                    </div>

                    <DialogFooter className="px-6 pb-6 pt-4">
                        <DialogClose asChild>
                            <Button label="Cancel" variant="outline" />
                        </DialogClose>

                        <Button label="Save" onClick={handleSubmit(updateProjectDeploymentWorkflow)} />
                    </DialogFooter>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default ProjectDeploymentEditWorkflowDialog;
