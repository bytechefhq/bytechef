import {ThreadMessageLike} from '@assistant-ui/react';
import {describe, expect, it} from 'vitest';

import {sanitizeAssistantMessages, stripLeakedToolMarkup} from '../stripLeakedToolMarkup';

describe('stripLeakedToolMarkup', () => {
    it('returns clean text unchanged by reference', () => {
        const text = 'Your workflow is ready. Connect Gmail and Slack to go live.';

        expect(stripLeakedToolMarkup(text)).toBe(text);
    });

    it('removes a complete function_calls block, keeping surrounding prose', () => {
        const text =
            "I'll build this workflow for you.\n\n" +
            '<function_calls> <invoke name="project_workflow_agent"> <parameter name="request">Create a workflow</parameter> </invoke> </function_calls>\n\n' +
            'Done!';

        const result = stripLeakedToolMarkup(text);

        expect(result).not.toContain('function_calls');
        expect(result).not.toContain('project_workflow_agent');
        expect(result).not.toContain('parameter');
        expect(result).toContain("I'll build this workflow for you.");
        expect(result).toContain('Done!');
    });

    it('removes an unterminated block still streaming in', () => {
        const text =
            'Now let me create a setup guide: ' +
            '<function_calls> <invoke name="createAssetFile"> <parameter name="content"># Guide\n\nlots of text';

        const result = stripLeakedToolMarkup(text);

        expect(result).toBe('Now let me create a setup guide:');
    });

    it('strips a bare invoke block with a leaked {{placeholder}} inside', () => {
        const text =
            'Opening it now. <invoke name="openWorkflowTab"> <parameter name="workflowId">{{workflowId}}</parameter> </invoke>';

        const result = stripLeakedToolMarkup(text);

        expect(result).toBe('Opening it now.');
        expect(result).not.toContain('{{workflowId}}');
    });

    it('handles the antml-prefixed variant', () => {
        const text = 'Text <function_calls><invoke name="x"></invoke></function_calls> after';

        const result = stripLeakedToolMarkup(text);

        expect(result).not.toContain('function_calls');
        expect(result).not.toContain('invoke');
        expect(result).toContain('Text');
        expect(result).toContain('after');
    });
});

describe('sanitizeAssistantMessages', () => {
    it('returns the same array reference when nothing needs stripping', () => {
        const messages: ThreadMessageLike[] = [
            {content: 'Hello', role: 'user'},
            {content: 'Hi there', role: 'assistant'},
        ];

        expect(sanitizeAssistantMessages(messages)).toBe(messages);
    });

    it('only rewrites the assistant messages that contained leaked markup', () => {
        const userMessage: ThreadMessageLike = {content: 'build it', role: 'user'};
        const cleanAssistant: ThreadMessageLike = {content: 'Sure!', role: 'assistant'};
        const leakedAssistant: ThreadMessageLike = {
            content: 'Working on it. <function_calls><invoke name="a"></invoke></function_calls>',
            role: 'assistant',
        };

        const result = sanitizeAssistantMessages([userMessage, cleanAssistant, leakedAssistant]);

        expect(result).not.toBe([userMessage, cleanAssistant, leakedAssistant]);
        expect(result[0]).toBe(userMessage);
        expect(result[1]).toBe(cleanAssistant);
        expect(result[2].content).toBe('Working on it.');
    });

    it('leaves non-string assistant content untouched', () => {
        const messages: ThreadMessageLike[] = [{content: [{text: 'part', type: 'text'}], role: 'assistant'}];

        expect(sanitizeAssistantMessages(messages)).toBe(messages);
    });
});
