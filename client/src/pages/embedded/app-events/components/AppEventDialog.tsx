import {Button} from '@/components/ui/button';
import {
    Dialog,
    DialogClose,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from '@/components/ui/dialog';
import {Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {AppEventModel} from '@/middleware/embedded/configuration';
import {useCreateAppEventMutation, useUpdateAppEventMutation} from '@/mutations/embedded/appEvents.mutations';
import {AppEventKeys} from '@/queries/embedded/appEvents.queries';
import Editor from '@monaco-editor/react';
import {Cross2Icon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {ReactNode, useState} from 'react';
import {useForm} from 'react-hook-form';

interface AppEventDialogProps {
    appEvent?: AppEventModel;
    onClose?: () => void;
    triggerNode?: ReactNode;
}

const AppEventDialog = ({appEvent, onClose, triggerNode}: AppEventDialogProps) => {
    const [isOpen, setIsOpen] = useState(!triggerNode);

    const form = useForm({
        defaultValues: {
            name: appEvent?.name || '',
            schema: appEvent?.schema || '',
        } as AppEventModel,
    });

    const {control, getValues, handleSubmit, reset} = form;

    const queryClient = useQueryClient();

    const onSuccess = () => {
        queryClient.invalidateQueries({
            queryKey: AppEventKeys.appEvents,
        });

        closeDialog();
    };

    const createAppEventMutation = useCreateAppEventMutation({onSuccess});
    const updateAppEventMutation = useUpdateAppEventMutation({onSuccess});

    function closeDialog() {
        setIsOpen(false);

        if (onClose) {
            onClose();
        }

        reset();
    }

    function saveAppEvent() {
        if (appEvent?.id) {
            updateAppEventMutation.mutate({
                ...appEvent,
                ...getValues(),
            });
        } else {
            createAppEventMutation.mutate({
                ...appEvent,
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
                    <DialogHeader>
                        <div className="flex items-center justify-between">
                            <DialogTitle>{`${appEvent?.id ? 'Edit' : 'Create'}`} App Event</DialogTitle>

                            <DialogClose asChild>
                                <Cross2Icon className="size-4 cursor-pointer opacity-70" />
                            </DialogClose>
                        </div>

                        <DialogDescription>
                            Send app events from your application to trigger workflows using App Event trigger.
                        </DialogDescription>
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

                    <FormField
                        control={control}
                        name="schema"
                        render={({field}) => (
                            <FormItem>
                                <FormLabel>Schema</FormLabel>

                                <FormControl>
                                    <Editor
                                        className="rounded-md border border-input shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                                        defaultLanguage="json"
                                        defaultValue={field.value || '{}'}
                                        height={200}
                                        onChange={(value) => {
                                            if (value) {
                                                form.setValue('schema', value);
                                            }
                                        }}
                                    />
                                </FormControl>

                                <FormDescription>
                                    Define the app event as JSON that will be sent from your application. App event
                                    properties will be passed into your workflows as variables.
                                </FormDescription>

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

                        <Button onClick={handleSubmit(saveAppEvent)} type="submit">
                            Save
                        </Button>
                    </DialogFooter>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default AppEventDialog;
