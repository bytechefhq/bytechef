import {Dialog} from '@/components/Dialog';
import {Input} from '@/components/Input/Input';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import ProjectDeploymentDialogBasicStepProjectVersionsSelect from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogBasicStepProjectVersionsSelect';
import ProjectDeploymentDialogBasicStepProjectsComboBox from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogBasicStepProjectsComboBox';
import ServerProjectWorkflowsList from '@/pages/automation/server-workflows/ServerProjectWorkflowsList';
import useServerProjectWorkflowsForm from '@/pages/automation/server-workflows/useServerProjectWorkflowsForm';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import ServerWorkflowsDialogContent from '@/shared/components/server-workflows-dialog/ServerWorkflowsDialogContent';
import {
    A2aServer,
    useA2aEligibleProjectVersionWorkflowsQuery,
    useCreateA2aProjectMutation,
    useUpdateA2aProjectMutation,
} from '@/shared/middleware/graphql';
import {useGetWorkspaceProjectsQuery} from '@/shared/queries/automation/projects.queries';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import {NetworkIcon} from 'lucide-react';
import {useCallback, useEffect, useMemo, useState} from 'react';
import {useForm} from 'react-hook-form';
import {z} from 'zod';

import {A2aProjectItemType} from './a2a-project-list/hooks/useA2aProjectList';

const formSchema = z.object({
    projectId: z.number().min(1),
    projectVersion: z.number().min(1),
    selectedWorkflowIds: z.array(z.string()).min(1, 'Please select at least one workflow'),
});

interface A2aServerWorkflowDialogProps {
    a2aProject?: A2aProjectItemType;
    a2aServer: A2aServer;
    onOpenChange: (open: boolean) => void;
    open: boolean;
}

const A2aServerWorkflowDialog = ({a2aProject, a2aServer, onOpenChange, open}: A2aServerWorkflowDialogProps) => {
    const [currentProjectId, setCurrentProjectId] = useState<number | undefined>();
    const [currentProjectVersion, setCurrentProjectVersion] = useState<number | undefined>();

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {data: projects} = useGetWorkspaceProjectsQuery({
        apiCollections: false,
        id: currentWorkspaceId!,
        includeAllFields: false,
    });

    const {data: eligibleWorkflowsData} = useA2aEligibleProjectVersionWorkflowsQuery(
        {
            projectId: String(currentProjectId || 0),
            projectVersion: currentProjectVersion || 0,
        },
        {enabled: !!(currentProjectId && currentProjectVersion)}
    );

    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {
            projectId: undefined,
            projectVersion: undefined,
            selectedWorkflowIds: [],
        },
        resolver: zodResolver(formSchema),
    });

    const queryClient = useQueryClient();

    const createA2aProjectMutation = useCreateA2aProjectMutation();
    const updateA2aProjectMutation = useUpdateA2aProjectMutation();

    const isEditMode = !!a2aProject;
    const selectedWorkflowIds = form.watch('selectedWorkflowIds');

    const eligibleWorkflowIds = useMemo(
        () =>
            eligibleWorkflowsData?.a2aEligibleProjectVersionWorkflows.map(
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
        environmentId: Number(a2aServer.environmentId),
        projectDeploymentId: a2aProject ? Number(a2aProject.projectDeploymentId) : undefined,
        projectId: currentProjectId,
        projectVersion: currentProjectVersion,
    });

    const {setValue} = form;

    const handleSelectedWorkflowIdsChange = useCallback(
        (workflowIds: string[]) => setValue('selectedWorkflowIds', workflowIds),
        [setValue]
    );

    const editModeProjectName = useMemo(
        () => projects?.find((project) => Number(project.id) === Number(a2aProject?.projectId))?.name,
        [projects, a2aProject?.projectId]
    );

    const onSubmit = (values: z.infer<typeof formSchema>) => {
        const onSuccess = () => {
            void queryClient.invalidateQueries({queryKey: ['a2aProjectsByServerId']});
            onOpenChange(false);
        };

        const workflowConfigurations = getWorkflowConfigurations(values.selectedWorkflowIds);

        if (isEditMode && a2aProject) {
            updateA2aProjectMutation.mutate(
                {id: a2aProject.id, input: {selectedWorkflowIds: values.selectedWorkflowIds, workflowConfigurations}},
                {onSuccess}
            );
        } else {
            createA2aProjectMutation.mutate(
                {
                    input: {
                        a2aServerId: a2aServer.id,
                        projectId: values.projectId.toString(),
                        projectVersion: values.projectVersion,
                        selectedWorkflowIds: values.selectedWorkflowIds,
                        workflowConfigurations,
                    },
                },
                {
                    onSuccess: () => {
                        form.reset();

                        setCurrentProjectId(undefined);
                        setCurrentProjectVersion(undefined);

                        onSuccess();
                    },
                }
            );
        }
    };

    const handleComplete = async () => {
        const workflowsValid = await workflowsForm.trigger();

        if (!workflowsValid) {
            return;
        }

        await form.handleSubmit(onSubmit)();
    };

    const basicStepContent = (
        <Form {...form}>
            <fieldset className="space-y-4 border-0">
                <FormItem>
                    <FormLabel>A2A Server</FormLabel>

                    <Input disabled value={a2aServer.name} />
                </FormItem>

                {isEditMode ? (
                    <>
                        <FormItem>
                            <FormLabel>Project</FormLabel>

                            <Input disabled value={editModeProjectName || `Project #${a2aProject?.projectId}`} />
                        </FormItem>

                        <FormItem>
                            <FormLabel>Project Version</FormLabel>

                            <Input disabled value={`v${a2aProject?.projectVersion}`} />
                        </FormItem>
                    </>
                ) : (
                    <>
                        <FormField
                            control={form.control}
                            name="projectId"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Project</FormLabel>

                                    <FormControl>
                                        <ProjectDeploymentDialogBasicStepProjectsComboBox
                                            onBlur={field.onBlur}
                                            onChange={(item) => {
                                                if (item) {
                                                    form.setValue('projectId', item.value);
                                                    form.resetField('projectVersion');
                                                    form.setValue('selectedWorkflowIds', []);

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
                                control={form.control}
                                name="projectVersion"
                                render={({field}) => (
                                    <FormItem>
                                        <FormLabel>Project Version</FormLabel>

                                        <FormControl>
                                            <ProjectDeploymentDialogBasicStepProjectVersionsSelect
                                                onChange={(value) => {
                                                    field.onChange(value);
                                                    setCurrentProjectVersion(value);
                                                    form.setValue('selectedWorkflowIds', []);
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

    useEffect(() => {
        if (open && a2aProject?.projectId && a2aProject.projectVersion) {
            setCurrentProjectId(Number(a2aProject.projectId));
            setCurrentProjectVersion(a2aProject.projectVersion);

            form.reset({
                projectId: Number(a2aProject.projectId),
                projectVersion: a2aProject.projectVersion,
                selectedWorkflowIds: a2aProject.workflowIds ?? [],
            });
        }
    }, [open, a2aProject?.projectId, a2aProject?.projectVersion, a2aProject?.workflowIds, form]);

    return (
        <Dialog onOpenChange={onOpenChange} open={open}>
            <ServerWorkflowsDialogContent
                basicStepContent={basicStepContent}
                canProceedFromBasicStep={isEditMode || !!(currentProjectId && currentProjectVersion)}
                description="Expose agent-backed workflows (those with a New Workflow Call trigger) as A2A skills of this server."
                icon={<NetworkIcon />}
                isEditMode={isEditMode}
                isSubmitPending={createA2aProjectMutation.isPending || updateA2aProjectMutation.isPending}
                noEligibleWorkflowsMessage="No A2A-eligible workflows found for this project version. Only workflows with a New Workflow Call trigger that requires no input other than message can be exposed as A2A skills."
                onComplete={handleComplete}
                selectedWorkflowIds={selectedWorkflowIds}
                title={isEditMode ? 'Edit Skills' : 'Add Workflows'}
                validateBasicStep={() => form.trigger(['projectId', 'projectVersion'])}
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

export default A2aServerWorkflowDialog;
