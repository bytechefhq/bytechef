import {NotificationTypeEnum} from '@/shared/middleware/platform/notification';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {useForm} from 'react-hook-form';
import {describe, expect, it, vi} from 'vitest';

import {NotificationFormValuesType} from '../../hooks/useNotifications';
import NotificationDialog from '../NotificationDialog';

vi.mock('../../hooks/useNotifications', () => ({
    default: () => ({
        ff_1132: true,
        isNotificationEventsLoading: false,
        notificationEvents: [],
        openEditDialog: vi.fn(),
    }),
}));

interface NotificationDialogHarnessProps {
    saveNotification: (data: NotificationFormValuesType) => void;
    settings?: Record<string, unknown>;
    type: NotificationTypeEnum;
}

function NotificationDialogHarness({saveNotification, settings = {}, type}: NotificationDialogHarnessProps) {
    const form = useForm<NotificationFormValuesType>({
        defaultValues: {
            name: 'Failures',
            notificationEventIds: ['1'],
            settings,
            type,
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
    it('saves the Slack webhook URL under settings.slackWebhookUrl for a Slack notification', async () => {
        const saveNotification = vi.fn();

        render(<NotificationDialogHarness saveNotification={saveNotification} type={NotificationTypeEnum.Slack} />);

        await userEvent.type(screen.getByLabelText('Slack Webhook URL'), 'https://hooks.slack.com/services/T/B/X');
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(saveNotification).toHaveBeenCalledWith(
            expect.objectContaining({settings: {slackWebhookUrl: 'https://hooks.slack.com/services/T/B/X'}}),
            expect.anything()
        );
    });

    it('does not show the Slack webhook URL field for an email notification', () => {
        render(<NotificationDialogHarness saveNotification={vi.fn()} type={NotificationTypeEnum.Email} />);

        expect(screen.queryByLabelText('Slack Webhook URL')).not.toBeInTheDocument();
    });

    it('shows the saved Slack webhook URL when an existing Slack notification is opened', () => {
        render(
            <NotificationDialogHarness
                saveNotification={vi.fn()}
                settings={{slackWebhookUrl: 'https://hooks.slack.com/services/T/B/X'}}
                type={NotificationTypeEnum.Slack}
            />
        );

        expect(screen.getByLabelText('Slack Webhook URL')).toHaveValue('https://hooks.slack.com/services/T/B/X');
        expect(screen.queryByLabelText('Email')).not.toBeInTheDocument();
    });
});
