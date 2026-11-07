import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {Notification, NotificationEvent} from '@/shared/middleware/platform/notification';
import {EditIcon, Trash2Icon} from 'lucide-react';

export const EventsCell = ({notificationEvents}: {notificationEvents: NotificationEvent[]}) => (
    <div className="space-y-1">
        {notificationEvents.map((event) =>
            event.type ? (
                <Badge
                    className="ml-1"
                    key={event.id}
                    label={event.type}
                    styleType="secondary-filled"
                    weight="semibold"
                />
            ) : null
        )}
    </div>
);

interface ActionsCellProps {
    openEditDialog: (notification: Notification) => void;
    openDeleteDialog: (notification: Notification) => void;
    notification: Notification;
}

export const ActionsCell = ({notification, openDeleteDialog, openEditDialog}: ActionsCellProps) => (
    <div className="flex justify-center space-x-1">
        <Button
            icon={<EditIcon className="size-4" />}
            onClick={() => openEditDialog(notification)}
            size="icon"
            title="Edit notification"
            variant="ghost"
        />

        <Button
            icon={<Trash2Icon className="h-4 text-destructive" />}
            onClick={() => openDeleteDialog(notification)}
            size="icon"
            title="Delete notification"
            variant="ghost"
        />
    </div>
);
