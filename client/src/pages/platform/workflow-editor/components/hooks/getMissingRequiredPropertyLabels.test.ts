import {PropertyAllType} from '@/shared/types';
import {describe, expect, it} from 'vitest';

import {WorkflowNodeDetailsErrorI} from './getMissingRequiredConnectionErrors';
import getMissingRequiredPropertyLabels from './getMissingRequiredPropertyLabels';

const properties = [
    {label: 'Scope', name: 'scope'},
    {label: 'Key', name: 'key'},
    {label: 'Type', name: 'type'},
] as PropertyAllType[];

const propertyError = (name: string): WorkflowNodeDetailsErrorI => ({kind: 'PROPERTY', name, severity: 'ERROR'});

describe('getMissingRequiredPropertyLabels', () => {
    it('returns the labels in the order the properties are shown', () => {
        expect(
            getMissingRequiredPropertyLabels(
                [propertyError('type'), propertyError('scope'), propertyError('key')],
                properties
            )
        ).toEqual(['Scope', 'Key', 'Type']);
    });

    it('ignores errors that are not missing properties', () => {
        expect(
            getMissingRequiredPropertyLabels(
                [
                    {kind: 'CONNECTION', name: 'Data Storage', severity: 'ERROR'},
                    {kind: 'ISSUE', name: 'Unknown reference', propertyLabel: 'Key', severity: 'WARNING'},
                    propertyError('key'),
                ],
                properties
            )
        ).toEqual(['Key']);
    });

    it('falls back to the name, after the known properties, when no property matches', () => {
        expect(getMissingRequiredPropertyLabels([propertyError('options'), propertyError('key')], properties)).toEqual([
            'Key',
            'options',
        ]);
    });

    it('falls back to the name when the property has no label', () => {
        expect(getMissingRequiredPropertyLabels([propertyError('key')], [{name: 'key'}] as PropertyAllType[])).toEqual([
            'key',
        ]);
    });
});
