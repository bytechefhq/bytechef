import AlertDialog from '@/components/AlertDialog';
import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogBody,
    DialogCancelButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogMain,
} from '@/components/Dialog';
import {Input} from '@/components/Input/Input';
import {MultiSelect} from '@/components/MultiSelect/MultiSelect';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Textarea} from '@/components/ui/textarea';
import {
    Notification,
    NotificationTestResult as NotificationTestResultType,
    NotificationTypeEnum,
} from '@/shared/middleware/platform/notification';
import {EditionType, useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {Loader2Icon} from 'lucide-react';
import {useEffect, useMemo, useRef, useState} from 'react';
import {UseFormReturn, useWatch} from 'react-hook-form';

import useNotifications, {
    MASKED_VALUE,
    NotificationFormValuesType,
    normalizeWebhookUrl,
} from '../hooks/useNotifications';
import NotificationTestResult from './NotificationTestResult';
import WebhookHeadersField from './WebhookHeadersField';
import WebhookPresetSelect from './WebhookPresetSelect';

const WEBHOOK_BODY_TEMPLATE_DOCUMENTATION_URL =
    'https://docs.bytechef.io/platform/settings/notifications#body-template';

interface NotificationDialogProps {
    closeEditDialog: () => void;
    form: UseFormReturn<NotificationFormValuesType, unknown, NotificationFormValuesType>;
    isEditDialogOpen: boolean;
    onRegenerateSigningSecret?: (notificationId: number) => void;
    saveNotification: (data: NotificationFormValuesType) => void;
    selectedNotification?: Notification;
    testNotification: () => Promise<NotificationTestResultType>;
}

const NotificationDialog = ({
    closeEditDialog,
    form,
    isEditDialogOpen,
    onRegenerateSigningSecret,
    saveNotification,
    selectedNotification,
    testNotification,
}: NotificationDialogProps) => {
    const [isRegenerateConfirmOpen, setIsRegenerateConfirmOpen] = useState(false);
    const [isTestPending, setIsTestPending] = useState(false);
    const [testResult, setTestResult] = useState<NotificationTestResultType | undefined>(undefined);

    const testRunRef = useRef(0);

    const isEnterpriseEdition = useApplicationInfoStore((state) => state.application?.edition === EditionType.EE);

    const {isNotificationEventsLoading, notificationEvents, openEditDialog} = useNotifications();

    const {control, handleSubmit} = form;

    const notificationType = useWatch({control, name: 'type'});
    const settings = useWatch({control, name: 'settings'});

    const settingsKey = useMemo(() => JSON.stringify(settings ?? {}), [settings]);

    const isSavedWebhookUrlChanged = useMemo(() => {
        if (!selectedNotification?.id || selectedNotification.type !== NotificationTypeEnum.Webhook) {
            return false;
        }

        return normalizeWebhookUrl(settings?.webhook) !== normalizeWebhookUrl(selectedNotification.settings?.webhook);
    }, [selectedNotification, settings]);

    async function handleTest() {
        testRunRef.current += 1;

        const testRun = testRunRef.current;

        setIsTestPending(true);
        setTestResult(undefined);

        const isValid = await form.trigger();

        if (!isValid) {
            if (testRun === testRunRef.current) {
                setIsTestPending(false);
            }

            return;
        }

        if (testRun !== testRunRef.current) {
            return;
        }

        try {
            const result = await testNotification();

            if (testRun === testRunRef.current) {
                setTestResult(result);
            }
        } catch {
            if (testRun === testRunRef.current) {
                setTestResult(undefined);
            }
        } finally {
            if (testRun === testRunRef.current) {
                setIsTestPending(false);
            }
        }
    }

    useEffect(() => {
        testRunRef.current += 1;

        setIsTestPending(false);
        setTestResult(undefined);
    }, [isEditDialogOpen, notificationType, settingsKey]);

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
                <DialogMain>
                    <DialogHeader
                        description="Define notification parameters."
                        title={`${selectedNotification?.id ? 'Edit' : 'Create'} Notification`}
                    />

                    <Form {...form}>
                        <form
                            className="flex min-h-0 flex-1 flex-col"
                            onSubmit={handleSubmit(saveNotification, (error) =>
                                console.error('There has been an error submitting the Notifications form', error)
                            )}
                        >
                            <DialogBody>
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
                                                            label:
                                                                notificationEvent.type ??
                                                                notificationEvent.id.toString(),
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
                                                <Select onValueChange={field.onChange} value={field.value}>
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

                                                        {isEnterpriseEdition && (
                                                            <SelectItem
                                                                key={NotificationTypeEnum.Webhook.toString()}
                                                                value={NotificationTypeEnum.Webhook.toString()}
                                                            >
                                                                {NotificationTypeEnum.Webhook.toString()}
                                                            </SelectItem>
                                                        )}

                                                        {isEnterpriseEdition && (
                                                            <SelectItem
                                                                key={NotificationTypeEnum.Slack.toString()}
                                                                value={NotificationTypeEnum.Slack.toString()}
                                                            >
                                                                {NotificationTypeEnum.Slack.toString()}
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

                                {notificationType === NotificationTypeEnum.Slack && (
                                    <FormField
                                        control={control}
                                        name="settings.slackWebhookUrl"
                                        render={({field}) => (
                                            <FormItem>
                                                <FormLabel>Slack Webhook URL</FormLabel>

                                                <FormControl>
                                                    <Input
                                                        onChange={(event) => field.onChange(event.target.value)}
                                                        placeholder={
                                                            field.value === MASKED_VALUE
                                                                ? 'Saved, leave unchanged to keep'
                                                                : 'https://hooks.slack.com/services/...'
                                                        }
                                                        value={
                                                            field.value === MASKED_VALUE
                                                                ? ''
                                                                : (field.value as string) || ''
                                                        }
                                                    />
                                                </FormControl>

                                                <FormMessage />
                                            </FormItem>
                                        )}
                                    />
                                )}

                                {notificationType === NotificationTypeEnum.Webhook && <WebhookPresetSelect />}

                                {notificationType === NotificationTypeEnum.Webhook && (
                                    <FormField
                                        control={control}
                                        name="settings.webhook"
                                        render={({field}) => (
                                            <FormItem>
                                                <FormLabel>Webhook URL</FormLabel>

                                                <FormControl>
                                                    <Input
                                                        onChange={(event) => field.onChange(event.target.value)}
                                                        placeholder="https://example.com/ingest"
                                                        value={(field.value as string) || ''}
                                                    />
                                                </FormControl>

                                                <FormMessage />
                                            </FormItem>
                                        )}
                                    />
                                )}

                                {notificationType === NotificationTypeEnum.Webhook && (
                                    <>
                                        <WebhookHeadersField
                                            control={control}
                                            requireHeaderValues={isSavedWebhookUrlChanged}
                                        />

                                        <FormField
                                            control={control}
                                            name="settings.webhookBodyTemplate"
                                            render={({field}) => (
                                                <FormItem>
                                                    <FormLabel>Body template</FormLabel>

                                                    <FormControl>
                                                        <Textarea
                                                            className="font-mono text-xs md:text-xs"
                                                            onChange={(event) => field.onChange(event.target.value)}
                                                            placeholder="Optional"
                                                            rows={6}
                                                            value={(field.value as string) || ''}
                                                        />
                                                    </FormControl>

                                                    <FormDescription>
                                                        <span>
                                                            {
                                                                'Leave it empty to send the default JSON payload. Use {{path}} placeholders to insert its fields.'
                                                            }
                                                        </span>

                                                        <a
                                                            className="ml-1 underline"
                                                            href={WEBHOOK_BODY_TEMPLATE_DOCUMENTATION_URL}
                                                            rel="noreferrer"
                                                            target="_blank"
                                                        >
                                                            See the template and payload format
                                                        </a>
                                                    </FormDescription>

                                                    <FormMessage />
                                                </FormItem>
                                            )}
                                        />

                                        {selectedNotification?.id &&
                                            selectedNotification.type === NotificationTypeEnum.Webhook &&
                                            onRegenerateSigningSecret && (
                                                <div>
                                                    <Button
                                                        label={
                                                            selectedNotification.hasSigningSecret
                                                                ? 'Regenerate signing secret'
                                                                : 'Generate signing secret'
                                                        }
                                                        onClick={() => setIsRegenerateConfirmOpen(true)}
                                                        type="button"
                                                        variant="outline"
                                                    />
                                                </div>
                                            )}
                                    </>
                                )}

                                {testResult && (
                                    <NotificationTestResult notificationType={notificationType} result={testResult} />
                                )}
                            </DialogBody>

                            <DialogFooter>
                                <DialogCancelButton />

                                <Button
                                    disabled={isTestPending}
                                    icon={isTestPending ? <Loader2Icon className="animate-spin" /> : undefined}
                                    label="Test"
                                    onClick={handleTest}
                                    type="button"
                                    variant="outline"
                                />

                                <Button label="Save" type="submit" />
                            </DialogFooter>
                        </form>
                    </Form>
                </DialogMain>
            </DialogContent>

            {isRegenerateConfirmOpen && !!selectedNotification?.id && (
                <AlertDialog
                    confirmButtonVariant="default"
                    confirmLabel={selectedNotification.hasSigningSecret ? 'Regenerate' : 'Generate'}
                    description={
                        selectedNotification.hasSigningSecret
                            ? 'The current signing secret stops working immediately. Update your receiver with the new secret.'
                            : 'A signing secret will be generated and shown once.'
                    }
                    onCancel={() => setIsRegenerateConfirmOpen(false)}
                    onConfirm={() => {
                        setIsRegenerateConfirmOpen(false);

                        onRegenerateSigningSecret?.(selectedNotification.id!);
                    }}
                    open={isRegenerateConfirmOpen}
                    title={`${selectedNotification.hasSigningSecret ? 'Regenerate' : 'Generate'} signing secret?`}
                />
            )}
        </Dialog>
    );
};

export default NotificationDialog;
