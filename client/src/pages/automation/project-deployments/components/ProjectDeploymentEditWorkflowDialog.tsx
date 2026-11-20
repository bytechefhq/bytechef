import Switch from '@/components/Switch/Switch';
import {Form} from '@/components/ui/form';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ProjectDeploymentDialogWorkflowsStepItem from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogWorkflowsStepItem';
import getWorkflowComponentConnections from '@/pages/automation/project-deployments/components/project-deployment-dialog/projectDeploymentDialog-utils';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import WorkflowEditDialog from '@/shared/components/workflow-configuration/WorkflowEditDialog';
import {
    ProjectDeployment,
    ProjectDeploymentWorkflow,
    ProjectDeploymentWorkflowConnection,
    Workflow,
} from '@/shared/middleware/automation/configuration';
import {useUpdateProjectDeploymentWorkflowMutation} from '@/shared/mutations/automation/projectDeploymentWorkflows.mutations';
import {useGetWorkspaceConnectionsQuery} from '@/shared/queries/automation/connections.queries';
import {ProjectDeploymentKeys} from '@/shared/queries/automation/projectDeployments.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {InfoIcon} from 'lucide-react';
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
    const [connectionsGrouped, setConnectionsGrouped] = useState(false);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const componentConnections = getWorkflowComponentConnections(workflow);

    const {data: connections} = useGetWorkspaceConnectionsQuery(
        {
            environmentId: currentEnvironmentId,
            id: currentWorkspaceId!,
        },
        !!currentWorkspaceId
    );

    const form = useForm<ProjectDeployment>({
        defaultValues: {
            projectDeploymentWorkflows: undefined,
        } as ProjectDeployment,
    });

    const {control, formState, getValues, handleSubmit, setValue} = form;

    const queryClient = useQueryClient();

    const updateProjectDeploymentWorkflowMutation = useUpdateProjectDeploymentWorkflowMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({
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
        <WorkflowEditDialog
            description="Set the connections and inputs this workflow runs with."
            footerStartContent={
                componentConnections.length > 1 && (
                    <div className="mr-auto flex items-center gap-2">
                        <Switch
                            checked={connectionsGrouped}
                            label="Group Connections"
                            onCheckedChange={setConnectionsGrouped}
                        />

                        <Tooltip>
                            <TooltipTrigger asChild>
                                <InfoIcon className="size-4 cursor-default text-content-neutral-tertiary" />
                            </TooltipTrigger>

                            <TooltipContent>Connections grouped by their app.</TooltipContent>
                        </Tooltip>
                    </div>
                )
            }
            onClose={closeDialog}
            onSave={handleSubmit(updateProjectDeploymentWorkflow)}
            open={isOpen}
            workflowLabel={workflow?.label}
        >
            <Form {...form}>
                <ProjectDeploymentDialogWorkflowsStepItem
                    connections={connections}
                    connectionsGrouped={connectionsGrouped}
                    control={control}
                    formState={formState}
                    key={workflow.id!}
                    setValue={setValue}
                    workflow={workflow}
                    workflowIndex={0}
                    workflows={[workflow]}
                />
            </Form>
        </WorkflowEditDialog>
    );
};

export default ProjectDeploymentEditWorkflowDialog;
