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
import useApiKeys from '@/ee/shared/components/api-keys/hooks/useApiKeys';
import {useApiKeysStore} from '@/ee/shared/components/api-keys/stores/useApiKeysStore';
import {useToast} from '@/hooks/use-toast';
import {zodResolver} from '@hookform/resolvers/zod';
import {useCopyToClipboard} from '@uidotdev/usehooks';
import {ClipboardIcon} from 'lucide-react';
import {ReactNode, useState} from 'react';
import {useForm} from 'react-hook-form';
import {z} from 'zod';
import {useShallow} from 'zustand/react/shallow';

const formSchema = z.object({
    name: z.string().min(2, {
        message: 'Name must be at least 2 characters.',
    }),
});

interface ApiKeyDialogProps {
    triggerNode?: ReactNode;
}

const ApiKeyDialog = ({triggerNode}: ApiKeyDialogProps) => {
    const [isOpen, setIsOpen] = useState(!triggerNode);

    const {currentApiKey, secretKey, setCurrentApiKey, setSecretKey, setShowEditDialog} = useApiKeysStore(
        useShallow((state) => ({
            currentApiKey: state.currentApiKey,
            secretKey: state.secretKey,
            setCurrentApiKey: state.setCurrentApiKey,
            setSecretKey: state.setSecretKey,
            setShowEditDialog: state.setShowEditDialog,
        }))
    );

    const {handleSave} = useApiKeys();

    /* eslint-disable @typescript-eslint/no-unused-vars */
    const [_, copyToClipboard] = useCopyToClipboard();
    const {toast} = useToast();

    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {
            name: currentApiKey?.name || '',
        },
        resolver: zodResolver(formSchema),
    });

    const {control, getValues, handleSubmit, reset} = form;

    function closeDialog() {
        reset();
        setCurrentApiKey(undefined);
        setShowEditDialog(false);
        setSecretKey(undefined);
        setIsOpen(false);
    }

    function saveApiKey() {
        handleSave({
            ...currentApiKey,
            ...getValues(),
        });
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
                    <form className="flex flex-col gap-4" onSubmit={handleSubmit(saveApiKey)}>
                        <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                            <DialogTitle>
                                {secretKey ? 'Save your' : `${currentApiKey?.id ? 'Edit' : 'Create'}`} API Key
                            </DialogTitle>

                            <DialogCloseButton />
                        </DialogHeader>

                        {secretKey ? (
                            <div className="space-y-4">
                                <p className="text-sm">
                                    Please save this secret key somewhere safe and accessible. For security reasons, you
                                    won&apos;t be able to view it again through your ByteChef account. If you lose this
                                    secret key, you&apos;ll need to generate a new one.
                                </p>

                                <div className="flex space-x-1">
                                    <Input readOnly={true} value={secretKey} />

                                    <Button
                                        onClick={(event) => {
                                            event.preventDefault();

                                            copyToClipboard(secretKey);

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
                                    {secretKey ? 'Done' : 'Cancel'}
                                </Button>
                            </DialogClose>

                            {!secretKey && (
                                <Button type="submit">{currentApiKey?.id ? 'Save' : 'Create API Key'}</Button>
                            )}
                        </DialogFooter>
                    </form>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default ApiKeyDialog;
