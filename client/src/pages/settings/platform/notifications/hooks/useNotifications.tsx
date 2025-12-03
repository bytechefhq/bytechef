import {Notification, NotificationTypeEnum} from '@/shared/middleware/platform/notification';
import {
    useCreateNotificationMutation,
    useDeleteNotificationMutation,
    useUpdateNotificationMutation,
} from '@/shared/mutations/platform/notifications.mutations';
import {useGetNotificationEventsQuery} from '@/shared/queries/platform/notificationEvents.queries';
import {NotificationKeys, useGetNotificationsQuery} from '@/shared/queries/platform/notifications.queries';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import {createColumnHelper} from '@tanstack/react-table';
import {useEffect, useState} from 'react';
import {useForm} from 'react-hook-form';
import z from 'zod';

import {ActionsCell, EventsCell} from '../components/NotificationTableCell';

export type NotificationFormValuesType = {
    name: string;
    notificationEventIds: string[];
    settings: Record<string, unknown>;
    type: NotificationTypeEnum;
};

const formSchema = z.object({
    name: z.string().min(1, 'Name is required').max(256, 'Name cannot be longer than 256 characters'),
    notificationEventIds: z.array(z.string()).nonempty('Please select at least one notification event.'),
    settings: z.record(z.string(), z.any()),
    type: z.enum(['EMAIL', 'WEBHOOK'], {
        message: 'Please select a notification type.',
    }),
});

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
    const [notificationType, setNotificationType] = useState(selectedNotification?.type || NotificationTypeEnum.Email);

    const {
        data: notificationsData,
        error: notificationsError,
        isLoading: isNotificationsLoading,
    } = useGetNotificationsQuery();

    const {data: notificationEvents, isLoading: isNotificationEventsLoading} = useGetNotificationEventsQuery();

    const queryClient = useQueryClient();
    const ff_1132 = useFeatureFlagsStore()('ff-1132');

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
        };

        if (selectedNotification?.id) {
            updateNotificationMutation.mutate(updatedFormData);
        } else {
            createNotificationMutation.mutate(updatedFormData);
        }

        closeEditDialog();
    }

    const columnHelper = createColumnHelper<Notification>();

    const columns = [
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
    ];

    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: emptyFormValues,
        resolver: zodResolver(formSchema),
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

            setNotificationType(selectedNotification.type || NotificationTypeEnum.Email);
        } else {
            form.reset(emptyFormValues);

            setNotificationType(NotificationTypeEnum.Email);
        }
    }, [selectedNotification, form]);

    return {
        closeDeleteDialog,
        closeEditDialog,
        columns,
        deleteNotificationMutation,
        ff_1132,
        form,
        handleDeleteNotification,
        isDeleteDialogOpen,
        isEditDialogOpen,
        isNotificationEventsLoading,
        isNotificationsLoading,
        notificationEvents,
        notificationType,
        notificationsData,
        notificationsError,
        openDeleteDialog,
        openEditDialog,
        queryClient,
        saveNotification,
        selectedNotification,
        setNotificationType,
        setSelectedNotification,
    };
}
