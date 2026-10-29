import {describe, expect, it} from 'vitest';

import {notificationFormSchema} from '../useNotifications';

const baseValues = {
    name: 'Failures',
    notificationEventIds: ['1'],
};

function slackWebhookUrlIssues(settings: Record<string, unknown>, type: 'EMAIL' | 'SLACK' | 'WEBHOOK' = 'SLACK') {
    const result = notificationFormSchema.safeParse({...baseValues, settings, type});

    if (result.success) {
        return [];
    }

    return result.error.issues
        .filter((issue) => issue.path.join('.') === 'settings.slackWebhookUrl')
        .map((issue) => issue.message);
}

describe('notificationFormSchema', () => {
    it('requires a Slack webhook URL for a Slack notification', () => {
        expect(slackWebhookUrlIssues({})).toEqual(['Slack webhook URL is required.']);
        expect(slackWebhookUrlIssues({slackWebhookUrl: '   '})).toEqual(['Slack webhook URL is required.']);
    });

    it('rejects a Slack webhook URL that is not a URL', () => {
        expect(slackWebhookUrlIssues({slackWebhookUrl: 'not a url'})).toEqual(['Enter a valid Slack webhook URL.']);
    });

    it('accepts a valid Slack webhook URL', () => {
        const result = notificationFormSchema.safeParse({
            ...baseValues,
            settings: {slackWebhookUrl: 'https://hooks.slack.com/services/T/B/X'},
            type: 'SLACK',
        });

        expect(result.success).toBe(true);
    });

    it('does not require a Slack webhook URL for other notification types', () => {
        expect(slackWebhookUrlIssues({email: 'ops@example.com'}, 'EMAIL')).toEqual([]);
        expect(slackWebhookUrlIssues({webhook: 'https://example.com/hook'}, 'WEBHOOK')).toEqual([]);
    });
});
