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
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Textarea} from '@/components/ui/textarea';
import {useToast} from '@/components/ui/use-toast';
import {SigningKeyModel} from '@/shared/middleware/embedded/user';
import {
    useCreateSigningKeyMutation,
    useUpdateSigningKeyMutation,
} from '@/shared/mutations/embedded/signingKeys.mutations';
import {SigningKeyKeys} from '@/shared/queries/embedded/signingKeys.queries';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import {useCopyToClipboard} from '@uidotdev/usehooks';
import {ClipboardIcon} from 'lucide-react';
import {ReactNode, useState} from 'react';
import {useForm} from 'react-hook-form';
import {z} from 'zod';

const formSchema = z.object({
    environment: z.string(),
});

interface SigningKeyDialogProps {
    onClose?: () => void;
    remainingEnvironments: string[];
    signingKey?: SigningKeyModel;
    triggerNode?: ReactNode;
}

const SigningKeyDialog = ({onClose, remainingEnvironments, signingKey, triggerNode}: SigningKeyDialogProps) => {
    const [isOpen, setIsOpen] = useState(!triggerNode);
    const [privateKey, setPrivateKey] = useState<string | undefined>();

    /* eslint-disable @typescript-eslint/no-unused-vars */
    const [_, copyToClipboard] = useCopyToClipboard();
    const {toast} = useToast();

    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {},
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
            } as SigningKeyModel);
        } else {
            createSigningKeyMutation.mutate({
                ...getValues(),
                name: getValues()?.environment,
            } as SigningKeyModel);
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
                    <form className="flex flex-col gap-4" onSubmit={handleSubmit(saveSigningKey)}>
                        <DialogHeader>
                            <div className="flex items-center justify-between">
                                <DialogTitle>
                                    {privateKey
                                        ? 'Save your private '
                                        : `${signingKey?.id ? 'Edit' : 'Create'}` + ' Signing Key'}
                                </DialogTitle>
                            </div>
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
                            <>
                                {!signingKey?.id && (
                                    <FormField
                                        control={control}
                                        name="environment"
                                        render={({field}) => (
                                            <FormItem>
                                                <FormLabel>Environment</FormLabel>

                                                <FormControl>
                                                    <Select
                                                        defaultValue={field.value}
                                                        onValueChange={(value) => field.onChange(value)}
                                                    >
                                                        <SelectTrigger className="w-full">
                                                            <SelectValue placeholder="Select environment" />
                                                        </SelectTrigger>

                                                        <SelectContent>
                                                            {!remainingEnvironments.find(
                                                                (remainingEnvironment) =>
                                                                    remainingEnvironment === 'TEST'
                                                            ) && <SelectItem value="TEST">Test</SelectItem>}

                                                            {!remainingEnvironments.find(
                                                                (remainingEnvironment) =>
                                                                    remainingEnvironment === 'PRODUCTION'
                                                            ) && <SelectItem value="PRODUCTION">Production</SelectItem>}
                                                        </SelectContent>
                                                    </Select>
                                                </FormControl>

                                                <FormMessage />
                                            </FormItem>
                                        )}
                                        rules={{required: true}}
                                        shouldUnregister={false}
                                    />
                                )}
                            </>
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
