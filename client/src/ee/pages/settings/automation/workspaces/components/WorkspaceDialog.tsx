import Button from '@/components/Button/Button';
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
import {
    useCreateWorkspaceMutation,
    useUpdateWorkspaceMutation,
} from '@/ee/shared/mutations/automation/workspaces.mutations';
import {Workspace} from '@/shared/middleware/automation/configuration';
import {WorkspaceKeys} from '@/shared/queries/automation/workspaces.queries';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import {ReactNode, useState} from 'react';
import {useForm} from 'react-hook-form';
import {z} from 'zod';

const formSchema = z.object({
    description: z.string().optional(),
    name: z.string().min(1, 'Name is required').max(256, 'Name cannot be longer than 256 characters'),
});

interface WorkspaceDialogProps {
    workspace?: Workspace;
    onClose?: () => void;
    triggerNode?: ReactNode;
}

const WorkspaceDialog = ({onClose, triggerNode, workspace}: WorkspaceDialogProps) => {
    const [isOpen, setIsOpen] = useState(!triggerNode);

    const account = useAuthenticationStore((state) => state.account);

    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {
            description: workspace?.description || '',
            name: workspace?.name || '',
        },
        resolver: zodResolver(formSchema),
    });

    const {control, getValues, handleSubmit, reset} = form;

    const queryClient = useQueryClient();

    const onSuccess = () => {
        queryClient.invalidateQueries({
            queryKey: WorkspaceKeys.workspaces,
        });

        if (account) {
            queryClient.refetchQueries({
                queryKey: WorkspaceKeys.userWorkspaces(account.id!),
            });
        }

        closeDialog();
    };

    const createWorkspaceMutation = useCreateWorkspaceMutation({onSuccess});
    const updateWorkspaceMutation = useUpdateWorkspaceMutation({onSuccess});

    function closeDialog() {
        setIsOpen(false);

        if (onClose) {
            onClose();
        }

        reset();
    }

    function saveWorkspace() {
        if (workspace?.id) {
            updateWorkspaceMutation.mutate({
                ...workspace,
                ...getValues(),
            });
        } else {
            createWorkspaceMutation.mutate({
                ...workspace,
                ...getValues(),
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
                <Form {...form}>
                    <form className="flex flex-col gap-4" onSubmit={handleSubmit(saveWorkspace)}>
                        <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                            <div className="flex flex-col space-y-1">
                                <DialogTitle>{`${workspace?.id ? 'Edit' : 'Create'}`} Workspace</DialogTitle>

                                <DialogDescription>Define workspace name and description.</DialogDescription>
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
                            name="description"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Description</FormLabel>

                                    <FormControl>
                                        <Textarea rows={6} {...field} />
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

                            <Button type="submit">Save</Button>
                        </DialogFooter>
                    </form>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default WorkspaceDialog;
