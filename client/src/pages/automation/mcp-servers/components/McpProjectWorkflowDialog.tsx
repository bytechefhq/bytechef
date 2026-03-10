import Button from '@/components/Button/Button';
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
import {Input} from '@/components/ui/input';
import {Textarea} from '@/components/ui/textarea';
import ProjectDeploymentDialogBasicStepProjectVersionsSelect from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogBasicStepProjectVersionsSelect';
import ProjectDeploymentDialogBasicStepProjectsComboBox from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogBasicStepProjectsComboBox';
import {
    McpProject,
    McpServer,
    useCreateMcpProjectMutation,
    useToolEligibleProjectVersionWorkflowsQuery,
    useUpdateMcpProjectMutation,
} from '@/shared/middleware/graphql';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import {ReactNode, useMemo, useState} from 'react';
import {useForm} from 'react-hook-form';
import {z} from 'zod';

const formSchema = z.object({
    description: z.string().optional(),
    mcpServerId: z.string().min(1),
    projectId: z.number().min(1),
    projectVersion: z.number().min(1),
    selectedWorkflowIds: z.array(z.string()).min(1, 'Please select at least one workflow'),
});

interface McpProjectDialogProps {
    mcpProject?: McpProject;
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
            description: '',
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

    const eligibleWorkflows = eligibleWorkflowsData?.toolEligibleProjectVersionWorkflows;

    const {control, getValues, handleSubmit, reset, resetField, setValue} = form;

    const queryClient = useQueryClient();

    const onCreateSuccess = () => {
        queryClient.invalidateQueries({
            queryKey: ['mcpProjectsByServerId'],
        });

        queryClient.invalidateQueries({
            queryKey: ['mcpProjects'],
        });

        closeDialog();
    };

    const createMcpProjectMutation = useCreateMcpProjectMutation({onSuccess: onCreateSuccess});

    const updateMcpProjectMutation = useUpdateMcpProjectMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ['mcpProjectsByServerId'],
            });

            queryClient.invalidateQueries({
                queryKey: ['mcpProjects'],
            });

            closeDialog();
        },
    });

    const closeDialog = () => {
        setIsOpen(false);

        if (onClose) {
            onClose();
        }

        reset();
    };

    function saveMcpProject() {
        const formValues = getValues();

        if (isEditMode) {
            updateMcpProjectMutation.mutate({
                id: mcpProject.id,
                input: {
                    selectedWorkflowIds: formValues.selectedWorkflowIds,
                },
            });
        } else {
            createMcpProjectMutation.mutate({
                input: {
                    mcpServerId: formValues.mcpServerId,
                    projectId: formValues.projectId.toString(),
                    projectVersion: formValues.projectVersion,
                    selectedWorkflowIds: formValues.selectedWorkflowIds,
                },
            });
        }
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
            {triggerNode && <DialogTrigger asChild>{triggerNode}</DialogTrigger>}

            <DialogContent>
                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                    <div className="flex flex-col space-y-1">
                        <DialogTitle>{`${mcpProject?.id ? 'Edit' : 'Select'}`} Workflows</DialogTitle>

                        <DialogDescription>
                            {mcpProject?.id
                                ? 'Edit the MCP server workflow configuration.'
                                : 'Select workflows to MCP server.'}
                        </DialogDescription>
                    </div>

                    <DialogCloseButton />
                </DialogHeader>

                <Form {...form}>
                    <form className="flex flex-col gap-4" onSubmit={handleSubmit(saveMcpProject)}>
                        {isEditMode && (
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
                        )}

                        {!isEditMode && (
                            <>
                                <FormField
                                    control={control}
                                    name="mcpServerId"
                                    render={({field}) => (
                                        <FormItem>
                                            <FormLabel>MCP Server</FormLabel>

                                            <FormControl>
                                                <Input
                                                    {...field}
                                                    disabled={!!mcpServer}
                                                    placeholder={mcpServer ? mcpServer.name : 'Select MCP Server'}
                                                    value={mcpServer ? mcpServer.name : field.value}
                                                />
                                            </FormControl>

                                            <FormMessage />
                                        </FormItem>
                                    )}
                                />

                                <FormField
                                    control={control}
                                    name="projectId"
                                    render={({field}) => (
                                        <FormItem>
                                            <FormLabel>Project</FormLabel>

                                            <FormControl>
                                                <ProjectDeploymentDialogBasicStepProjectsComboBox
                                                    apiCollections={false}
                                                    onBlur={field.onBlur}
                                                    onChange={(item) => {
                                                        if (item) {
                                                            setValue('projectId', item.value);
                                                            resetField('projectVersion');

                                                            setCurrentProjectId(item.value);
                                                            setCurrentProjectVersion(undefined);
                                                        }
                                                    }}
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

                        {eligibleWorkflows && eligibleWorkflows.length > 0 && (
                            <FormField
                                control={control}
                                name="selectedWorkflowIds"
                                render={({field}) => (
                                    <FormItem>
                                        <FormLabel>Select Workflows</FormLabel>

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

                                                    <label className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70">
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

                        {!isEditMode && (
                            <FormField
                                control={control}
                                name="description"
                                render={({field}) => (
                                    <FormItem>
                                        <FormLabel>Description (Optional)</FormLabel>

                                        <FormControl>
                                            <Textarea {...field} placeholder="Describe this MCP project..." />
                                        </FormControl>

                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                        )}

                        <DialogFooter>
                            <DialogClose asChild>
                                <Button label="Cancel" type="button" variant="outline" />
                            </DialogClose>

                            <Button label={isEditMode ? 'Update' : 'Add'} type="submit" />
                        </DialogFooter>
                    </form>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default McpProjectWorkflowDialog;
