import {Badge} from '@/components/ui/badge';
import {Button} from '@/components/ui/button';
import {Notification, NotificationEvent} from '@/shared/middleware/platform/configuration';
import {PenIcon, TrashIcon} from 'lucide-react';

export const EventsCell = ({notificationEvents}: {notificationEvents: NotificationEvent[]}) => (
    <div className="space-y-1">
        {notificationEvents.map((event) => (
            <Badge
                className="ml-1 bg-surface-neutral-tertiary font-medium text-content-neutral-primary shadow-none hover:bg-surface-neutral-tertiary"
                key={event.id}
            >
                {event.type}
            </Badge>
        ))}
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
