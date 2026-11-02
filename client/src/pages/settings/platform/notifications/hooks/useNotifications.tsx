import {Notification, NotificationEventTypeEnum, NotificationTypeEnum} from '@/shared/middleware/platform/notification';
import {
    useCreateNotificationMutation,
    useDeleteNotificationMutation,
    useUpdateNotificationMutation,
} from '@/shared/mutations/platform/notifications.mutations';
import {useGetNotificationEventsQuery} from '@/shared/queries/platform/notificationEvents.queries';
import {NotificationKeys, useGetNotificationsQuery} from '@/shared/queries/platform/notifications.queries';
import {coreTableFeatures} from '@/shared/util/table-features';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import {createColumnHelper} from '@tanstack/react-table';
import {useEffect, useMemo, useState} from 'react';
import {useForm} from 'react-hook-form';
import z from 'zod';

import {ActionsCell, EventsCell} from '../components/NotificationTableCell';

export type NotificationFormValuesType = {
    name: string;
    notificationEventIds: string[];
    settings: Record<string, unknown>;
    type: NotificationTypeEnum;
};

export const notificationFormSchema = z
    .object({
        name: z.string().min(1, 'Name is required').max(256, 'Name cannot be longer than 256 characters'),
        notificationEventIds: z.array(z.string()).nonempty('Please select at least one notification event.'),
        settings: z.record(z.string(), z.any()),
        type: z.enum(['EMAIL', 'SLACK', 'WEBHOOK'], {
            message: 'Please select a notification type.',
        }),
    })
    .superRefine(({settings, type}, context) => {
        if (type === 'SLACK') {
            validateUrlSetting({
                context,
                invalidMessage: 'Enter a valid Slack webhook URL.',
                requiredMessage: 'Slack webhook URL is required.',
                settingKey: 'slackWebhookUrl',
                value: settings.slackWebhookUrl,
            });
        } else if (type === 'WEBHOOK') {
            validateUrlSetting({
                context,
                invalidMessage: 'Enter a valid http or https URL.',
                requiredMessage: 'Webhook URL is required.',
                settingKey: 'webhook',
                value: settings.webhook,
            });
        }
    });

interface ValidateUrlSettingPropsI {
    context: z.RefinementCtx;
    invalidMessage: string;
    requiredMessage: string;
    settingKey: string;
    value: unknown;
}

function validateUrlSetting({context, invalidMessage, requiredMessage, settingKey, value}: ValidateUrlSettingPropsI) {
    if (typeof value !== 'string' || value.trim() === '') {
        context.addIssue({
            code: 'custom',
            message: requiredMessage,
            path: ['settings', settingKey],
        });

        return;
    }

    if (!z.url({protocol: /^https?$/}).safeParse(value.trim()).success) {
        context.addIssue({
            code: 'custom',
            message: invalidMessage,
            path: ['settings', settingKey],
        });
    }
}

function trimUrlSettings(settings: Record<string, unknown>): Notification['settings'] {
    const trimmedSettings: Record<string, unknown> = {...settings};

    for (const settingKey of ['slackWebhookUrl', 'webhook']) {
        const value = trimmedSettings[settingKey];

        if (typeof value === 'string') {
            trimmedSettings[settingKey] = value.trim();
        }
    }

    return trimmedSettings as Notification['settings'];
}

const emptyFormValues = {
    name: '',
    notificationEventIds: [],
    settings: {},
    type: NotificationTypeEnum.Email,
};

export default function useNotifications() {
    const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);
    const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);
    const [selectedNotification, setSelectedNotification] = useState<Notification | undefined>(undefined);

    const {
        data: notificationsData,
        error: notificationsError,
        isLoading: isNotificationsLoading,
    } = useGetNotificationsQuery();

    const {data: allNotificationEvents, isLoading: isNotificationEventsLoading} = useGetNotificationEventsQuery();

    const queryClient = useQueryClient();

    const deleteNotificationMutation = useDeleteNotificationMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: NotificationKeys,
            });
        },
    });

    const onSuccess = () => {
        queryClient.invalidateQueries({
            queryKey: NotificationKeys,
        });

        closeEditDialog();
    };

    const createNotificationMutation = useCreateNotificationMutation({onSuccess});
    const updateNotificationMutation = useUpdateNotificationMutation({onSuccess});

    const handleDeleteNotification = (notificationId: number) => {
        if (notificationId) {
            deleteNotificationMutation.mutate(notificationId);

            setIsDeleteDialogOpen(false);
        }
    };

    function openDeleteDialog(notification: Notification) {
        setSelectedNotification(notification);

        setIsDeleteDialogOpen(true);
    }

    function openEditDialog(notification?: Notification) {
        setSelectedNotification(notification || undefined);

        setIsEditDialogOpen(true);
    }

    function closeDeleteDialog() {
        setIsDeleteDialogOpen(false);
    }

    function closeEditDialog() {
        setIsEditDialogOpen(false);

        setSelectedNotification(undefined);

        reset();
    }

    function saveNotification() {
        const formValues = getValues();

        const updatedFormData = {
            ...selectedNotification,
            ...formValues,
            ...{notificationEventIds: formValues.notificationEventIds.map((id: string) => Number(id))},
            settings: trimUrlSettings(formValues.settings),
        };

        if (selectedNotification?.id) {
            updateNotificationMutation.mutate(updatedFormData);
        } else {
            createNotificationMutation.mutate(updatedFormData);
        }

        closeEditDialog();
    }

    const notificationEvents = useMemo(
        () =>
            allNotificationEvents?.filter(
                (notificationEvent) => notificationEvent.type !== NotificationEventTypeEnum.JobCancelled
            ),
        [allNotificationEvents]
    );

    const columnHelper = createColumnHelper<typeof coreTableFeatures, Notification>();

    const columns = columnHelper.columns([
        columnHelper.accessor('name', {
            cell: (name) => <>{name.getValue()}</>,
            header: 'Name',
            meta: {
                width: '25%',
            },
        }),
        columnHelper.accessor('notificationEvents', {
            cell: (events) => <EventsCell notificationEvents={events.getValue() ?? []} />,
            header: 'Events',
        }),
        columnHelper.accessor('lastModifiedDate', {
            cell: (lastModified) => (
                <span className="whitespace-nowrap">{lastModified.getValue()?.toLocaleString()}</span>
            ),
            header: 'Last Modified Date',
        }),
        columnHelper.accessor('lastModifiedBy', {
            cell: (lastModifiedBy) => <span className="whitespace-nowrap">{lastModifiedBy.getValue()}</span>,
            header: 'Last Modified By',
        }),
        columnHelper.display({
            cell: (table) => (
                <ActionsCell
                    notification={table.row.original}
                    openDeleteDialog={openDeleteDialog}
                    openEditDialog={openEditDialog}
                />
            ),
            header: 'Actions',
            id: 'actions',
        }),
    ]);

    const form = useForm<z.infer<typeof notificationFormSchema>>({
        defaultValues: emptyFormValues,
        resolver: zodResolver(notificationFormSchema),
    });

    const {getValues, reset} = form;

    useEffect(() => {
        if (selectedNotification) {
            form.reset({
                name: selectedNotification.name || '',
                notificationEventIds: selectedNotification.notificationEvents?.map((event) => String(event.id)) || [],
                settings: selectedNotification.settings || {},
                type: selectedNotification.type || NotificationTypeEnum.Email,
            });
        } else {
            form.reset(emptyFormValues);
        }
    }, [selectedNotification, form]);

    return {
        closeDeleteDialog,
        closeEditDialog,
        columns,
        deleteNotificationMutation,
        form,
        handleDeleteNotification,
        isDeleteDialogOpen,
        isEditDialogOpen,
        isNotificationEventsLoading,
        isNotificationsLoading,
        notificationEvents,
        notificationsData,
        notificationsError,
        openDeleteDialog,
        openEditDialog,
        queryClient,
        saveNotification,
        selectedNotification,
        setSelectedNotification,
    };
}
