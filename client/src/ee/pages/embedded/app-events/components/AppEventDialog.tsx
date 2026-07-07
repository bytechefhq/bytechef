import Button from '@/components/Button/Button';
import {Input} from '@/components/Input/Input';
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
import {AppEvent} from '@/ee/shared/middleware/embedded/configuration';
import {useCreateAppEventMutation, useUpdateAppEventMutation} from '@/ee/shared/mutations/embedded/appEvents.mutations';
import {AppEventKeys} from '@/ee/shared/queries/embedded/appEvents.queries';
import MonacoEditorLoader from '@/shared/components/MonacoEditorLoader';
import {useQueryClient} from '@tanstack/react-query';
import {ReactNode, Suspense, lazy, useState} from 'react';
import {useForm} from 'react-hook-form';

const MonacoEditor = lazy(() => import('@/shared/components/MonacoEditorWrapper'));

const EXAMPLE_SCHEMA = `{
  "userId": "12345",
  "email": "user@example.com",
  "name": "John Doe",
  "eventType": "user.signed_up",
  "timestamp": "2026-05-18T10:00:00Z"
}`;

interface AppEventDialogProps {
    appEvent?: AppEvent;
    onClose?: () => void;
    triggerNode?: ReactNode;
}

const AppEventDialog = ({appEvent, onClose, triggerNode}: AppEventDialogProps) => {
    const [isOpen, setIsOpen] = useState(!triggerNode);

    const form = useForm({
        defaultValues: {
            name: appEvent?.name || '',
            schema: appEvent?.schema || EXAMPLE_SCHEMA,
        } as AppEvent,
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
                    <form className="flex flex-col gap-4" onSubmit={handleSubmit(saveAppEvent)}>
                        <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                            <div className="flex flex-col space-y-1">
                                <DialogTitle>{`${appEvent?.id ? 'Edit' : 'Create'}`} App Event</DialogTitle>

                                <DialogDescription>
                                    Send app events from your application to trigger workflows using App Event trigger.
                                </DialogDescription>
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
                            rules={{required: true}}
                        />

                        <FormField
                            control={control}
                            name="schema"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Schema</FormLabel>

                                    <FormControl>
                                        <div className="h-48 overflow-hidden rounded-md border border-input shadow-xs focus-within:ring-1 focus-within:ring-ring focus-within:outline-hidden">
                                            <Suspense fallback={<MonacoEditorLoader />}>
                                                <MonacoEditor
                                                    defaultLanguage="json"
                                                    onChange={(value) => {
                                                        if (value) {
                                                            form.setValue('schema', value);
                                                        }
                                                    }}
                                                    onMount={() => {}}
                                                    options={{
                                                        automaticLayout: true,
                                                        folding: true,
                                                        fontSize: 12,
                                                        lineNumbers: 'on',
                                                        minimap: {enabled: false},
                                                        scrollBeyondLastLine: false,
                                                        tabSize: 2,
                                                        wordWrap: 'on',
                                                    }}
                                                    value={field.value}
                                                />
                                            </Suspense>
                                        </div>
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

export default AppEventDialog;
