import {getToolName} from '@/ee/pages/embedded/integrations/components/integration-list/integrationComponentElementUtils';
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
