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

    it('labels nested object paths and orders them by their root property', () => {
        expect(
            getMissingRequiredPropertyLabels([propertyError('response.responseSchema'), propertyError('scope')], [
                {
                    label: 'Response',
                    name: 'response',
                    properties: [{label: 'Response Schema', name: 'responseSchema'}],
                },
                ...properties,
            ] as PropertyAllType[])
        ).toEqual(['Response > Response Schema', 'Scope']);
    });

    it('labels array item paths once, whichever items miss the property', () => {
        expect(
            getMissingRequiredPropertyLabels(
                [propertyError('headers[1].name'), propertyError('key'), propertyError('headers[0].name')],
                [
                    ...properties,
                    {
                        items: [{properties: [{label: 'Name', name: 'name'}], type: 'OBJECT'}],
                        label: 'Headers',
                        name: 'headers',
                    },
                ] as PropertyAllType[]
            )
        ).toEqual(['Key', 'Headers > Name']);
    });

    it('falls back to the path segment name when a nested property is unknown', () => {
        expect(getMissingRequiredPropertyLabels([propertyError('scope.unknown')], properties)).toEqual([
            'Scope > unknown',
        ]);
    });

    it('falls back to the name when the property has no label', () => {
        expect(getMissingRequiredPropertyLabels([propertyError('key')], [{name: 'key'}] as PropertyAllType[])).toEqual([
            'key',
        ]);
    });
});
