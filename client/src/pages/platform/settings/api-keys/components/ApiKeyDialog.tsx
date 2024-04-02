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
import {ApiKeyModel} from '@/middleware/platform/user';
import {useCreateApiKeyMutation, useUpdateApiKeyMutation} from '@/mutations/platform/apiKeys.mutations';
import {ApiKeyKeys} from '@/queries/platform/apiKeys.queries';
import {Cross2Icon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {ReactNode, useState} from 'react';
import {useForm} from 'react-hook-form';

interface ApiKeyDialogProps {
    apiKey?: ApiKeyModel;
    onClose?: () => void;
    triggerNode?: ReactNode;
}

const ApiKeyDialog = ({apiKey, onClose, triggerNode}: ApiKeyDialogProps) => {
    const [isOpen, setIsOpen] = useState(!triggerNode);

    const form = useForm({
        defaultValues: {
            name: apiKey?.name || '',
        } as ApiKeyModel,
    });

    const {control, getValues, handleSubmit, reset} = form;

    const queryClient = useQueryClient();

    const onSuccess = () => {
        queryClient.invalidateQueries({
            queryKey: ApiKeyKeys.apiKeys,
        });

        closeDialog();
    };

    const createApiKeyMutation = useCreateApiKeyMutation({onSuccess});
    const updateApiKeyMutation = useUpdateApiKeyMutation({onSuccess});

    function closeDialog() {
        setIsOpen(false);

        if (onClose) {
            onClose();
        }

        reset();
    }

    function saveApiKey() {
        if (apiKey?.id) {
            updateApiKeyMutation.mutate({
                ...apiKey,
                ...getValues(),
            });
        } else {
            createApiKeyMutation.mutate({
                ...apiKey,
                ...getValues(),
            });
        }
    }

    return (
        <Dialog
            onOpenChange={(isOpen) => {
                console.log(isOpen);
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
                    <form className="flex flex-col gap-4" onSubmit={handleSubmit(saveApiKey)}>
                        <DialogHeader>
                            <div className="flex items-center justify-between">
                                <DialogTitle>{`${apiKey?.id ? 'Edit' : 'Create'}`} API Key</DialogTitle>

                                <DialogClose asChild>
                                    <Cross2Icon className="size-4 cursor-pointer opacity-70" />
                                </DialogClose>
                            </div>
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
                            rules={{required: true}}
                        />

                        <DialogFooter>
                            <DialogClose asChild>
                                <Button type="button" variant="outline">
                                    Cancel
                                </Button>
                            </DialogClose>

                            <Button onClick={handleSubmit(saveApiKey)} type="submit">
                                Save
                            </Button>
                        </DialogFooter>
                    </form>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default ApiKeyDialog;
