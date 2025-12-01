import Badge from '@/components/Badge/Badge';
import {Button} from '@/components/ui/button';
import {Notification, NotificationEvent} from '@/shared/middleware/platform/notification';
import {PenIcon, TrashIcon} from 'lucide-react';

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
    <div className="flex space-x-1">
        <Button
            className="text-content-neutral-primary/50 hover:bg-surface-neutral-primary-hover"
            onClick={() => openEditDialog(notification)}
            size="icon"
            title="Edit notification"
            variant="ghost"
        >
            <PenIcon className="size-4 hover:cursor-pointer" />
        </Button>

        <Button
            className="text-content-destructive/50 hover:bg-surface-destructive-secondary hover:text-content-destructive"
            onClick={() => openDeleteDialog(notification)}
            size="icon"
            title="Delete notification"
            variant="ghost"
        >
            <TrashIcon className="size-4 hover:cursor-pointer" />
        </Button>
    </div>
);
