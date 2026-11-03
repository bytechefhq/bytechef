import {NotificationTypeEnum} from '@/shared/middleware/platform/notification';
import {EditionType, applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {zodResolver} from '@hookform/resolvers/zod';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {useForm} from 'react-hook-form';
import {beforeAll, beforeEach, describe, expect, it, vi} from 'vitest';

import {NotificationFormValuesType, notificationFormSchema} from '../../hooks/useNotifications';
import NotificationDialog from '../NotificationDialog';

vi.mock('../../hooks/useNotifications', async (importOriginal) => ({
    ...(await importOriginal<typeof import('../../hooks/useNotifications')>()),
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
        resolver: zodResolver(notificationFormSchema),
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

    it('does not save a Webhook notification without a URL', async () => {
        const saveNotification = vi.fn();

        render(<NotificationDialogHarness saveNotification={saveNotification} type={NotificationTypeEnum.Webhook} />);

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(await screen.findByText('Webhook URL is required.')).toBeInTheDocument();
        expect(saveNotification).not.toHaveBeenCalled();
    });

    it('saves the Webhook URL under settings.webhook for a Webhook notification', async () => {
        const saveNotification = vi.fn();

        render(<NotificationDialogHarness saveNotification={saveNotification} type={NotificationTypeEnum.Webhook} />);

        await userEvent.type(screen.getByLabelText('Webhook URL'), 'https://example.com/ingest');
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(saveNotification).toHaveBeenCalledWith(
            expect.objectContaining({settings: {webhook: 'https://example.com/ingest', webhookHeaders: []}}),
            expect.anything()
        );
    });

    it('links the Webhook URL field to the payload format documentation', () => {
        render(<NotificationDialogHarness saveNotification={vi.fn()} type={NotificationTypeEnum.Webhook} />);

        expect(screen.getByRole('link', {name: 'See the payload format'})).toHaveAttribute(
            'href',
            'https://docs.bytechef.io/platform/settings/notifications#webhook-requests'
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

    it('shows header and template fields only for Webhook', () => {
        const {unmount} = render(
            <NotificationDialogHarness saveNotification={vi.fn()} type={NotificationTypeEnum.Webhook} />
        );

        expect(screen.getByRole('button', {name: 'Add header'})).toBeInTheDocument();
        expect(screen.getByLabelText('Body template')).toBeInTheDocument();

        unmount();

        render(<NotificationDialogHarness saveNotification={vi.fn()} type={NotificationTypeEnum.Slack} />);

        expect(screen.queryByRole('button', {name: 'Add header'})).not.toBeInTheDocument();
        expect(screen.queryByLabelText('Body template')).not.toBeInTheDocument();
    });

    it('saves webhook headers and body template under settings', async () => {
        const saveNotification = vi.fn();

        render(<NotificationDialogHarness saveNotification={saveNotification} type={NotificationTypeEnum.Webhook} />);

        await userEvent.type(screen.getByLabelText('Webhook URL'), 'https://example.com/ingest');
        await userEvent.click(screen.getByRole('button', {name: 'Add header'}));
        await userEvent.type(screen.getByLabelText('Header name'), 'X-Api-Key');
        await userEvent.type(screen.getByLabelText('Header value'), 'secret');
        await userEvent.type(screen.getByLabelText('Body template'), 'plain');
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(saveNotification).toHaveBeenCalledWith(
            expect.objectContaining({
                settings: {
                    webhook: 'https://example.com/ingest',
                    webhookBodyTemplate: 'plain',
                    webhookHeaders: [{name: 'X-Api-Key', value: 'secret'}],
                },
            }),
            expect.anything()
        );
    });

    it('sends ******** for an untouched masked header value', async () => {
        const saveNotification = vi.fn();

        render(
            <NotificationDialogHarness
                saveNotification={saveNotification}
                settings={{
                    webhook: 'https://example.com/ingest',
                    webhookHeaders: [{name: 'Authorization', value: '********'}],
                }}
                type={NotificationTypeEnum.Webhook}
            />
        );

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(saveNotification).toHaveBeenCalledWith(
            expect.objectContaining({
                settings: expect.objectContaining({webhookHeaders: [{name: 'Authorization', value: '********'}]}),
            }),
            expect.anything()
        );
    });

    it('rejects a reserved header name', async () => {
        const saveNotification = vi.fn();

        render(<NotificationDialogHarness saveNotification={saveNotification} type={NotificationTypeEnum.Webhook} />);

        await userEvent.type(screen.getByLabelText('Webhook URL'), 'https://example.com/ingest');
        await userEvent.click(screen.getByRole('button', {name: 'Add header'}));
        await userEvent.type(screen.getByLabelText('Header name'), 'X-ByteChef-Event');
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(await screen.findByText('This header name is reserved.')).toBeInTheDocument();
        expect(saveNotification).not.toHaveBeenCalled();
    });

    it('keeps a masked Slack URL without failing URL validation', async () => {
        const saveNotification = vi.fn();

        render(
            <NotificationDialogHarness
                saveNotification={saveNotification}
                settings={{slackWebhookUrl: '********'}}
                type={NotificationTypeEnum.Slack}
            />
        );

        expect(screen.getByLabelText('Slack Webhook URL')).toHaveAttribute(
            'placeholder',
            'Saved, leave unchanged to keep'
        );

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(saveNotification).toHaveBeenCalledWith(
            expect.objectContaining({settings: {slackWebhookUrl: '********'}}),
            expect.anything()
        );
    });
});
