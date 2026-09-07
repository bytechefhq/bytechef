import {PropertyAllType} from '@/shared/types';
import {describe, expect, it} from 'vitest';

import getParametersForVersionChange from './getParametersForVersionChange';

const stringProperty = (name: string, defaultValue?: string): PropertyAllType =>
    ({defaultValue, name, type: 'STRING'}) as PropertyAllType;

describe('getParametersForVersionChange', () => {
    it('should keep values of properties that still exist in the new version', () => {
        expect(
            getParametersForVersionChange({
                currentParameters: {message: 'hello', recipient: 'someone'},
                properties: [stringProperty('message'), stringProperty('recipient')],
            })
        ).toEqual({message: 'hello', recipient: 'someone'});
    });

    it('should drop values of properties removed in the new version', () => {
        expect(
            getParametersForVersionChange({
                currentParameters: {message: 'hello', removedProperty: 'stale'},
                properties: [stringProperty('message')],
            })
        ).toEqual({message: 'hello'});
    });

    it('should fill in default values for properties added in the new version', () => {
        expect(
            getParametersForVersionChange({
                currentParameters: {message: 'hello'},
                properties: [stringProperty('message'), stringProperty('format', 'markdown')],
            })
        ).toEqual({format: 'markdown', message: 'hello'});
    });

    it('should prefer the configured value over the default value of the new version', () => {
        expect(
            getParametersForVersionChange({
                currentParameters: {format: 'plain'},
                properties: [stringProperty('format', 'markdown')],
            })
        ).toEqual({format: 'plain'});
    });

    it('should keep a falsy configured value instead of falling back to the default value', () => {
        expect(
            getParametersForVersionChange({
                currentParameters: {enabled: false, retries: 0},
                properties: [
                    {defaultValue: true, name: 'enabled', type: 'BOOLEAN'} as unknown as PropertyAllType,
                    {defaultValue: 3, name: 'retries', type: 'INTEGER'} as unknown as PropertyAllType,
                ],
            })
        ).toEqual({enabled: false, retries: 0});
    });

    it('should merge nested object properties recursively', () => {
        expect(
            getParametersForVersionChange({
                currentParameters: {options: {removedOption: 'stale', timeout: 30}},
                properties: [
                    {
                        name: 'options',
                        properties: [
                            {name: 'timeout', type: 'INTEGER'},
                            {defaultValue: 'json', name: 'responseType', type: 'STRING'},
                        ],
                        type: 'OBJECT',
                    } as PropertyAllType,
                ],
            })
        ).toEqual({options: {responseType: 'json', timeout: 30}});
    });

    it('should keep array values as they are', () => {
        expect(
            getParametersForVersionChange({
                currentParameters: {tags: ['first', 'second']},
                properties: [{items: [{type: 'STRING'}], name: 'tags', type: 'ARRAY'} as PropertyAllType],
            })
        ).toEqual({tags: ['first', 'second']});
    });

    it('should return only default values when there are no configured parameters', () => {
        expect(
            getParametersForVersionChange({
                currentParameters: undefined,
                properties: [stringProperty('format', 'markdown')],
            })
        ).toEqual({format: 'markdown'});
    });

    it('should return an empty object when the new version has no properties', () => {
        expect(
            getParametersForVersionChange({
                currentParameters: {message: 'hello'},
                properties: [],
            })
        ).toEqual({});
    });
});
