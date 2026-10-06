import {describe, expect, it} from 'vitest';

import getChatTriggerName from './getChatTriggerName';

describe('getChatTriggerName', () => {
    it('returns the name of the chat trigger when it is not trigger_1', () => {
        expect(
            getChatTriggerName({
                triggers: [{name: 'chat_1', type: 'chat/v1/newChatRequest'}],
            })
        ).toBe('chat_1');
    });

    it('picks the chat trigger among several triggers', () => {
        expect(
            getChatTriggerName({
                triggers: [
                    {name: 'schedule_1', type: 'schedule/v1/cron'},
                    {name: 'chat_2', type: 'chat/v2/newChatRequest'},
                ],
            })
        ).toBe('chat_2');
    });

    it('falls back to trigger_1 when the workflow has no chat trigger', () => {
        expect(getChatTriggerName({triggers: [{name: 'schedule_1', type: 'schedule/v1/cron'}]})).toBe('trigger_1');
        expect(getChatTriggerName({})).toBe('trigger_1');
    });
});
