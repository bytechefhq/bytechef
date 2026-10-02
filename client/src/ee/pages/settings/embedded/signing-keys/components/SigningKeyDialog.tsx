import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogBody,
    DialogCancelButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogMain,
    DialogTrigger,
} from '@/components/Dialog';
import {Input} from '@/components/Input/Input';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Textarea} from '@/components/ui/textarea';
import {SigningKey} from '@/ee/shared/middleware/embedded/security';
import {
    useCreateSigningKeyMutation,
    useUpdateSigningKeyMutation,
} from '@/ee/shared/mutations/embedded/signingKeys.mutations';
import {SigningKeyKeys} from '@/ee/shared/queries/embedded/signingKeys.queries';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import {useCopyToClipboard} from '@uidotdev/usehooks';
import {ClipboardIcon} from 'lucide-react';
import {ReactNode, useState} from 'react';
import {useForm} from 'react-hook-form';
import {toast} from 'sonner';
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

            <DialogContent>
                <DialogMain>
                    <Form {...form}>
                        <form className="flex min-h-0 flex-1 flex-col" onSubmit={handleSubmit(saveSigningKey)}>
                            <DialogHeader
                                title={
                                    (privateKey ? 'Save your private ' : `${signingKey?.id ? 'Edit' : 'Create'}`) +
                                    ' Signing Key'
                                }
                            />

                            <DialogBody className="flex flex-col gap-4">
                                {privateKey ? (
                                    <div className="space-y-4">
                                        <p className="text-sm">
                                            Please save this Signing Key somewhere safe and accessible. For security
                                            reasons, you won&apos;t be able to view it again through your ByteChef
                                            account. If you lose this Signing Key, you&apos;ll need to generate a new
                                            one.
                                        </p>

                                        <div className="flex flex-col space-y-1">
                                            <Textarea
                                                className="field-sizing-fixed resize-none font-mono text-xs text-nowrap md:text-xs"
                                                readOnly={true}
                                                rows={12}
                                                value={privateKey}
                                            />

                                            <div className="flex justify-end">
                                                <Button
                                                    onClick={() => {
                                                        copyToClipboard(privateKey);

                                                        toast('The Signing Key is copied.');
                                                    }}
                                                    type="button"
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
                            </DialogBody>

                            <DialogFooter>
                                <DialogCancelButton label={privateKey ? 'Done' : 'Cancel'} />

                                {!privateKey && (
                                    <Button onClick={handleSubmit(saveSigningKey)} type="submit">
                                        {signingKey?.id ? 'Save' : 'Create Signing Key'}
                                    </Button>
                                )}
                            </DialogFooter>
                        </form>
                    </Form>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default SigningKeyDialog;
