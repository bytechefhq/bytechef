import {describe, expect, it} from 'vitest';

import {findTurnRewindIndexes} from './findTurnRewindIndexes';

const user = {role: 'user'};
const assistant = {role: 'assistant'};
const tool = {role: 'tool'};

const messages = [user, assistant, user, assistant, assistant, assistant];

const agentMessages = [user, assistant, user, assistant, tool, tool, assistant];

describe('findTurnRewindIndexes', () => {
    it('resolves the start of the turn from its last assistant message', () => {
        expect(findTurnRewindIndexes(messages, agentMessages, 5)).toEqual({
            agentUserMessageIndex: 2,
            userMessageIndex: 2,
        });
    });

    it('resolves the start of the turn from a middle assistant message', () => {
        expect(findTurnRewindIndexes(messages, agentMessages, 3)).toEqual({
            agentUserMessageIndex: 2,
            userMessageIndex: 2,
        });
    });

    it('resolves a user message to itself', () => {
        expect(findTurnRewindIndexes(messages, agentMessages, 0)).toEqual({
            agentUserMessageIndex: 0,
            userMessageIndex: 0,
        });
    });

    it('matches the latest turns when the agent transcript only holds them', () => {
        expect(findTurnRewindIndexes(messages, [user, assistant, tool, assistant], 4)).toEqual({
            agentUserMessageIndex: 0,
            userMessageIndex: 2,
        });
    });

    it('returns null when the agent transcript does not reach back to the turn', () => {
        expect(findTurnRewindIndexes(messages, [user, assistant], 1)).toBeNull();
    });

    it('returns null when there is no user message at or before the index', () => {
        expect(findTurnRewindIndexes([assistant, user], [user], 0)).toBeNull();
        expect(findTurnRewindIndexes(messages, agentMessages, -1)).toBeNull();
    });
});
