import {
    filterIntegrationComponentElements,
    getToolName,
} from '@/ee/pages/embedded/integrations/components/integration-list/integrationComponentElementUtils';
import {describe, expect, it} from 'vitest';

describe('getToolName', () => {
    it('matches the server MCP tool naming', () => {
        expect(getToolName('gmail', 'sendEmail')).toBe('GMAIL_SEND_EMAIL');
        expect(getToolName('accelo', 'createCompany')).toBe('ACCELO_CREATE_COMPANY');
        expect(getToolName('slack', 'send')).toBe('SLACK_SEND');
    });

    it('keeps digits and existing underscores without adding separators', () => {
        expect(getToolName('microsoftOutlook365', 'getMail2')).toBe('MICROSOFTOUTLOOK365_GET_MAIL2');
        expect(getToolName('google_sheets', 'insertRow')).toBe('GOOGLE_SHEETS_INSERT_ROW');
    });
});

describe('filterIntegrationComponentElements', () => {
    const elements = [
        {description: 'Sends an email', id: 'GMAIL_SEND_EMAIL', title: 'Send Email'},
        {description: 'Lists labels', id: 'GMAIL_LIST_LABELS', title: 'List Labels'},
    ];

    it('returns every element for blank search text', () => {
        expect(filterIntegrationComponentElements(elements, '  ')).toEqual(elements);
    });

    it('matches title, id and description case-insensitively', () => {
        expect(filterIntegrationComponentElements(elements, 'send')).toEqual([elements[0]]);
        expect(filterIntegrationComponentElements(elements, 'list_labels')).toEqual([elements[1]]);
        expect(filterIntegrationComponentElements(elements, 'AN EMAIL')).toEqual([elements[0]]);
    });
});
