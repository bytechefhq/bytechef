import getDataPillsFromProperties from '@/pages/platform/workflow-editor/utils/getDataPillsFromProperties';
import {ComponentPropertiesType, PropertyAllType} from '@/shared/types';
import {describe, expect, it} from 'vitest';

const componentProperties = (
    workflowNodeName: string,
    properties: Array<PropertyAllType> = []
): ComponentPropertiesType => ({
    componentDefinition: {
        icon: `${workflowNodeName}-icon`,
        name: workflowNodeName.replace(/_\d+$/, ''),
        version: 1,
    },
    properties,
    workflowNodeName,
});

const stringProperty = (name: string): PropertyAllType => ({name, type: 'STRING'}) as PropertyAllType;

describe('getDataPillsFromProperties', () => {
    it('names every data pill after its own component, not after a positional match', () => {
        const dataPills = getDataPillsFromProperties(
            [
                componentProperties('trigger_1', [stringProperty('id')]),
                componentProperties('component_1', [stringProperty('subject')]),
                componentProperties('component_2', [stringProperty('body')]),
            ],
            ['trigger_1', 'component_2']
        );

        expect(dataPills.map((dataPill) => dataPill.value)).toEqual([
            'trigger_1',
            'trigger_1.id',
            'component_2',
            'component_2.body',
        ]);
    });

    it('never produces a data pill with an undefined value', () => {
        const dataPills = getDataPillsFromProperties(
            [
                componentProperties('trigger_1', [stringProperty('id')]),
                componentProperties('component_1', [stringProperty('subject')]),
                componentProperties('component_2', [stringProperty('body')]),
            ],
            ['trigger_1', 'component_2']
        );

        expect(dataPills.every((dataPill) => typeof dataPill.value === 'string')).toBe(true);
        expect(dataPills.every((dataPill) => typeof dataPill.nodeName === 'string')).toBe(true);
    });

    it('skips the manual trigger and condition nodes', () => {
        const dataPills = getDataPillsFromProperties(
            [
                componentProperties('manual', [stringProperty('id')]),
                componentProperties('condition_1', [stringProperty('result')]),
                componentProperties('component_1', [stringProperty('subject')]),
            ],
            ['manual', 'condition_1', 'component_1']
        );

        expect(dataPills.map((dataPill) => dataPill.value)).toEqual(['component_1', 'component_1.subject']);
    });
});
