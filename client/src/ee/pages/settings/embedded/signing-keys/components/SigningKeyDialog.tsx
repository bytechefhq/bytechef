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
import {Textarea} from '@/components/ui/textarea';
import {SigningKey} from '@/ee/shared/middleware/embedded/security';
import {
    useCreateSigningKeyMutation,
    useUpdateSigningKeyMutation,
} from '@/ee/shared/mutations/embedded/signingKeys.mutations';
import {SigningKeyKeys} from '@/ee/shared/queries/embedded/signingKeys.queries';
import {useToast} from '@/hooks/use-toast';
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

interface SigningKeyDialogProps {
    onClose?: () => void;
    signingKey?: SigningKey;
    triggerNode?: ReactNode;
}

const SigningKeyDialog = ({onClose, signingKey, triggerNode}: SigningKeyDialogProps) => {
    const [isOpen, setIsOpen] = useState(!triggerNode);
    const [privateKey, setPrivateKey] = useState<string | undefined>();

    /* eslint-disable @typescript-eslint/no-unused-vars */
    const [_, copyToClipboard] = useCopyToClipboard();
    const {toast} = useToast();

    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {
            name: signingKey?.name || '',
        },
        resolver: zodResolver(formSchema),
    });

    const {control, getValues, handleSubmit, reset} = form;

    const queryClient = useQueryClient();

    const createSigningKeyMutation = useCreateSigningKeyMutation({
        onSuccess: (result: {privateKey?: string}) => {
            queryClient.invalidateQueries({
                queryKey: SigningKeyKeys.signingKeys,
            });

            setPrivateKey(result.privateKey);

            reset();
        },
    });
    const updateSigningKeyMutation = useUpdateSigningKeyMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: SigningKeyKeys.signingKeys,
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
        setPrivateKey(undefined);
    }

    function saveSigningKey() {
        if (signingKey?.id) {
            updateSigningKeyMutation.mutate({
                ...signingKey,
                ...getValues(),
            } as SigningKey);
        } else {
            createSigningKeyMutation.mutate({
                ...getValues(),
            } as SigningKey);
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

            <DialogContent className="min-w-signing-key-dialog-width">
                <Form {...form}>
                    <form className="flex flex-col gap-4" onSubmit={handleSubmit(saveSigningKey)}>
                        <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                            <DialogTitle>
                                {(privateKey ? 'Save your private ' : `${signingKey?.id ? 'Edit' : 'Create'}`) +
                                    ' Signing Key'}
                            </DialogTitle>

                            <DialogCloseButton />
                        </DialogHeader>

                        {privateKey ? (
                            <div className="space-y-4">
                                <p className="text-sm">
                                    Please save this Signing Key somewhere safe and accessible. For security reasons,
                                    you won&apos;t be able to view it again through your ByteChef account. If you lose
                                    this Signing Key, you&apos;ll need to generate a new one.
                                </p>

                                <div className="flex flex-col space-y-1">
                                    <Textarea className="text-nowrap" readOnly={true} rows={6} value={privateKey} />

                                    <div className="flex justify-end">
                                        <Button
                                            onClick={() => {
                                                copyToClipboard(privateKey);

                                                toast({description: 'The Signing Key is copied.'});
                                            }}
                                        >
                                            <ClipboardIcon className="h-4" /> Copy
                                        </Button>
                                    </div>
                                </div>
                            </div>
                        ) : (
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
                        )}

                        <DialogFooter>
                            <DialogClose asChild>
                                <Button type="button" variant="outline">
                                    {privateKey ? 'Done' : 'Cancel'}
                                </Button>
                            </DialogClose>

                            {!privateKey && (
                                <Button onClick={handleSubmit(saveSigningKey)} type="submit">
                                    {signingKey?.id ? 'Save' : 'Create Signing Key'}
                                </Button>
                            )}
                        </DialogFooter>
                    </form>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default SigningKeyDialog;
