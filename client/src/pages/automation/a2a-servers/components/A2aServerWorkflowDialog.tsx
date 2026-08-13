import Button from '@/components/Button/Button';
import {Input} from '@/components/Input/Input';
import {Checkbox} from '@/components/ui/checkbox';
import {
    Dialog,
    DialogClose,
    DialogCloseButton,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from '@/components/ui/dialog';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import ProjectDeploymentDialogBasicStepProjectVersionsSelect from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogBasicStepProjectVersionsSelect';
import ProjectDeploymentDialogBasicStepProjectsComboBox from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogBasicStepProjectsComboBox';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    A2aServer,
    useA2aProjectsByServerIdQuery,
    useCreateA2aProjectMutation,
    useToolEligibleProjectVersionWorkflowsQuery,
    useUpdateA2aProjectMutation,
} from '@/shared/middleware/graphql';
import {useGetWorkspaceProjectsQuery} from '@/shared/queries/automation/projects.queries';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import {ReactNode, useEffect, useMemo, useState} from 'react';
import {useForm} from 'react-hook-form';
import {z} from 'zod';

import A2aServerSkillsEditor from './A2aServerSkillsEditor';

const formSchema = z.object({
    projectId: z.number().min(1),
    projectVersion: z.number().min(1),
    selectedWorkflowIds: z.array(z.string()).min(1, 'Please select at least one workflow'),
});

interface A2aServerWorkflowDialogProps {
    a2aServer: A2aServer;
    onOpenChange?: (open: boolean) => void;
    open?: boolean;
    triggerNode?: ReactNode;
}

const A2aServerWorkflowDialog = ({
    a2aServer,
    onOpenChange: externalOnOpenChange,
    open: externalOpen,
    triggerNode,
}: A2aServerWorkflowDialogProps) => {
    const [internalOpen, setInternalOpen] = useState(false);
    const [currentProjectId, setCurrentProjectId] = useState<number | undefined>();
    const [currentProjectVersion, setCurrentProjectVersion] = useState<number | undefined>();

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const open = externalOpen !== undefined ? externalOpen : internalOpen;
    const setOpen = externalOnOpenChange || setInternalOpen;

    const {data: projectsByServerData} = useA2aProjectsByServerIdQuery({a2aServerId: a2aServer.id});

    const {data: projects} = useGetWorkspaceProjectsQuery({
        apiCollections: false,
        id: currentWorkspaceId!,
        includeAllFields: false,
    });

    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {
            projectId: undefined,
            projectVersion: undefined,
            selectedWorkflowIds: [],
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

    const queryClient = useQueryClient();

    const existingA2aProject = projectsByServerData?.a2aProjectsByServerId?.[0] ?? undefined;
    const isEditMode = !!existingA2aProject;
    const eligibleWorkflows = eligibleWorkflowsData?.toolEligibleProjectVersionWorkflows;
    const selectedWorkflowIds = form.watch('selectedWorkflowIds');

    const hasNoEligibleWorkflows = !!(currentProjectId && currentProjectVersion && eligibleWorkflows?.length === 0);

    const editModeProjectName = useMemo(
        () => projects?.find((project) => Number(project.id) === Number(existingA2aProject?.projectId))?.name,
        [projects, existingA2aProject?.projectId]
    );

    const createA2aProjectMutation = useCreateA2aProjectMutation();
    const updateA2aProjectMutation = useUpdateA2aProjectMutation();

    const onSubmit = (values: z.infer<typeof formSchema>) => {
        const onSuccess = () => {
            queryClient.invalidateQueries({queryKey: ['a2aProjectsByServerId']});
            setOpen(false);
        };

        if (isEditMode && existingA2aProject) {
            updateA2aProjectMutation.mutate(
                {id: existingA2aProject.id, input: {selectedWorkflowIds: values.selectedWorkflowIds}},
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
                {onSuccess}
            );
        }
    };

    useEffect(() => {
        if (open && existingA2aProject?.projectId && existingA2aProject.projectVersion) {
            setCurrentProjectId(Number(existingA2aProject.projectId));
            setCurrentProjectVersion(existingA2aProject.projectVersion);

            form.reset({
                projectId: Number(existingA2aProject.projectId),
                projectVersion: existingA2aProject.projectVersion,
                selectedWorkflowIds: existingA2aProject.workflowIds ?? [],
            });
        }
    }, [
        open,
        existingA2aProject?.projectId,
        existingA2aProject?.projectVersion,
        existingA2aProject?.workflowIds,
        form,
    ]);

    return (
        <Dialog onOpenChange={setOpen} open={open}>
            {triggerNode && <DialogTrigger asChild>{triggerNode}</DialogTrigger>}

            <DialogContent>
                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                    <div className="flex flex-col space-y-1">
                        <DialogTitle>{isEditMode ? 'Edit Skills' : 'Select Workflows'}</DialogTitle>

                        <DialogDescription>
                            Expose agent-backed workflows (those with a New Workflow Call trigger) as A2A skills of this
                            server.
                        </DialogDescription>
                    </div>

                    <DialogCloseButton />
                </DialogHeader>

                <Form {...form}>
                    <form className="flex flex-col gap-4" onSubmit={form.handleSubmit(onSubmit)}>
                        {isEditMode && (
                            <>
                                <FormItem>
                                    <FormLabel>Project</FormLabel>

                                    <Input
                                        disabled
                                        value={editModeProjectName ?? `Project #${existingA2aProject?.projectId}`}
                                    />
                                </FormItem>

                                <FormItem>
                                    <FormLabel>Project Version</FormLabel>

                                    <Input disabled value={`v${existingA2aProject?.projectVersion}`} />
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
                                No tool-eligible workflows found for this project version. Only workflows with a New
                                Workflow Call trigger can be exposed as A2A skills.
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
                                                <div className="flex items-center space-x-2" key={projectWorkflow.id}>
                                                    <Checkbox
                                                        checked={field.value?.includes(
                                                            projectWorkflow.workflow.id || ''
                                                        )}
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
                                                                            workflowId !== projectWorkflow.workflow.id
                                                                    )
                                                                );
                                                            }
                                                        }}
                                                    />

                                                    <label className="text-sm leading-none font-medium">
                                                        {projectWorkflow.workflow.label || projectWorkflow.workflow.id}
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

                        {isEditMode && existingA2aProject && (
                            <A2aServerSkillsEditor a2aProjectId={existingA2aProject.id} />
                        )}

                        <DialogFooter>
                            <DialogClose asChild>
                                <Button label="Cancel" type="button" variant="outline" />
                            </DialogClose>

                            <Button
                                disabled={
                                    hasNoEligibleWorkflows || !selectedWorkflowIds || selectedWorkflowIds.length === 0
                                }
                                label={isEditMode ? 'Update' : 'Add'}
                                type="submit"
                            />
                        </DialogFooter>
                    </form>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default A2aServerWorkflowDialog;
