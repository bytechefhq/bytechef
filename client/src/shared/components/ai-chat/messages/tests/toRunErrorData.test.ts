import {toRunErrorData} from '@/shared/components/ai-chat/messages/toRunErrorData';
import {describe, expect, it} from 'vitest';

describe('toRunErrorData', () => {
    it('explains a missing AI provider and where to enable one', () => {
        expect(
            toRunErrorData({code: 'NO_AI_PROVIDER', message: 'No AI provider is enabled in environment 1.'})
        ).toEqual({
            code: 'NO_AI_PROVIDER',
            message:
                'No AI provider is set up for this environment. Enable one in Settings > AI > Providers. If you do not have permission, ask an administrator.',
        });
    });

    it('explains that the user lacks the permission for the turn', () => {
        expect(toRunErrorData({code: 'ACCESS_DENIED'})).toEqual({
            code: 'ACCESS_DENIED',
            message:
                "You don't have permission to do this in this workspace. Ask a workspace administrator for access.",
        });
    });

    it('asks the user to pick another model when the selected one is unavailable', () => {
        expect(
            toRunErrorData({
                code: 'SELECTED_MODEL_UNAVAILABLE',
                message: 'The selected model gpt-x (openai) is not available in environment 1.',
            })
        ).toEqual({
            code: 'SELECTED_MODEL_UNAVAILABLE',
            message: 'The model you picked is no longer available. Pick another model in the model picker.',
        });
    });

    it('asks the user to pick a model again when the selection is incomplete', () => {
        expect(
            toRunErrorData({
                code: 'MODEL_SELECTION_INCOMPLETE',
                message: 'The model selection is incomplete (provider=openai, model=null). Pick a model again.',
            })
        ).toEqual({
            code: 'MODEL_SELECTION_INCOMPLETE',
            message: 'Your model selection is incomplete. Pick a model again.',
        });
    });

    it('humanizes the message of an unknown code and drops the code', () => {
        expect(
            toRunErrorData({code: 'SOMETHING_NEW', message: 'java.lang.IllegalStateException: catalog down'})
        ).toEqual({message: 'catalog down'});
    });

    it('humanizes the message when there is no code', () => {
        expect(toRunErrorData({message: 'java.lang.IllegalStateException: catalog down'})).toEqual({
            message: 'catalog down',
        });
    });

    it('falls back to a generic message when there is neither a code nor a message', () => {
        expect(toRunErrorData({message: '   '})).toEqual({
            message: 'The agent run failed before completing this turn.',
        });
    });
});
