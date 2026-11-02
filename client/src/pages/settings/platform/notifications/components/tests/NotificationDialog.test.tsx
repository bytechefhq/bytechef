import {NotificationTypeEnum} from '@/shared/middleware/platform/notification';
import {EditionType, applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {useForm} from 'react-hook-form';
import {beforeAll, beforeEach, describe, expect, it, vi} from 'vitest';

import {NotificationFormValuesType} from '../../hooks/useNotifications';
import NotificationDialog from '../NotificationDialog';

vi.mock('../../hooks/useNotifications', () => ({
    default: () => ({
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

beforeAll(() => {
    Element.prototype.hasPointerCapture = Element.prototype.hasPointerCapture || vi.fn();
    Element.prototype.releasePointerCapture = Element.prototype.releasePointerCapture || vi.fn();
    Element.prototype.scrollIntoView = Element.prototype.scrollIntoView || vi.fn();
});

describe('NotificationDialog', () => {
    beforeEach(() => {
        applicationInfoStore.setState({application: {edition: EditionType.EE}});
    });

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

    it('offers Email, Webhook and Slack types in the enterprise edition', async () => {
        render(<NotificationDialogHarness saveNotification={vi.fn()} type={NotificationTypeEnum.Email} />);

        await userEvent.click(screen.getByRole('combobox'));

        expect(screen.getByRole('option', {name: NotificationTypeEnum.Email})).toBeInTheDocument();
        expect(screen.getByRole('option', {name: NotificationTypeEnum.Webhook})).toBeInTheDocument();
        expect(screen.getByRole('option', {name: NotificationTypeEnum.Slack})).toBeInTheDocument();
    });

    it('offers Email and Slack but not Webhook in the community edition', async () => {
        applicationInfoStore.setState({application: {edition: EditionType.CE}});

        render(<NotificationDialogHarness saveNotification={vi.fn()} type={NotificationTypeEnum.Email} />);

        await userEvent.click(screen.getByRole('combobox'));

        expect(screen.getByRole('option', {name: NotificationTypeEnum.Email})).toBeInTheDocument();
        expect(screen.getByRole('option', {name: NotificationTypeEnum.Slack})).toBeInTheDocument();
        expect(screen.queryByRole('option', {name: NotificationTypeEnum.Webhook})).not.toBeInTheDocument();
    });
});
