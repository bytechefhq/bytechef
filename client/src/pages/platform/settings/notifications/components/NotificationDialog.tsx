import {MultiSelect} from '@/components/MultiSelect';
import {Button} from '@/components/ui/button';
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
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {useCreateNotificationMutation, useUpdateNotificationMutation} from '@/ee/mutations/notifications.mutations';
import {useGetNotificationEventsQuery} from '@/ee/queries/notificationEvents.queries';
import {NotificationKeys} from '@/ee/queries/notifications.queries';
import {Notification, NotificationTypeEnum} from '@/shared/middleware/platform/configuration';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import React, {ReactNode, useState} from 'react';
import {useForm} from 'react-hook-form';
import {z} from 'zod';

const formSchema = z.object({
    name: z.string().min(1, 'Name is required').max(256, 'Name cannot be longer than 256 characters'),
    notificationEventIds: z.array(z.number()),
    settings: z.union([z.object({email: z.string()}), z.object({webhook: z.string()})]),
    type: z.enum(['EMAIL', 'WEBHOOK'], {
        required_error: 'Please select a notification type.',
    }),
});

interface NotificationDialogProps {
    notification?: Notification;
    onClose?: () => void;
    triggerNode?: ReactNode;
}

const NotificationDialog = ({notification, onClose, triggerNode}: NotificationDialogProps) => {
    const [isOpen, setIsOpen] = useState(!triggerNode);

    const [notificationType, setNotificationType] = useState(NotificationTypeEnum.Email);

    const ff_1132 = useFeatureFlagsStore()('ff-1132');

    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {
            name: notification?.name || '',
            notificationEventIds: notification?.notificationEvents?.map((event) => event.id) || [],
            settings: notification?.settings || {},
            type: notification?.type || notificationType,
        },
        resolver: zodResolver(formSchema),
    });

    const {control, getValues, handleSubmit, reset} = form;

    const {data: notificationEvents, isLoading: isNotificationEventsLoading} = useGetNotificationEventsQuery();

    const queryClient = useQueryClient();

    const onSuccess = () => {
        queryClient.invalidateQueries({
            queryKey: NotificationKeys,
        });

        closeDialog();
    };

    const createNotificationMutation = useCreateNotificationMutation({onSuccess});

    const updateNotificationMutation = useUpdateNotificationMutation({onSuccess});

    function closeDialog() {
        setIsOpen(false);

        if (onClose) {
            onClose();
        }

        reset();
    }

    function saveNotification() {
        if (notification?.id) {
            updateNotificationMutation.mutate({
                ...notification,
                ...getValues(),
            });
        } else {
            createNotificationMutation.mutate({
                ...notification,
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
                    <form
                        className="flex flex-col gap-4"
                        onSubmit={handleSubmit(saveNotification, (error) => console.log(error))}
                    >
                        <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                            <div className="flex flex-col space-y-1">
                                <DialogTitle>{`${notification?.id ? 'Edit' : 'Create'}`} Notification</DialogTitle>

                                <DialogDescription>Define notification parameters.</DialogDescription>
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
                        />

                        <FormField
                            control={control}
                            name="type"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Type</FormLabel>

                                    <FormControl>
                                        <Select
                                            onValueChange={(value) => {
                                                field.onChange(value);
                                                setNotificationType(value);
                                            }}
                                            value={field.value}
                                        >
                                            <SelectTrigger className="w-full">
                                                <SelectValue />
                                            </SelectTrigger>

                                            <SelectContent>
                                                <SelectItem
                                                    key={NotificationTypeEnum.Email.toString()}
                                                    value={NotificationTypeEnum.Email.toString()}
                                                >
                                                    {NotificationTypeEnum.Email.toString()}
                                                </SelectItem>

                                                {ff_1132 && (
                                                    <SelectItem
                                                        key={NotificationTypeEnum.Webhook.toString()}
                                                        value={NotificationTypeEnum.Webhook.toString()}
                                                    >
                                                        {NotificationTypeEnum.Webhook.toString()}
                                                    </SelectItem>
                                                )}
                                            </SelectContent>
                                        </Select>
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        {notificationEvents && (
                            <FormField
                                control={control}
                                name="notificationEventIds"
                                render={({field}) => (
                                    <FormItem>
                                        <FormLabel>Events</FormLabel>

                                        <FormControl>
                                            <MultiSelect
                                                defaultValue={[]}
                                                onValueChange={field.onChange}
                                                options={notificationEvents?.map((notificationEvent) => ({
                                                    label: notificationEvent.type ?? notificationEvent.id.toString(),
                                                    value: notificationEvent.id,
                                                }))}
                                                optionsLoading={isNotificationEventsLoading}
                                                placeholder="Select events"
                                                value={field.value}
                                            />
                                        </FormControl>

                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                        )}

                        <div className="pb-12">
                            <h2 className="text-base font-semibold leading-7 text-gray-900">Settings</h2>

                            {notificationType === NotificationTypeEnum.Email && (
                                <FormField
                                    control={control}
                                    name="settings.email"
                                    render={({field}) => (
                                        <FormItem>
                                            <FormLabel>Email</FormLabel>

                                            <FormControl>
                                                <Input autoComplete="email" type="email" {...field} />
                                            </FormControl>

                                            <FormMessage />
                                        </FormItem>
                                    )}
                                />
                            )}

                            {notificationType === NotificationTypeEnum.Webhook && (
                                <FormField
                                    control={control}
                                    name="settings.webhook"
                                    render={({field}) => (
                                        <FormItem>
                                            <FormLabel>Webhook URL</FormLabel>

                                            <FormControl>
                                                <Input {...field} />
                                            </FormControl>

                                            <FormMessage />
                                        </FormItem>
                                    )}
                                />
                            )}
                        </div>

                        <DialogFooter>
                            <DialogClose asChild>
                                <Button type="button" variant="outline">
                                    Cancel
                                </Button>
                            </DialogClose>

                            <Button type="submit">Save</Button>
                        </DialogFooter>
                    </form>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default NotificationDialog;
