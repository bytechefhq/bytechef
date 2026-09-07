import {ComponentDefinition} from '@/shared/middleware/platform/configuration';
import {ClusterElementsType} from '@/shared/types';
import {describe, expect, it} from 'vitest';

import {updateNestedClusterElementField} from './clusterElementsFieldChangeUtils';

const currentComponentDefinition = {name: 'openAi', version: 2} as ComponentDefinition;

const currentOperationProperties = [{defaultValue: 'default', name: 'model', type: 'STRING'}] as never;

describe('updateNestedClusterElementField', () => {
    it('should apply the parameters override to a directly nested cluster element', () => {
        const clusterElements: ClusterElementsType = {
            model: {name: 'openAiModel', parameters: {model: 'gpt-4'}, type: 'openAi/v1/model'},
        };

        const updatedClusterElements = updateNestedClusterElementField({
            clusterElements,
            currentComponentDefinition,
            currentOperationProperties,
            elementName: 'openAiModel',
            fieldUpdate: {field: 'operation', value: 'model'},
            parameters: {model: 'gpt-4'},
        });

        expect((updatedClusterElements.model as {parameters: unknown}).parameters).toEqual({model: 'gpt-4'});
    });

    it('should apply the parameters override to a cluster element nested inside another cluster root', () => {
        const clusterElements: ClusterElementsType = {
            tools: [
                {
                    clusterElements: {
                        model: {name: 'openAiModel', parameters: {model: 'gpt-4'}, type: 'openAi/v1/model'},
                    },
                    name: 'nestedAgent',
                    type: 'aiAgent/v1/agent',
                },
            ],
        };

        const updatedClusterElements = updateNestedClusterElementField({
            clusterElements,
            currentComponentDefinition,
            currentOperationProperties,
            elementName: 'openAiModel',
            fieldUpdate: {field: 'operation', value: 'model'},
            parameters: {model: 'gpt-4'},
        });

        const nestedAgent = (
            updatedClusterElements.tools as unknown as Array<{clusterElements: {model: {parameters: unknown}}}>
        )[0];

        expect(nestedAgent.clusterElements.model.parameters).toEqual({model: 'gpt-4'});
    });
});
