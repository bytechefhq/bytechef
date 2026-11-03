import {Notification, NotificationTestResult, NotificationTypeEnum} from '@/shared/middleware/platform/notification';
import {EditionType, applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {zodResolver} from '@hookform/resolvers/zod';
import {act, fireEvent, render, screen, waitFor} from '@testing-library/react';
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
    onRegenerateSigningSecret?: (notificationId: number) => void;
    saveNotification: (data: NotificationFormValuesType) => void;
    selectedNotification?: Notification;
    settings?: Record<string, unknown>;
    testNotification?: () => Promise<NotificationTestResult>;
    type: NotificationTypeEnum;
}

function NotificationDialogHarness({
    onRegenerateSigningSecret,
    saveNotification,
    selectedNotification,
    settings = {},
    testNotification = vi.fn(),
    type,
}: NotificationDialogHarnessProps) {
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
            onRegenerateSigningSecret={onRegenerateSigningSecret}
            saveNotification={saveNotification}
            selectedNotification={selectedNotification}
            testNotification={testNotification}
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

    it('offers the signing secret button only when the stored notification is a Webhook', () => {
        const {unmount} = render(
            <NotificationDialogHarness
                onRegenerateSigningSecret={vi.fn()}
                saveNotification={vi.fn()}
                selectedNotification={{id: 7, name: 'Failures', type: NotificationTypeEnum.Email} as Notification}
                settings={{webhook: 'https://example.com/ingest'}}
                type={NotificationTypeEnum.Webhook}
            />
        );

        expect(screen.queryByRole('button', {name: 'Generate signing secret'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Regenerate signing secret'})).not.toBeInTheDocument();

        unmount();

        render(
            <NotificationDialogHarness
                onRegenerateSigningSecret={vi.fn()}
                saveNotification={vi.fn()}
                selectedNotification={
                    {
                        hasSigningSecret: true,
                        id: 7,
                        name: 'Failures',
                        type: NotificationTypeEnum.Webhook,
                    } as Notification
                }
                settings={{webhook: 'https://example.com/ingest'}}
                type={NotificationTypeEnum.Webhook}
            />
        );

        expect(screen.getByRole('button', {name: 'Regenerate signing secret'})).toBeInTheDocument();
    });

    it('requires a value after a stored header is renamed', async () => {
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

        await userEvent.type(screen.getByLabelText('Header name'), '2');
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(await screen.findByText('Enter a value')).toBeInTheDocument();
        expect(saveNotification).not.toHaveBeenCalled();
    });

    it('sends the current unsaved values on Test', async () => {
        const testNotification = vi.fn().mockResolvedValue({signed: false, success: true});

        render(
            <NotificationDialogHarness
                saveNotification={vi.fn()}
                testNotification={testNotification}
                type={NotificationTypeEnum.Slack}
            />
        );

        await userEvent.type(screen.getByLabelText('Slack Webhook URL'), 'https://hooks.slack.com/services/T/B/X');
        await userEvent.click(screen.getByRole('button', {name: 'Test'}));

        expect(await screen.findByText('Test sent')).toBeInTheDocument();
        expect(testNotification).toHaveBeenCalledTimes(1);
    });

    it('disables Test while a test is running', async () => {
        const testNotification = vi.fn(() => new Promise<NotificationTestResult>(() => {}));

        render(
            <NotificationDialogHarness
                saveNotification={vi.fn()}
                settings={{slackWebhookUrl: 'https://hooks.slack.com/services/T/B/X'}}
                testNotification={testNotification}
                type={NotificationTypeEnum.Slack}
            />
        );

        await userEvent.click(screen.getByRole('button', {name: 'Test'}));

        await waitFor(() => expect(screen.getByRole('button', {name: 'Test'})).toBeDisabled());
    });

    it('does not send a test when the form is invalid', async () => {
        const testNotification = vi.fn();

        render(
            <NotificationDialogHarness
                saveNotification={vi.fn()}
                testNotification={testNotification}
                type={NotificationTypeEnum.Webhook}
            />
        );

        await userEvent.click(screen.getByRole('button', {name: 'Test'}));

        expect(await screen.findByText('Webhook URL is required.')).toBeInTheDocument();
        expect(testNotification).not.toHaveBeenCalled();
    });

    it('shows the unsigned note for an unsaved webhook', async () => {
        const testNotification = vi.fn().mockResolvedValue({signed: false, statusCode: 200, success: true});

        render(
            <NotificationDialogHarness
                saveNotification={vi.fn()}
                settings={{webhook: 'https://example.com/ingest'}}
                testNotification={testNotification}
                type={NotificationTypeEnum.Webhook}
            />
        );

        await userEvent.click(screen.getByRole('button', {name: 'Test'}));

        expect(await screen.findByText('HTTP 200')).toBeInTheDocument();
        expect(
            screen.getByText('Sent without a signature; save the notification to get a signing secret.')
        ).toBeInTheDocument();
    });

    it('clears the result when the type changes', async () => {
        const testNotification = vi.fn().mockResolvedValue({signed: false, success: true});

        render(
            <NotificationDialogHarness
                saveNotification={vi.fn()}
                settings={{slackWebhookUrl: 'https://hooks.slack.com/services/T/B/X'}}
                testNotification={testNotification}
                type={NotificationTypeEnum.Slack}
            />
        );

        await userEvent.click(screen.getByRole('button', {name: 'Test'}));

        expect(await screen.findByText('Test sent')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('combobox'));
        await userEvent.click(screen.getByRole('option', {name: NotificationTypeEnum.Email}));

        expect(screen.queryByText('Test sent')).not.toBeInTheDocument();
    });

    it('drops a result that resolves after the type changed', async () => {
        let resolveTest: (result: NotificationTestResult) => void = () => {};

        const testNotification = vi.fn(
            () =>
                new Promise<NotificationTestResult>((resolve) => {
                    resolveTest = resolve;
                })
        );

        render(
            <NotificationDialogHarness
                saveNotification={vi.fn()}
                settings={{slackWebhookUrl: 'https://hooks.slack.com/services/T/B/X'}}
                testNotification={testNotification}
                type={NotificationTypeEnum.Slack}
            />
        );

        await userEvent.click(screen.getByRole('button', {name: 'Test'}));
        await userEvent.click(screen.getByRole('combobox'));
        await userEvent.click(screen.getByRole('option', {name: NotificationTypeEnum.Email}));

        await act(async () => {
            resolveTest({signed: false, success: true});
        });

        expect(screen.queryByText('Test sent')).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Test'})).toBeEnabled();
    });

    it('clears the result when the test request is rejected', async () => {
        const testNotification = vi
            .fn()
            .mockResolvedValueOnce({signed: false, success: true})
            .mockRejectedValueOnce(new Error('Bad request'));

        render(
            <NotificationDialogHarness
                saveNotification={vi.fn()}
                settings={{slackWebhookUrl: 'https://hooks.slack.com/services/T/B/X'}}
                testNotification={testNotification}
                type={NotificationTypeEnum.Slack}
            />
        );

        await userEvent.click(screen.getByRole('button', {name: 'Test'}));

        expect(await screen.findByText('Test sent')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Test'}));

        await waitFor(() => expect(screen.queryByText('Test sent')).not.toBeInTheDocument());
    });

    it('clears the result when the settings change', async () => {
        const testNotification = vi.fn().mockResolvedValue({signed: false, success: true});

        render(
            <NotificationDialogHarness
                saveNotification={vi.fn()}
                settings={{slackWebhookUrl: 'https://hooks.slack.com/services/T/B/X'}}
                testNotification={testNotification}
                type={NotificationTypeEnum.Slack}
            />
        );

        await userEvent.click(screen.getByRole('button', {name: 'Test'}));

        expect(await screen.findByText('Test sent')).toBeInTheDocument();

        await userEvent.type(screen.getByLabelText('Slack Webhook URL'), 'Y');

        expect(screen.queryByText('Test sent')).not.toBeInTheDocument();
    });

    it('requires the header values again when the URL of a saved webhook changes', async () => {
        const saveNotification = vi.fn();
        const settings = {
            webhook: 'https://example.com/ingest',
            webhookHeaders: [{name: 'Authorization', value: '********'}],
        };

        render(
            <NotificationDialogHarness
                saveNotification={saveNotification}
                selectedNotification={
                    {
                        hasSigningSecret: true,
                        id: 7,
                        name: 'Failures',
                        settings,
                        type: NotificationTypeEnum.Webhook,
                    } as unknown as Notification
                }
                settings={settings}
                type={NotificationTypeEnum.Webhook}
            />
        );

        await userEvent.type(screen.getByLabelText('Webhook URL'), '/v2');
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(await screen.findByText('Enter a value')).toBeInTheDocument();
        expect(saveNotification).not.toHaveBeenCalled();
    });

    it('keeps masked header values when the URL differs only in host case and default port', async () => {
        const saveNotification = vi.fn();
        const settings = {
            webhook: 'https://example.com/ingest',
            webhookHeaders: [{name: 'Authorization', value: '********'}],
        };

        render(
            <NotificationDialogHarness
                saveNotification={saveNotification}
                selectedNotification={
                    {
                        hasSigningSecret: true,
                        id: 7,
                        name: 'Failures',
                        settings,
                        type: NotificationTypeEnum.Webhook,
                    } as unknown as Notification
                }
                settings={settings}
                type={NotificationTypeEnum.Webhook}
            />
        );

        fireEvent.change(screen.getByLabelText('Webhook URL'), {target: {value: ' HTTPS://Example.COM:443/ingest'}});
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() =>
            expect(saveNotification).toHaveBeenCalledWith(
                expect.objectContaining({
                    settings: expect.objectContaining({
                        webhookHeaders: [{name: 'Authorization', value: '********'}],
                    }),
                }),
                expect.anything()
            )
        );
    });

    describe('webhook presets', () => {
        const DATADOG_URL = 'https://event-management-intake.datadoghq.com/api/v2/events';

        async function pickPreset(label: string) {
            await userEvent.click(screen.getByRole('combobox', {name: 'Preset'}));
            await userEvent.click(screen.getByRole('option', {name: label}));
        }

        it('fills the form when Datadog is picked on an empty Webhook form', async () => {
            render(<NotificationDialogHarness saveNotification={vi.fn()} type={NotificationTypeEnum.Webhook} />);

            await pickPreset('Datadog');

            expect(screen.getByLabelText('Webhook URL')).toHaveValue(DATADOG_URL);

            const headerNames = screen.getAllByLabelText('Header name');
            const headerValues = screen.getAllByLabelText('Header value');

            expect(headerNames.map((input) => (input as HTMLInputElement).value)).toEqual([
                'DD-API-KEY',
                'DD-APPLICATION-KEY',
            ]);
            expect(headerValues.map((input) => (input as HTMLInputElement).value)).toEqual(['', '']);
            expect((screen.getByLabelText('Body template') as HTMLTextAreaElement).value).toContain(
                '"category": "alert"'
            );
        });

        it('keeps every field editable after a preset is applied', async () => {
            render(<NotificationDialogHarness saveNotification={vi.fn()} type={NotificationTypeEnum.Webhook} />);

            await pickPreset('Datadog');
            await userEvent.type(screen.getByLabelText('Webhook URL'), '/extra');

            expect(screen.getByLabelText('Webhook URL')).toHaveValue(`${DATADOG_URL}/extra`);
        });

        it('asks for confirmation before replacing a non-empty form', async () => {
            render(
                <NotificationDialogHarness
                    saveNotification={vi.fn()}
                    settings={{webhook: 'https://example.com/ingest', webhookHeaders: []}}
                    type={NotificationTypeEnum.Webhook}
                />
            );

            await pickPreset('New Relic');

            expect(screen.getByText('Replace the URL, headers and body template?')).toBeInTheDocument();

            await userEvent.click(screen.getByRole('button', {name: 'Cancel'}));

            expect(screen.getByLabelText('Webhook URL')).toHaveValue('https://example.com/ingest');

            await pickPreset('New Relic');
            await userEvent.click(screen.getByRole('button', {name: 'Replace'}));

            expect(screen.getByLabelText('Webhook URL')).toHaveValue(
                'https://insights-collector.newrelic.com/v1/accounts/YOUR_ACCOUNT_ID/events'
            );
            expect(screen.getByLabelText('Header name')).toHaveValue('Api-Key');
        });

        it('asks for confirmation when only the body template is filled', async () => {
            render(
                <NotificationDialogHarness
                    saveNotification={vi.fn()}
                    settings={{webhookBodyTemplate: 'plain'}}
                    type={NotificationTypeEnum.Webhook}
                />
            );

            await pickPreset('Custom');

            expect(screen.getByText('Replace the URL, headers and body template?')).toBeInTheDocument();
        });

        it('applies Custom immediately on an empty form and leaves it empty', async () => {
            render(<NotificationDialogHarness saveNotification={vi.fn()} type={NotificationTypeEnum.Webhook} />);

            await pickPreset('Custom');

            expect(screen.queryByText('Replace the URL, headers and body template?')).not.toBeInTheDocument();
            expect(screen.getByLabelText('Webhook URL')).toHaveValue('');
            expect(screen.queryByLabelText('Header name')).not.toBeInTheDocument();
            expect(screen.getByLabelText('Body template')).toHaveValue('');
        });

        it('replaces masked header rows of a saved webhook with the preset rows', async () => {
            const settings = {
                webhook: 'https://example.com/ingest',
                webhookHeaders: [{name: 'Authorization', value: '********'}],
            };

            render(
                <NotificationDialogHarness
                    saveNotification={vi.fn()}
                    selectedNotification={
                        {
                            hasSigningSecret: true,
                            id: 7,
                            name: 'Failures',
                            settings,
                            type: NotificationTypeEnum.Webhook,
                        } as unknown as Notification
                    }
                    settings={settings}
                    type={NotificationTypeEnum.Webhook}
                />
            );

            await pickPreset('Datadog');
            await userEvent.click(screen.getByRole('button', {name: 'Replace'}));

            const headerNames = screen.getAllByLabelText('Header name');
            const headerValues = screen.getAllByLabelText('Header value');

            expect(headerNames.map((input) => (input as HTMLInputElement).value)).toEqual([
                'DD-API-KEY',
                'DD-APPLICATION-KEY',
            ]);
            expect(headerValues.map((input) => (input as HTMLInputElement).value)).toEqual(['', '']);
            expect(screen.queryByDisplayValue('********')).not.toBeInTheDocument();
        });

        it('does not send the preset selection to saveNotification', async () => {
            const saveNotification = vi.fn();

            render(
                <NotificationDialogHarness saveNotification={saveNotification} type={NotificationTypeEnum.Webhook} />
            );

            await pickPreset('Datadog');

            const headerValues = screen.getAllByLabelText('Header value');

            await userEvent.type(headerValues[0], 'api');
            await userEvent.type(headerValues[1], 'app');
            await userEvent.click(screen.getByRole('button', {name: 'Save'}));

            expect(saveNotification).toHaveBeenCalledTimes(1);

            const savedValues = saveNotification.mock.calls[0][0] as Record<string, unknown>;

            expect(JSON.stringify(savedValues)).not.toMatch(/preset/i);
            expect(Object.keys(savedValues)).not.toContain('preset');
            expect(Object.keys(savedValues.settings as Record<string, unknown>)).not.toContain('preset');
        });

        it('blocks Save with Enter a value until the header values are filled', async () => {
            const saveNotification = vi.fn();

            render(
                <NotificationDialogHarness saveNotification={saveNotification} type={NotificationTypeEnum.Webhook} />
            );

            await pickPreset('Datadog');
            await userEvent.click(screen.getByRole('button', {name: 'Save'}));

            expect((await screen.findAllByText('Enter a value')).length).toBeGreaterThan(0);
            expect(saveNotification).not.toHaveBeenCalled();
        });
    });
});
