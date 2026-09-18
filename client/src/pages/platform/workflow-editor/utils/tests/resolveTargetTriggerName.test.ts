import {NodeDataType} from '@/shared/types';
import {describe, expect, it} from 'vitest';

import resolveTargetTriggerName from '../resolveTargetTriggerName';

describe('resolveTargetTriggerName', () => {
    it('resolves the synthetic Manual placeholder to trigger_1', () => {
        const data = {componentName: 'manual', name: 'manual', operationName: 'manual'} as NodeDataType;

        expect(resolveTargetTriggerName(data)).toBe('trigger_1');
    });

    it('reuses the existing name when dropping on a real trigger (replace in place)', () => {
        const data = {componentName: 'webhook', name: 'trigger_2', operationName: 'onReceive'} as NodeDataType;

        expect(resolveTargetTriggerName(data)).toBe('trigger_2');
    });

    it('does not treat a manual component with a different operation as the placeholder', () => {
        const data = {componentName: 'manual', name: 'trigger_3', operationName: 'somethingElse'} as NodeDataType;

        expect(resolveTargetTriggerName(data)).toBe('trigger_3');
    });
});
