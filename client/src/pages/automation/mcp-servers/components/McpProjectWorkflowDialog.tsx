import {Button} from '@/components/ui/button';
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
import {McpProject, McpServer, useCreateMcpProjectMutation} from '@/shared/middleware/graphql';
import {useGetProjectVersionWorkflowsQuery} from '@/shared/queries/automation/projectWorkflows.queries';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import {ReactNode, useState} from 'react';
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
    const [isOpen, setIsOpen] = useState(!triggerNode);
    const [curProjectId, setCurProjectId] = useState<number | undefined>(
        mcpProject?.projectDeploymentId ? Number(mcpProject.projectDeploymentId) : undefined
    );
    const [curProjectVersion, setCurProjectVersion] = useState<number | undefined>();

    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {
            description: '',
            mcpServerId: mcpProject?.mcpServerId || mcpServer?.id || '',
            projectId: mcpProject?.projectDeploymentId ? Number(mcpProject.projectDeploymentId) : undefined,
            projectVersion: undefined,
            selectedWorkflowIds: [],
        },
        resolver: zodResolver(formSchema),
    });

    const {data: workflows} = useGetProjectVersionWorkflowsQuery(
        curProjectId || 0,
        curProjectVersion || 0,
        true,
        !!(curProjectId && curProjectVersion)
    );

    const {control, getValues, handleSubmit, reset, resetField, setValue} = form;

    const queryClient = useQueryClient();

    const onSuccess = () => {
        queryClient.invalidateQueries({
            queryKey: ['mcpProjectsByServerId'],
        });

        closeDialog();
    };

    const createMcpProjectMutation = useCreateMcpProjectMutation({onSuccess});

    const closeDialog = () => {
        setIsOpen(false);

        if (onClose) {
            onClose();
        }

        reset();
    };

    function saveMcpProject() {
        const formValues = getValues();

        if (!mcpProject?.id) {
            createMcpProjectMutation.mutate({
                input: {
                    mcpServerId: formValues.mcpServerId,
                    projectId: formValues.projectId.toString(),
                    projectVersion: formValues.projectVersion,
                    selectedWorkflowIds: formValues.selectedWorkflowIds,
                },
            });
        } else {
            // TODO: Implement update functionality when needed
            console.log('Update MCP Project:', formValues);
            closeDialog();
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

                        {!mcpProject?.id && (
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

                                                        setCurProjectId(item.value);
                                                        setCurProjectVersion(undefined);
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
                        )}

                        {!mcpProject?.id && curProjectId && (
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
                                                    setCurProjectVersion(value);
                                                    // Reset selected workflows when version changes
                                                    setValue('selectedWorkflowIds', []);
                                                }}
                                                projectId={curProjectId}
                                                projectVersion={curProjectVersion}
                                            />
                                        </FormControl>

                                        <FormMessage />
                                    </FormItem>
                                )}
                                shouldUnregister={false}
                            />
                        )}

                        {!mcpProject?.id && workflows && workflows.length > 0 && (
                            <FormField
                                control={control}
                                name="selectedWorkflowIds"
                                render={({field}) => (
                                    <FormItem>
                                        <FormLabel>Select Workflows</FormLabel>

                                        <div className="space-y-2">
                                            {workflows.map((workflow) => (
                                                <div className="flex items-center space-x-2" key={workflow.id}>
                                                    <Checkbox
                                                        checked={field.value?.includes(workflow.id || '')}
                                                        onCheckedChange={(checked) => {
                                                            const currentValues = field.value || [];
                                                            if (checked) {
                                                                field.onChange([...currentValues, workflow.id]);
                                                            } else {
                                                                field.onChange(
                                                                    currentValues.filter((id) => id !== workflow.id)
                                                                );
                                                            }
                                                        }}
                                                    />

                                                    <label className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70">
                                                        {workflow.label || workflow.id}
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

                        <DialogFooter>
                            <DialogClose asChild>
                                <Button type="button" variant="outline">
                                    Cancel
                                </Button>
                            </DialogClose>

                            <Button type="submit">{mcpProject?.id ? 'Update' : 'Add'}</Button>
                        </DialogFooter>
                    </form>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default McpProjectWorkflowDialog;
