import {Dialog, DialogTrigger} from '@/components/Dialog';
import {Input} from '@/components/Input/Input';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import ProjectDeploymentDialogBasicStepProjectVersionsSelect from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogBasicStepProjectVersionsSelect';
import ProjectDeploymentDialogBasicStepProjectsComboBox from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogBasicStepProjectsComboBox';
import ServerProjectWorkflowsList from '@/pages/automation/server-workflows/ServerProjectWorkflowsList';
import useServerProjectWorkflowsForm from '@/pages/automation/server-workflows/useServerProjectWorkflowsForm';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import McpServerFormField from '@/shared/components/mcp-server/McpServerFormField';
import ServerWorkflowsDialogContent from '@/shared/components/server-workflows-dialog/ServerWorkflowsDialogContent';
import {
    McpServer,
    useCreateMcpProjectMutation,
    useToolEligibleProjectVersionWorkflowsQuery,
    useUpdateMcpProjectMutation,
} from '@/shared/middleware/graphql';
import {useGetWorkspaceProjectsQuery} from '@/shared/queries/automation/projects.queries';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import {ServerIcon} from 'lucide-react';
import {ReactNode, useCallback, useMemo, useState} from 'react';
import {useForm} from 'react-hook-form';
import {z} from 'zod';

import {McpProjectItemType} from './mcp-project-workflow-list/hooks/useMcpProjectList';

const formSchema = z.object({
    mcpServerId: z.string().min(1),
    projectId: z.number().min(1),
    projectVersion: z.number().min(1),
    selectedWorkflowIds: z.array(z.string()).min(1, 'Please select at least one workflow'),
});

interface McpProjectDialogProps {
    mcpProject?: McpProjectItemType;
    mcpServer?: McpServer;
    onClose?: () => void;
    triggerNode?: ReactNode;
}

const McpProjectWorkflowDialog = ({mcpProject, mcpServer, onClose, triggerNode}: McpProjectDialogProps) => {
    const isEditMode = !!mcpProject?.id;

    const [isOpen, setIsOpen] = useState(!triggerNode);
    const [currentProjectId, setCurrentProjectId] = useState<number | undefined>(
        isEditMode ? Number(mcpProject.project?.id) : undefined
    );
    const [currentProjectVersion, setCurrentProjectVersion] = useState<number | undefined>(
        isEditMode ? (mcpProject.projectVersion ?? undefined) : undefined
    );

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {data: projects} = useGetWorkspaceProjectsQuery({
        apiCollections: false,
        id: currentWorkspaceId!,
        includeAllFields: false,
    });

    const initialSelectedWorkflowIds = useMemo(() => {
        if (!isEditMode) {
            return [];
        }

        const mcpProjectWorkflows = mcpProject.mcpProjectWorkflows ?? [];

        return mcpProjectWorkflows
            .filter((workflow) => workflow?.workflow?.id != null)
            .map((workflow) => workflow!.workflow!.id!);
    }, [isEditMode, mcpProject?.mcpProjectWorkflows]);

    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {
            mcpServerId: mcpProject?.mcpServerId || mcpServer?.id || '',
            projectId: isEditMode ? Number(mcpProject.project?.id) : undefined,
            projectVersion: isEditMode ? (mcpProject.projectVersion ?? undefined) : undefined,
            selectedWorkflowIds: initialSelectedWorkflowIds,
        },
        resolver: zodResolver(formSchema),
    });

    const {data: eligibleWorkflowsData} = useToolEligibleProjectVersionWorkflowsQuery(
        {
            projectId: String(currentProjectId || 0),
            projectVersion: currentProjectVersion || 0,
        },
        {enabled: !!(currentProjectId && currentProjectVersion)}
    );

    const {control, getValues, handleSubmit, reset, resetField, setValue, trigger} = form;

    const selectedWorkflowIds = form.watch('selectedWorkflowIds');

    const eligibleWorkflowIds = useMemo(
        () =>
            eligibleWorkflowsData?.toolEligibleProjectVersionWorkflows.map(
                (projectWorkflow) => projectWorkflow.workflow.id
            ),
        [eligibleWorkflowsData]
    );

    const {
        connections,
        form: workflowsForm,
        getWorkflowConfigurations,
        workflows,
    } = useServerProjectWorkflowsForm({
        eligibleWorkflowIds,
        environmentId: mcpServer?.environmentId != null ? Number(mcpServer.environmentId) : undefined,
        projectDeploymentId: isEditMode ? Number(mcpProject.projectDeploymentId) : undefined,
        projectId: currentProjectId,
        projectVersion: currentProjectVersion,
    });

    const queryClient = useQueryClient();

    const invalidateAndClose = () => {
        queryClient.invalidateQueries({
            queryKey: ['mcpProjectsByServerId'],
        });

        queryClient.invalidateQueries({
            queryKey: ['mcpProjects'],
        });

        closeDialog();
    };

    const createMcpProjectMutation = useCreateMcpProjectMutation({onSuccess: invalidateAndClose});

    const updateMcpProjectMutation = useUpdateMcpProjectMutation({onSuccess: invalidateAndClose});

    const closeDialog = () => {
        setIsOpen(false);

        if (onClose) {
            onClose();
        }

        reset();
    };

    const handleSelectedWorkflowIdsChange = useCallback(
        (workflowIds: string[]) => setValue('selectedWorkflowIds', workflowIds),
        [setValue]
    );

    const handleComplete = async () => {
        const workflowsValid = await workflowsForm.trigger();

        if (!workflowsValid) {
            return;
        }

        await handleSubmit(saveMcpProject)();
    };

    function saveMcpProject() {
        const formValues = getValues();

        const workflowConfigurations = getWorkflowConfigurations(formValues.selectedWorkflowIds);

        if (isEditMode) {
            updateMcpProjectMutation.mutate({
                id: mcpProject.id,
                input: {
                    selectedWorkflowIds: formValues.selectedWorkflowIds,
                    workflowConfigurations,
                },
            });
        } else {
            createMcpProjectMutation.mutate({
                input: {
                    mcpServerId: formValues.mcpServerId,
                    projectId: formValues.projectId.toString(),
                    projectVersion: formValues.projectVersion,
                    selectedWorkflowIds: formValues.selectedWorkflowIds,
                    workflowConfigurations,
                },
            });
        }
    }

    const basicStepContent = (
        <Form {...form}>
            <fieldset className="space-y-4 border-0">
                {isEditMode ? (
                    <>
                        <FormItem>
                            <FormLabel>Project</FormLabel>

                            <Input disabled value={mcpProject.project?.name || ''} />
                        </FormItem>

                        <FormItem>
                            <FormLabel>Project Version</FormLabel>

                            <Input disabled value={`v${mcpProject.projectVersion}`} />
                        </FormItem>
                    </>
                ) : (
                    <>
                        <McpServerFormField control={control} mcpServer={mcpServer} name="mcpServerId" />

                        <FormField
                            control={control}
                            name="projectId"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Project</FormLabel>

                                    <FormControl>
                                        <ProjectDeploymentDialogBasicStepProjectsComboBox
                                            onBlur={field.onBlur}
                                            onChange={(item) => {
                                                if (item) {
                                                    setValue('projectId', item.value);
                                                    resetField('projectVersion');
                                                    setValue('selectedWorkflowIds', []);

                                                    setCurrentProjectId(item.value);
                                                    setCurrentProjectVersion(undefined);
                                                }
                                            }}
                                            projects={projects}
                                            value={field.value}
                                        />
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                            shouldUnregister={false}
                        />

                        {currentProjectId && (
                            <FormField
                                control={control}
                                name="projectVersion"
                                render={({field}) => (
                                    <FormItem>
                                        <FormLabel>Project Version</FormLabel>

                                        <FormControl>
                                            <ProjectDeploymentDialogBasicStepProjectVersionsSelect
                                                onChange={(value) => {
                                                    field.onChange(value);
                                                    setCurrentProjectVersion(value);
                                                    setValue('selectedWorkflowIds', []);
                                                }}
                                                projectId={currentProjectId}
                                                projectVersion={currentProjectVersion}
                                            />
                                        </FormControl>

                                        <FormMessage />
                                    </FormItem>
                                )}
                                shouldUnregister={false}
                            />
                        )}
                    </>
                )}
            </fieldset>
        </Form>
    );

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
            {triggerNode && <DialogTrigger asChild>{triggerNode}</DialogTrigger>}

            <ServerWorkflowsDialogContent
                basicStepContent={basicStepContent}
                canProceedFromBasicStep={isEditMode || !!(currentProjectId && currentProjectVersion)}
                description="Expose workflows (those with a New Workflow Call trigger) as tools of this MCP server."
                icon={<ServerIcon />}
                isEditMode={isEditMode}
                isSubmitPending={createMcpProjectMutation.isPending || updateMcpProjectMutation.isPending}
                noEligibleWorkflowsMessage="No tool-eligible workflows found for this project version. Only workflows with a New Workflow Call trigger can be added to an MCP server."
                onComplete={handleComplete}
                selectedWorkflowIds={selectedWorkflowIds}
                title={isEditMode ? 'Edit Workflows' : 'Add Workflows'}
                validateBasicStep={() => trigger(['mcpServerId', 'projectId', 'projectVersion'])}
                workflowCount={workflows?.length}
                workflowsList={
                    workflows && (
                        <ServerProjectWorkflowsList
                            connections={connections}
                            form={workflowsForm}
                            onSelectedWorkflowIdsChange={handleSelectedWorkflowIdsChange}
                            selectedWorkflowIds={selectedWorkflowIds}
                            workflows={workflows}
                        />
                    )
                }
            />
        </Dialog>
    );
};

export default McpProjectWorkflowDialog;
