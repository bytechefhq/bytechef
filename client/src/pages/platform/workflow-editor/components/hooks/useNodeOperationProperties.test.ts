import {PropertyAllType} from '@/shared/types';
import {renderHook} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

import useNodeOperationProperties from './useNodeOperationProperties';

const excelNode = {
    componentName: 'microsoftExcel',
    name: 'microsoftExcel_1',
    type: 'microsoftExcel/v2/appendRow',
};

const teamsNode = {
    componentName: 'microsoftTeams',
    name: 'microsoftTeams_1',
    type: 'microsoftTeams/v1/sendChannelMessage',
};

const excelDefinition = {
    componentName: 'microsoftExcel',
    name: 'appendRow',
    properties: [{name: 'workbookId'}] as Array<PropertyAllType>,
};

const teamsDefinition = {
    componentName: 'microsoftTeams',
    name: 'sendChannelMessage',
    properties: [{name: 'teamId'}] as Array<PropertyAllType>,
};

type HookPropsType = Parameters<typeof useNodeOperationProperties>[0];

const getPropertyNames = (properties: Array<PropertyAllType>) => properties.map((property) => property.name);

describe('useNodeOperationProperties', () => {
    it('returns the properties of the definition that belongs to the current node', () => {
        const {result} = renderHook<Array<PropertyAllType>, HookPropsType>(
            (props) => useNodeOperationProperties(props),
            {
                initialProps: {currentNode: excelNode, operationDefinition: excelDefinition},
            }
        );

        expect(getPropertyNames(result.current)).toEqual(['workbookId']);
    });

    it('never returns the previous node properties after switching to a node of another component', () => {
        const renderedPropertyNames: Array<Array<string | undefined>> = [];

        const {rerender, result} = renderHook<Array<PropertyAllType>, HookPropsType>(
            (props) => {
                const properties = useNodeOperationProperties(props);

                if (props.currentNode === teamsNode) {
                    renderedPropertyNames.push(getPropertyNames(properties));
                }

                return properties;
            },
            {initialProps: {currentNode: excelNode, operationDefinition: excelDefinition}}
        );

        rerender({currentNode: teamsNode, operationDefinition: excelDefinition});

        expect(result.current).toEqual([]);

        rerender({currentNode: teamsNode, operationDefinition: teamsDefinition});

        expect(getPropertyNames(result.current)).toEqual(['teamId']);
        expect(renderedPropertyNames).not.toContainEqual(['workbookId']);
    });

    it('keeps the node properties while the definition of a new operation of the same node loads', () => {
        const {rerender, result} = renderHook<Array<PropertyAllType>, HookPropsType>(
            (props) => useNodeOperationProperties(props),
            {
                initialProps: {currentNode: excelNode, operationDefinition: excelDefinition},
            }
        );

        rerender({currentNode: {...excelNode, type: 'microsoftExcel/v2/getRows'}, operationDefinition: undefined});

        expect(getPropertyNames(result.current)).toEqual(['workbookId']);
    });

    it('returns no properties before any definition has loaded', () => {
        const {result} = renderHook<Array<PropertyAllType>, HookPropsType>(
            (props) => useNodeOperationProperties(props),
            {
                initialProps: {currentNode: teamsNode, operationDefinition: undefined},
            }
        );

        expect(result.current).toEqual([]);
    });
});
