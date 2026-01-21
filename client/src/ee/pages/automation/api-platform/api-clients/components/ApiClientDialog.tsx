import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogClose,
    DialogCloseButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from '@/components/ui/dialog';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {ApiClient} from '@/ee/shared/middleware/automation/api-platform';
import {useToast} from '@/hooks/use-toast';
import {useCreateApiClientMutation, useUpdateApiClientMutation} from '@/shared/mutations/platform/apiClients.mutations';
import {ApiClientKeys} from '@/shared/queries/platform/apiClients.queries';
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

interface ApiClientDialogProps {
    apiClient?: ApiClient;
    onClose?: () => void;
    triggerNode?: ReactNode;
}

const ApiClientDialog = ({apiClient, onClose, triggerNode}: ApiClientDialogProps) => {
    const [isOpen, setIsOpen] = useState(!triggerNode);
    const [secretApiKey, setSecretApiKey] = useState<string | undefined>();

    /* eslint-disable @typescript-eslint/no-unused-vars */
    const [_, copyToClipboard] = useCopyToClipboard();
    const {toast} = useToast();

    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {
            name: apiClient?.name || '',
        },
        resolver: zodResolver(formSchema),
    });

    const {control, getValues, handleSubmit, reset} = form;

    const queryClient = useQueryClient();

    const createApiClientMutation = useCreateApiClientMutation({
        onSuccess: (result: {secretKey?: string}) => {
            queryClient.invalidateQueries({
                queryKey: ApiClientKeys.apiClients,
            });

            setSecretApiKey(result.secretKey);

            reset();
        },
    });
    const updateApiClientMutation = useUpdateApiClientMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ApiClientKeys.apiClients,
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

    function saveApiClient() {
        if (apiClient?.id) {
            updateApiClientMutation.mutate({
                ...apiClient,
                ...getValues(),
            } as ApiClient);
        } else {
            createApiClientMutation.mutate({
                ...apiClient,
                ...getValues(),
            } as ApiClient);
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

            <DialogContent className="min-w-api-key-dialog-width">
                <Form {...form}>
                    <form className="flex flex-col gap-4" onSubmit={handleSubmit(saveApiClient)}>
                        <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                            <DialogTitle>
                                {secretApiKey
                                    ? 'Save your secret API key'
                                    : `${apiClient?.id ? 'Edit' : 'Create'} API Client`}
                            </DialogTitle>

                            <DialogCloseButton />
                        </DialogHeader>

                        {secretApiKey ? (
                            <div className="space-y-4">
                                <p className="text-sm">
                                    Please save this secret key somewhere safe and accessible. For security reasons, you
                                    won&apos;t be able to view it again through your ByteChef account. If you lose this
                                    secret key, you&apos;ll need to generate a new one.
                                </p>

                                <div className="flex space-x-1">
                                    <Input readOnly={true} value={secretApiKey} />

                                    <Button
                                        onClick={() => {
                                            copyToClipboard(secretApiKey);

                                            toast({description: 'The secret API key is copied.'});
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
                                <Button type="submit">{apiClient?.id ? 'Save' : 'Create API Client'}</Button>
                            )}
                        </DialogFooter>
                    </form>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default ApiClientDialog;
