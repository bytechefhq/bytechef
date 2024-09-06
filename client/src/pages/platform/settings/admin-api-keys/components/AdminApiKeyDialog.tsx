import {Button} from '@/components/ui/button';
import {
    Dialog,
    DialogClose,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from '@/components/ui/dialog';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {useToast} from '@/components/ui/use-toast';
import {AdminApiKey} from '@/shared/middleware/platform/user';
import {
    useCreateAdminApiKeyMutation,
    useUpdateAdminApiKeyMutation,
} from '@/shared/mutations/platform/adminApiKeys.mutations';
import {AdminApiKeyKeys} from '@/shared/queries/platform/adminApiKeys.queries';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import {useCopyToClipboard} from '@uidotdev/usehooks';
import {ClipboardIcon} from 'lucide-react';
import {ReactNode, useState} from 'react';
import {useForm} from 'react-hook-form';
import {z} from 'zod';

const formSchema = z.object({
    name: z.string().min(2, {
        message: 'Name must be at least 2 characters.',
    }),
});

interface AdminApiKeyDialogProps {
    adminApiKey?: AdminApiKey;
    onClose?: () => void;
    triggerNode?: ReactNode;
}

const AdminApiKeyDialog = ({adminApiKey, onClose, triggerNode}: AdminApiKeyDialogProps) => {
    const [isOpen, setIsOpen] = useState(!triggerNode);
    const [secretApiKey, setSecretApiKey] = useState<string | undefined>();

    /* eslint-disable @typescript-eslint/no-unused-vars */
    const [_, copyToClipboard] = useCopyToClipboard();
    const {toast} = useToast();

    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {
            name: adminApiKey?.name || '',
        },
        resolver: zodResolver(formSchema),
    });

    const {control, getValues, handleSubmit, reset} = form;

    const queryClient = useQueryClient();

    const createAdminApiKeyMutation = useCreateAdminApiKeyMutation({
        onSuccess: (result: {secretKey?: string}) => {
            queryClient.invalidateQueries({
                queryKey: AdminApiKeyKeys.adminApiKeys,
            });

            setSecretApiKey(result.secretKey);

            reset();
        },
    });
    const updateAdminApiKeyMutation = useUpdateAdminApiKeyMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: AdminApiKeyKeys.adminApiKeys,
            });

            closeDialog();
        },
    });

    function closeDialog() {
        setIsOpen(false);

        if (onClose) {
            onClose();
        }

        reset();
        setSecretApiKey(undefined);
    }

    function saveApiKey() {
        console.log(adminApiKey);
        if (adminApiKey?.id) {
            updateAdminApiKeyMutation.mutate({
                ...adminApiKey,
                ...getValues(),
            } as AdminApiKey);
        } else {
            createAdminApiKeyMutation.mutate({
                ...adminApiKey,
                ...getValues(),
            } as AdminApiKey);
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

            <DialogContent className="min-w-[550px]">
                <Form {...form}>
                    <form className="flex flex-col gap-4" onSubmit={handleSubmit(saveApiKey)}>
                        <DialogHeader>
                            <div className="flex items-center justify-between">
                                <DialogTitle>
                                    {secretApiKey ? 'Save your' : `${adminApiKey?.id ? 'Edit' : 'Create'}`} secret Admin
                                    API Key
                                </DialogTitle>
                            </div>
                        </DialogHeader>

                        {secretApiKey ? (
                            <div className="space-y-4">
                                <p className="text-sm">
                                    Please save this secret Admin API Key somewhere safe and accessible. For security
                                    reasons, you won&apos;t be able to view it again through your ByteChef account. If
                                    you lose this secret Admin API Key, you&apos;ll need to generate a new one.
                                </p>

                                <div className="flex space-x-1">
                                    <Input readOnly={true} value={secretApiKey} />

                                    <Button
                                        onClick={() => {
                                            copyToClipboard(secretApiKey);

                                            toast({description: 'The secret Admin API Key is copied.'});
                                        }}
                                    >
                                        <ClipboardIcon className="h-4" /> Copy
                                    </Button>
                                </div>
                            </div>
                        ) : (
                            <>
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
                            </>
                        )}

                        <DialogFooter>
                            <DialogClose asChild>
                                <Button type="button" variant="outline">
                                    {secretApiKey ? 'Done' : 'Cancel'}
                                </Button>
                            </DialogClose>

                            {!secretApiKey && (
                                <Button type="submit">
                                    {adminApiKey?.id ? 'Save' : 'Create secret Admin API Key'}
                                </Button>
                            )}
                        </DialogFooter>
                    </form>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default AdminApiKeyDialog;
