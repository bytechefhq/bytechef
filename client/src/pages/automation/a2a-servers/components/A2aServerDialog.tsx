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
import {Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {A2aServer, PlatformType, useCreateA2aServerMutation, useUpdateA2aServerMutation} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import {ReactNode, useState} from 'react';
import {useForm} from 'react-hook-form';
import {z} from 'zod';

const formSchema = z.object({
    authenticationRequired: z.boolean(),
    description: z.string(),
    enabled: z.boolean(),
    name: z.string().min(1, {message: 'Name is required'}),
});

type FormValuesType = z.infer<typeof formSchema>;

interface A2aServerDialogProps {
    a2aServer?: A2aServer;
    onOpenChange?: (open: boolean) => void;
    open?: boolean;
    triggerNode: ReactNode;
}

const A2aServerDialog = ({
    a2aServer,
    onOpenChange: externalOnOpenChange,
    open: externalOpen,
    triggerNode,
}: A2aServerDialogProps) => {
    const [internalOpen, setInternalOpen] = useState(false);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const open = externalOpen !== undefined ? externalOpen : internalOpen;
    const setOpen = externalOnOpenChange || setInternalOpen;

    const form = useForm<FormValuesType>({
        defaultValues: {
            authenticationRequired: a2aServer?.authenticationRequired ?? true,
            description: a2aServer?.description ?? '',
            enabled: a2aServer?.enabled ?? true,
            name: a2aServer?.name ?? '',
        },
        resolver: zodResolver(formSchema),
    });

    const queryClient = useQueryClient();

    const createA2aServerMutation = useCreateA2aServerMutation();
    const updateA2aServerMutation = useUpdateA2aServerMutation();

    const onSubmit = (values: FormValuesType) => {
        if (a2aServer) {
            updateA2aServerMutation.mutate(
                {
                    id: a2aServer.id,
                    input: {
                        authenticationRequired: values.authenticationRequired,
                        description: values.description,
                        enabled: values.enabled,
                        name: values.name,
                    },
                },
                {
                    onSuccess: () => {
                        queryClient.invalidateQueries({queryKey: ['a2aServers']});
                        setOpen(false);
                    },
                }
            );
        } else {
            createA2aServerMutation.mutate(
                {
                    input: {
                        authenticationRequired: values.authenticationRequired,
                        description: values.description,
                        environmentId: currentEnvironmentId!.toString(),
                        name: values.name,
                        type: PlatformType.Automation,
                    },
                },
                {
                    onSuccess: () => {
                        queryClient.invalidateQueries({queryKey: ['a2aServers']});
                        setOpen(false);
                    },
                }
            );
        }

        form.reset({});
    };

    return (
        <Dialog onOpenChange={setOpen} open={open}>
            <DialogTrigger asChild>{triggerNode}</DialogTrigger>

            <DialogContent>
                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                    <div className="flex flex-col space-y-1">
                        <DialogTitle>{a2aServer ? 'Edit A2A Server' : 'Create A2A Server'}</DialogTitle>

                        <DialogDescription>
                            {a2aServer
                                ? 'Edit the details of the A2A server.'
                                : 'Create a new A2A server by filling out the form below.'}
                        </DialogDescription>
                    </div>

                    <DialogCloseButton />
                </DialogHeader>

                <Form {...form}>
                    <form className="space-y-4" onSubmit={form.handleSubmit(onSubmit)}>
                        <FormField
                            control={form.control}
                            name="name"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Name</FormLabel>

                                    <FormControl>
                                        <Input placeholder="Enter server name" {...field} />
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <FormField
                            control={form.control}
                            name="description"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Description</FormLabel>

                                    <FormControl>
                                        <Input placeholder="Enter a description for the agent card" {...field} />
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <FormField
                            control={form.control}
                            name="authenticationRequired"
                            render={({field}) => (
                                <FormItem>
                                    <div className="flex items-center space-x-2">
                                        <FormControl>
                                            <Checkbox checked={field.value} onCheckedChange={field.onChange} />
                                        </FormControl>

                                        <FormLabel className="font-normal">Require authentication</FormLabel>
                                    </div>

                                    <FormDescription>
                                        Require an API key in addition to the server secret in the URL.
                                    </FormDescription>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        {a2aServer && (
                            <FormField
                                control={form.control}
                                name="enabled"
                                render={({field}) => (
                                    <FormItem>
                                        <div className="flex items-center space-x-2">
                                            <FormControl>
                                                <Checkbox checked={field.value} onCheckedChange={field.onChange} />
                                            </FormControl>

                                            <FormLabel className="font-normal">Enabled</FormLabel>
                                        </div>

                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                        )}

                        <DialogFooter>
                            <DialogClose asChild>
                                <Button label="Cancel" type="button" variant="outline" />
                            </DialogClose>

                            <Button label="Save" type="submit" />
                        </DialogFooter>
                    </form>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default A2aServerDialog;
