import Button from '@/components/Button/Button';
import {Input} from '@/components/Input/Input';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {
    Dialog,
    DialogCloseButton,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {ProjectApi} from '@/shared/middleware/automation/configuration';
import {CodeWorkflowLanguage, useCreateCodeWorkflowMutation} from '@/shared/middleware/graphql';
import {ProjectKeys} from '@/shared/queries/automation/projects.queries';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import {useForm} from 'react-hook-form';
import {useNavigate} from 'react-router-dom';
import {z} from 'zod';

const formSchema = z.object({
    language: z.enum(CodeWorkflowLanguage),
    name: z.string().min(1, 'Name is required').max(256, 'Name cannot be longer than 256 characters'),
});

interface NewCodeWorkflowDialogProps {
    onClose: () => void;
}

const NewCodeWorkflowDialog = ({onClose}: NewCodeWorkflowDialogProps) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {
            language: CodeWorkflowLanguage.Javascript,
            name: '',
        },
        resolver: zodResolver(formSchema),
    });

    const {control, getValues, handleSubmit} = form;

    const navigate = useNavigate();
    const queryClient = useQueryClient();

    const createCodeWorkflowMutation = useCreateCodeWorkflowMutation({
        onSuccess: async (data) => {
            const projectId = Number(data.createCodeWorkflow);

            const project = await queryClient.fetchQuery({
                queryFn: () => new ProjectApi().getProject({id: projectId}),
                queryKey: ProjectKeys.project(projectId),
            });

            onClose();

            navigate(`/automation/projects/${projectId}/project-workflows/${project.projectWorkflowIds![0]}`);
        },
    });

    const handleCreateCodeWorkflow = () => {
        const {language, name} = getValues();

        createCodeWorkflowMutation.mutate({language, name, workspaceId: currentWorkspaceId!});
    };

    return (
        <Dialog onOpenChange={(open) => !open && !createCodeWorkflowMutation.isPending && onClose()} open>
            <DialogContent
                onInteractOutside={(event) => createCodeWorkflowMutation.isPending && event.preventDefault()}
            >
                <Form {...form}>
                    <form className="flex flex-col gap-4" onSubmit={handleSubmit(handleCreateCodeWorkflow)}>
                        <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                            <div className="flex flex-col space-y-1">
                                <DialogTitle>New Code Workflow</DialogTitle>

                                <DialogDescription>
                                    Create a code-backed workflow and start editing its source.
                                </DialogDescription>
                            </div>

                            <DialogCloseButton />
                        </DialogHeader>

                        <FormField
                            control={control}
                            name="name"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Name</FormLabel>

                                    <FormControl>
                                        <Input {...field} />
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <FormField
                            control={control}
                            name="language"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Language</FormLabel>

                                    <Select onValueChange={field.onChange} value={field.value}>
                                        <FormControl>
                                            <SelectTrigger>
                                                <SelectValue />
                                            </SelectTrigger>
                                        </FormControl>

                                        <SelectContent>
                                            <SelectItem value={CodeWorkflowLanguage.Javascript}>JavaScript</SelectItem>

                                            <SelectItem value={CodeWorkflowLanguage.Python}>Python</SelectItem>

                                            <SelectItem value={CodeWorkflowLanguage.Ruby}>Ruby</SelectItem>
                                        </SelectContent>
                                    </Select>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <DialogFooter>
                            <Button
                                disabled={createCodeWorkflowMutation.isPending}
                                onClick={onClose}
                                type="button"
                                variant="ghost"
                            >
                                Cancel
                            </Button>

                            <Button disabled={createCodeWorkflowMutation.isPending} type="submit">
                                Create
                            </Button>
                        </DialogFooter>
                    </form>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default NewCodeWorkflowDialog;
