import {describe, expect, it} from 'vitest';

import {isDataStreamSimpleModeAvailable} from './isDataStreamSimpleModeAvailable';

const WORKFLOW_NODE_NAME = 'dataStream_1';

function definitionWithProcessor(processorValue: unknown): string {
    return JSON.stringify({
        tasks: [
            {
                clusterElements: {processor: processorValue},
                name: WORKFLOW_NODE_NAME,
                type: 'dataStream/v1',
            },
        ],
    });
}

describe('isDataStreamSimpleModeAvailable', () => {
    it('fails open when there is no workflow definition yet', () => {
        expect(isDataStreamSimpleModeAvailable(undefined, WORKFLOW_NODE_NAME)).toBe(true);
    });

    it('fails open when there is no workflow node name', () => {
        expect(isDataStreamSimpleModeAvailable(definitionWithProcessor(undefined), undefined)).toBe(true);
    });

    it('fails open on an unparseable definition', () => {
        expect(isDataStreamSimpleModeAvailable('{not valid json', WORKFLOW_NODE_NAME)).toBe(true);
    });

    it('fails open when the root is missing from the definition', () => {
        const definition = JSON.stringify({tasks: [{clusterElements: {}, name: 'someOtherRoot', type: 'x/v1'}]});

        expect(isDataStreamSimpleModeAvailable(definition, WORKFLOW_NODE_NAME)).toBe(true);
    });

    it('fails open when the root has no processor configured', () => {
        const definition = definitionWithProcessor(undefined);

        expect(isDataStreamSimpleModeAvailable(definition, WORKFLOW_NODE_NAME)).toBe(true);
    });

    it('is available when the processor is the fieldMapper operation, object-shaped', () => {
        const definition = definitionWithProcessor({
            name: 'processor_1',
            type: 'dataStreamProcessor/v1/fieldMapper',
        });

        expect(isDataStreamSimpleModeAvailable(definition, WORKFLOW_NODE_NAME)).toBe(true);
    });

    it('is available when the processor is the fieldMapper operation, array-wrapped', () => {
        const definition = definitionWithProcessor([{name: 'processor_1', type: 'dataStreamProcessor/v1/fieldMapper'}]);

        expect(isDataStreamSimpleModeAvailable(definition, WORKFLOW_NODE_NAME)).toBe(true);
    });

    it('is unavailable when the processor has been hand-configured to a different operation', () => {
        const definition = definitionWithProcessor({
            name: 'processor_1',
            type: 'dataStreamProcessor/v1/customScript',
        });

        expect(isDataStreamSimpleModeAvailable(definition, WORKFLOW_NODE_NAME)).toBe(false);
    });

    it('is unavailable when the processor is a different component entirely', () => {
        const definition = definitionWithProcessor({name: 'processor_1', type: 'someOtherComponent/v1/fieldMapper'});

        expect(isDataStreamSimpleModeAvailable(definition, WORKFLOW_NODE_NAME)).toBe(false);
    });
});
