import {getPageUrl} from '@/pages/automation/project-deployments/components/project-deployment-workflow-list/util/pageUrl-utils';
import {describe, expect, it} from 'vitest';

// Test the getPageUrl logic
describe('getPageUrl', () => {
    it('returns empty string when staticWebhookUrl is undefined', () => {
        const result = getPageUrl('form', 1, undefined);

        expect(result).toBe('');
    });

    it('returns empty string when staticWebhookUrl is empty', () => {
        const result = getPageUrl('form', 1, '');

        expect(result).toBe('');
    });

    it('generates form trigger URL', () => {
        const result = getPageUrl('form', 1, 'https://example.com/webhooks/abc123');

        expect(result).toBe('/form/1/abc123');
    });

    it('generates chat trigger URL', () => {
        const result = getPageUrl('chat', undefined, 'https://example.com/webhooks/xyz789');

        expect(result).toBe('/automation/chat/xyz789');
    });

    it('handles different environment IDs', () => {
        const result = getPageUrl('form', 42, 'https://example.com/webhooks/test-id');

        expect(result).toBe('/form/42/test-id');
    });

    it('extracts webhook ID from URL with path segments', () => {
        const result = getPageUrl('chat', undefined, 'https://api.example.com/v1/webhooks/webhook-identifier-123');

        expect(result).toBe('/automation/chat/webhook-identifier-123');
    });

    it('handles webhook URL with trailing slash', () => {
        const result = getPageUrl('form', 1, 'https://example.com/webhooks/abc123/');

        expect(result).toBe('/form/1/abc123/');
    });

    it('handles webhook URL with query parameters', () => {
        const result = getPageUrl('chat', 1, 'https://example.com/webhooks/abc123?param=value');

        expect(result).toBe('/automation/chat/abc123?param=value');
    });

    it('handles complex webhook IDs', () => {
        const result = getPageUrl('form', 5, 'https://example.com/webhooks/complex-id-with-dashes-123');

        expect(result).toBe('/form/5/complex-id-with-dashes-123');
    });
});
