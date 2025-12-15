import Button from '@/components/Button/Button';
import {MultiSelect} from '@/components/MultiSelect/MultiSelect';
import {
    Dialog,
    DialogClose,
    DialogCloseButton,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Notification, NotificationTypeEnum} from '@/shared/middleware/platform/notification';
import {UseFormReturn} from 'react-hook-form';

import useNotifications, {NotificationFormValuesType} from '../hooks/useNotifications';

interface NotificationDialogProps {
    closeEditDialog: () => void;
    form: UseFormReturn<NotificationFormValuesType, unknown, NotificationFormValuesType>;
    isEditDialogOpen: boolean;
    saveNotification: (data: NotificationFormValuesType) => void;
    selectedNotification?: Notification;
}

const NotificationDialog = ({
    closeEditDialog,
    form,
    isEditDialogOpen,
    saveNotification,
    selectedNotification,
}: NotificationDialogProps) => {
    const {
        ff_1132,
        isNotificationEventsLoading,
        notificationEvents,
        notificationType,
        openEditDialog,
        setNotificationType,
    } = useNotifications();

    const {control, handleSubmit} = form;

    return (
        <Dialog
            onOpenChange={(open) => {
                if (open) {
                    openEditDialog(selectedNotification);
                } else {
                    closeEditDialog();
                }
            }}
            open={isEditDialogOpen}
        >
            <DialogContent>
                <Form {...form}>
                    <form
                        className="flex flex-col gap-4"
                        onSubmit={handleSubmit(saveNotification, (error) =>
                            console.error('There has been an error submitting the Notifications form', error)
                        )}
                    >
                        <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                            <div className="flex flex-col space-y-1">
                                <DialogTitle>
                                    {`${selectedNotification?.id ? 'Edit' : 'Create'}`} Notification
                                </DialogTitle>

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
                                                options={notificationEvents.map((notificationEvent) => ({
                                                    label: notificationEvent.type ?? notificationEvent.id.toString(),
                                                    value: notificationEvent.id.toString(),
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

                                                setNotificationType(value as NotificationTypeEnum);
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

                        {notificationType === NotificationTypeEnum.Email && (
                            <FormField
                                control={control}
                                name="settings.email"
                                render={({field}) => (
                                    <FormItem>
                                        <FormLabel>Email</FormLabel>

                                        <FormControl>
                                            <Input
                                                autoComplete="email"
                                                onChange={(e) => field.onChange(e.target.value)}
                                                type="email"
                                                value={(field.value as string) || ''}
                                            />
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
                                            <Input
                                                onChange={(e) => field.onChange(e.target.value)}
                                                value={(field.value as string) || ''}
                                            />
                                        </FormControl>

                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                        )}

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

export default NotificationDialog;
