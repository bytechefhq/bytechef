import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogBody,
    DialogCancelButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogMain,
} from '@/components/Dialog';
import {Input} from '@/components/Input/Input';
import {Checkbox} from '@/components/ui/checkbox';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import ProjectDeploymentDialogBasicStepProjectVersionsSelect from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogBasicStepProjectVersionsSelect';
import ProjectDeploymentDialogBasicStepProjectsComboBox from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogBasicStepProjectsComboBox';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    A2aServer,
    useCreateA2aProjectMutation,
    useToolEligibleProjectVersionWorkflowsQuery,
    useUpdateA2aProjectMutation,
} from '@/shared/middleware/graphql';
import {useGetWorkspaceProjectsQuery} from '@/shared/queries/automation/projects.queries';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useMemo, useState} from 'react';
import {useForm} from 'react-hook-form';
import {z} from 'zod';

import A2aServerSkillsEditor from './A2aServerSkillsEditor';
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

    const {data: eligibleWorkflowsData} = useToolEligibleProjectVersionWorkflowsQuery(
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
    const eligibleWorkflows = eligibleWorkflowsData?.toolEligibleProjectVersionWorkflows;
    const selectedWorkflowIds = form.watch('selectedWorkflowIds');

    const hasNoEligibleWorkflows = !!(currentProjectId && currentProjectVersion && eligibleWorkflows?.length === 0);

    const editModeProjectName = useMemo(
        () => projects?.find((project) => Number(project.id) === Number(a2aProject?.projectId))?.name,
        [projects, a2aProject?.projectId]
    );

    const onSubmit = (values: z.infer<typeof formSchema>) => {
        const onSuccess = () => {
            void queryClient.invalidateQueries({queryKey: ['a2aProjectsByServerId']});
            onOpenChange(false);
        };

        if (isEditMode && a2aProject) {
            updateA2aProjectMutation.mutate(
                {id: a2aProject.id, input: {selectedWorkflowIds: values.selectedWorkflowIds}},
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
            <DialogContent>
                <DialogMain>
                    <DialogHeader
                        description="Expose agent-backed workflows (those with a New Workflow Call trigger) as A2A skills of this server."
                        title={isEditMode ? 'Edit Skills' : 'Select Workflows'}
                    />

                    <Form {...form}>
                        <form className="flex min-h-0 flex-1 flex-col" onSubmit={form.handleSubmit(onSubmit)}>
                            <DialogBody>
                                {isEditMode && (
                                    <>
                                        <FormItem>
                                            <FormLabel>Project</FormLabel>

                                            <Input
                                                disabled
                                                value={editModeProjectName || `Project #${a2aProject?.projectId}`}
                                            />
                                        </FormItem>

                                        <FormItem>
                                            <FormLabel>Project Version</FormLabel>

                                            <Input disabled value={`v${a2aProject?.projectVersion}`} />
                                        </FormItem>
                                    </>
                                )}

                                {!isEditMode && (
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

                                {hasNoEligibleWorkflows && (
                                    <p className="text-sm text-content-neutral-secondary">
                                        No tool-eligible workflows found for this project version. Only workflows with a
                                        New Workflow Call trigger can be exposed as A2A skills.
                                    </p>
                                )}

                                {eligibleWorkflows && eligibleWorkflows.length > 0 && (
                                    <FormField
                                        control={form.control}
                                        name="selectedWorkflowIds"
                                        render={({field}) => (
                                            <FormItem>
                                                <FormLabel>Workflows</FormLabel>

                                                <div className="space-y-2">
                                                    {eligibleWorkflows.map((projectWorkflow) => (
                                                        <div
                                                            className="flex items-center space-x-2"
                                                            key={projectWorkflow.id}
                                                        >
                                                            <Checkbox
                                                                checked={field.value?.includes(
                                                                    projectWorkflow.workflow.id || ''
                                                                )}
                                                                id={`a2a-server-workflow-${projectWorkflow.id}`}
                                                                onCheckedChange={(checked) => {
                                                                    const currentValues = field.value || [];

                                                                    if (checked) {
                                                                        field.onChange([
                                                                            ...currentValues,
                                                                            projectWorkflow.workflow.id,
                                                                        ]);
                                                                    } else {
                                                                        field.onChange(
                                                                            currentValues.filter(
                                                                                (workflowId) =>
                                                                                    workflowId !==
                                                                                    projectWorkflow.workflow.id
                                                                            )
                                                                        );
                                                                    }
                                                                }}
                                                            />

                                                            <label
                                                                className="text-sm leading-none font-medium"
                                                                htmlFor={`a2a-server-workflow-${projectWorkflow.id}`}
                                                            >
                                                                {projectWorkflow.workflow.label ||
                                                                    projectWorkflow.workflow.id}
                                                            </label>
                                                        </div>
                                                    ))}
                                                </div>

                                                <FormMessage />
                                            </FormItem>
                                        )}
                                        shouldUnregister={false}
                                    />
                                )}

                                {isEditMode && a2aProject && <A2aServerSkillsEditor a2aProjectId={a2aProject.id} />}
                            </DialogBody>

                            <DialogFooter>
                                <DialogCancelButton />

                                <Button
                                    disabled={
                                        hasNoEligibleWorkflows ||
                                        !selectedWorkflowIds ||
                                        selectedWorkflowIds.length === 0
                                    }
                                    label={isEditMode ? 'Update' : 'Add'}
                                    type="submit"
                                />
                            </DialogFooter>
                        </form>
                    </Form>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default A2aServerWorkflowDialog;
