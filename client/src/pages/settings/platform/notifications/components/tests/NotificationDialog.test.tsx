import {NotificationTypeEnum} from '@/shared/middleware/platform/notification';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {useForm} from 'react-hook-form';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {NotificationFormValuesType} from '../../hooks/useNotifications';
import NotificationDialog from '../NotificationDialog';

const {hookState} = vi.hoisted(() => ({
    hookState: {notificationType: 'EMAIL' as string},
}));

vi.mock('../../hooks/useNotifications', () => ({
    default: () => ({
        ff_1132: true,
        isNotificationEventsLoading: false,
        notificationEvents: [],
        notificationType: hookState.notificationType,
        openEditDialog: vi.fn(),
        setNotificationType: vi.fn(),
    }),
}));

function NotificationDialogHarness({saveNotification}: {saveNotification: (data: NotificationFormValuesType) => void}) {
    const form = useForm<NotificationFormValuesType>({
        defaultValues: {
            name: 'Failures',
            notificationEventIds: ['1'],
            settings: {},
            type: NotificationTypeEnum.Slack,
        },
    });

    return (
        <NotificationDialog
            closeEditDialog={vi.fn()}
            form={form}
            isEditDialogOpen
            saveNotification={saveNotification}
        />
    );
}

describe('NotificationDialog', () => {
    beforeEach(() => {
        hookState.notificationType = NotificationTypeEnum.Email;
    });

    it('saves the Slack webhook URL under settings.slackWebhookUrl for a Slack notification', async () => {
        hookState.notificationType = NotificationTypeEnum.Slack;

        const saveNotification = vi.fn();

        render(<NotificationDialogHarness saveNotification={saveNotification} />);

        await userEvent.type(screen.getByLabelText('Slack Webhook URL'), 'https://hooks.slack.com/services/T/B/X');
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(saveNotification).toHaveBeenCalledWith(
            expect.objectContaining({settings: {slackWebhookUrl: 'https://hooks.slack.com/services/T/B/X'}}),
            expect.anything()
        );
    });

    it('does not show the Slack webhook URL field for an email notification', () => {
        render(<NotificationDialogHarness saveNotification={vi.fn()} />);

        expect(screen.queryByLabelText('Slack Webhook URL')).not.toBeInTheDocument();
    });
});
