import {ComponentDefinition} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import {getComponentPropertyInputValues} from './getComponentPropertyInputValues';

const componentDefinition = {
    inputs: [
        {name: 'channel', properties: [{controlType: 'SELECT', label: 'Channel', name: 'channel', type: 'STRING'}]},
        {label: 'Date Range', name: 'dateRange', properties: []},
    ],
} as unknown as ComponentDefinition;

describe('getComponentPropertyInputValues', () => {
    it("sets groupName and falls back to the lone property's label for an unlabeled single-property group", () => {
        const result = getComponentPropertyInputValues({
            componentDefinition,
            componentName: 'slack',
            componentVersion: 2,
            currentLabel: '',
            currentName: '',
            selection: 'group:channel',
        });

        expect(result).toEqual({
            componentName: 'slack',
            componentVersion: 2,
            groupName: 'channel',
            label: 'Channel',
            name: 'channel',
        });
    });

    it('sets groupName and auto-fills name/label when a compound group is picked', () => {
        const result = getComponentPropertyInputValues({
            componentDefinition,
            componentName: 'slack',
            componentVersion: 2,
            currentLabel: '',
            currentName: '',
            selection: 'group:dateRange',
        });

        expect(result.groupName).toBe('dateRange');
        expect(result.label).toBe('Date Range');
        expect(result.name).toBe('dateRange');
    });

    it('keeps existing name/label when not empty', () => {
        const result = getComponentPropertyInputValues({
            componentDefinition,
            componentName: 'slack',
            componentVersion: 2,
            currentLabel: 'Existing Label',
            currentName: 'existingName',
            selection: 'group:channel',
        });

        expect(result.name).toBe('existingName');
        expect(result.label).toBe('Existing Label');
    });

    it('returns only component identity when the selection kind is unrecognized', () => {
        const result = getComponentPropertyInputValues({
            componentDefinition,
            componentName: 'slack',
            componentVersion: 2,
            currentLabel: '',
            currentName: '',
            selection: 'channel',
        });

        expect(result).toEqual({
            componentName: 'slack',
            componentVersion: 2,
        });
    });

    it('falls back to the group name as label when the group is not in the definition', () => {
        const result = getComponentPropertyInputValues({
            componentDefinition,
            componentName: 'slack',
            componentVersion: 2,
            currentLabel: '',
            currentName: '',
            selection: 'group:unknownGroup',
        });

        expect(result.label).toBe('unknownGroup');
        expect(result.name).toBe('unknownGroup');
        expect(result.groupName).toBe('unknownGroup');
    });
});
