import {ComponentDefinition} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import resolveOperationNameForVersion from './resolveOperationNameForVersion';

const componentDefinition = {
    actions: [{name: 'getMessage'}, {name: 'sendMessage'}],
    clusterElements: [
        {name: 'openAiModel', type: 'MODEL'},
        {name: 'openAiEmbedding', type: 'EMBEDDING'},
    ],
    name: 'test',
    triggers: [{name: 'newMessage'}, {name: 'newReaction'}],
    version: 2,
} as unknown as ComponentDefinition;

describe('resolveOperationNameForVersion', () => {
    it('should keep the current action when the new version still declares it', () => {
        expect(
            resolveOperationNameForVersion({
                componentDefinition,
                currentOperationName: 'sendMessage',
            })
        ).toBe('sendMessage');
    });

    it('should fall back to the first action when the new version dropped the current one', () => {
        expect(
            resolveOperationNameForVersion({
                componentDefinition,
                currentOperationName: 'removedAction',
            })
        ).toBe('getMessage');
    });

    it('should resolve against triggers for a trigger node', () => {
        expect(
            resolveOperationNameForVersion({
                componentDefinition,
                currentOperationName: 'newReaction',
                trigger: true,
            })
        ).toBe('newReaction');
    });

    it('should fall back to the first trigger when the new version dropped the current one', () => {
        expect(
            resolveOperationNameForVersion({
                componentDefinition,
                currentOperationName: 'sendMessage',
                trigger: true,
            })
        ).toBe('newMessage');
    });

    it('should resolve against cluster elements of the matching type', () => {
        expect(
            resolveOperationNameForVersion({
                clusterElementType: 'model',
                componentDefinition,
                currentOperationName: 'removedElement',
            })
        ).toBe('openAiModel');
    });

    it('should return undefined when the new version declares no matching operations', () => {
        expect(
            resolveOperationNameForVersion({
                componentDefinition: {name: 'test', version: 2} as ComponentDefinition,
                currentOperationName: 'sendMessage',
            })
        ).toBeUndefined();
    });
});
