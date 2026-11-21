import {Source} from '@/shared/components/copilot/stores/useCopilotStore';
import {describe, expect, it} from 'vitest';

import getCopilotSuggestions from './getCopilotSuggestions';

const titles = (source?: Source) =>
    getCopilotSuggestions(source).map((suggestion) => (typeof suggestion === 'string' ? suggestion : suggestion.title));

describe('getCopilotSuggestions', () => {
    it('suggests agent questions on the AI Agent page', () => {
        expect(titles(Source.AI_AGENT)).toContain('Explain what this agent');
        expect(titles(Source.AI_AGENT)).not.toContain('Describe what this workflow');
    });

    it('keeps the workflow suggestions for other sources', () => {
        expect(titles(Source.WORKFLOW_EDITOR)).toContain('Describe what this workflow');
        expect(titles(undefined)).toContain('Describe what this workflow');
    });
});
