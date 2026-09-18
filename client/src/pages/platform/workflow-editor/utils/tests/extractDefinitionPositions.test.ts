import {describe, expect, it} from 'vitest';

import extractDefinitionPositions from '../extractDefinitionPositions';

describe('extractDefinitionPositions', () => {
    it('collects saved positions for every trigger by name', () => {
        const definition = JSON.stringify({
            tasks: [],
            triggers: [
                {metadata: {ui: {nodePosition: {x: 1, y: 2}}}, name: 'trigger_1', type: 'webhook/v1/onReceive'},
                {metadata: {ui: {nodePosition: {x: 3, y: 4}}}, name: 'trigger_2', type: 'schedule/v1/onInterval'},
            ],
        });

        const positions = extractDefinitionPositions(definition);

        expect(positions.get('trigger_1')).toEqual({x: 1, y: 2});
        expect(positions.get('trigger_2')).toEqual({x: 3, y: 4});
    });
});
