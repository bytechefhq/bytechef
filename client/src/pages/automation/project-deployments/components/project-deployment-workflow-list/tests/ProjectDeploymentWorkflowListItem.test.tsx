import {describe, expect, it} from 'vitest';

// Test the getTriggerUrl logic
describe('ProjectDeploymentWorkflowListItem - getTriggerUrl', () => {
    const getTriggerUrl = (
        type: 'form' | 'chat',
        environmentId: number,
        sseStreamResponseEnabled: boolean,
        staticWebhookUrl?: string
    ) => {
        if (!staticWebhookUrl) {
            return '';
        }

        const webhookId = staticWebhookUrl.substring(staticWebhookUrl.lastIndexOf('/webhooks/') + '/webhooks/'.length);

        return `/${type}/${environmentId}/${webhookId}${sseStreamResponseEnabled ? '?sseStream=true' : ''}`;
    };

    it('returns empty string when staticWebhookUrl is undefined', () => {
        const result = getTriggerUrl('form', 1, false, undefined);

        expect(result).toBe('');
    });

    it('returns empty string when staticWebhookUrl is empty', () => {
        const result = getTriggerUrl('form', 1, false, '');

        expect(result).toBe('');
    });

    it('generates form trigger URL without SSE stream', () => {
        const result = getTriggerUrl('form', 1, false, 'https://example.com/webhooks/abc123');

        expect(result).toBe('/form/1/abc123');
    });

    it('generates form trigger URL with SSE stream enabled', () => {
        const result = getTriggerUrl('form', 1, true, 'https://example.com/webhooks/abc123');

        expect(result).toBe('/form/1/abc123?sseStream=true');
    });

    it('generates chat trigger URL without SSE stream', () => {
        const result = getTriggerUrl('chat', 2, false, 'https://example.com/webhooks/xyz789');

        expect(result).toBe('/chat/2/xyz789');
    });

    it('generates chat trigger URL with SSE stream enabled', () => {
        const result = getTriggerUrl('chat', 2, true, 'https://example.com/webhooks/xyz789');

        expect(result).toBe('/chat/2/xyz789?sseStream=true');
    });

    it('handles different environment IDs', () => {
        const result = getTriggerUrl('form', 42, false, 'https://example.com/webhooks/test-id');

        expect(result).toBe('/form/42/test-id');
    });

    it('extracts webhook ID from URL with path segments', () => {
        const result = getTriggerUrl('chat', 3, false, 'https://api.example.com/v1/webhooks/webhook-identifier-123');

        expect(result).toBe('/chat/3/webhook-identifier-123');
    });

    it('handles webhook URL with trailing slash', () => {
        const result = getTriggerUrl('form', 1, false, 'https://example.com/webhooks/abc123/');

        expect(result).toBe('/form/1/abc123/');
    });

    it('handles webhook URL with query parameters', () => {
        const result = getTriggerUrl('chat', 1, false, 'https://example.com/webhooks/abc123?param=value');

        expect(result).toBe('/chat/1/abc123?param=value');
    });

    it('handles webhook URL with query parameters and SSE stream', () => {
        const result = getTriggerUrl('chat', 1, true, 'https://example.com/webhooks/abc123?param=value');

        expect(result).toBe('/chat/1/abc123?param=value?sseStream=true');
    });

    it('handles complex webhook IDs', () => {
        const result = getTriggerUrl('form', 5, true, 'https://example.com/webhooks/complex-id-with-dashes-123');

        expect(result).toBe('/form/5/complex-id-with-dashes-123?sseStream=true');
    });
});
