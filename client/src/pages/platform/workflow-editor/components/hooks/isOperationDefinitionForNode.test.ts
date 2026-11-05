import {describe, expect, it} from 'vitest';

import isOperationDefinitionForNode from './isOperationDefinitionForNode';

describe('isOperationDefinitionForNode', () => {
    it('rejects the previous node definition right after switching to a node of another component', () => {
        expect(
            isOperationDefinitionForNode(
                {componentName: 'microsoftExcel', name: 'appendRow'},
                {componentName: 'microsoftTeams', type: 'microsoftTeams/v1/sendChannelMessage'}
            )
        ).toBe(false);
    });

    it('rejects a definition of another operation of the same component', () => {
        expect(
            isOperationDefinitionForNode(
                {componentName: 'microsoftExcel', name: 'appendRow'},
                {componentName: 'microsoftExcel', type: 'microsoftExcel/v2/getRows'}
            )
        ).toBe(false);
    });

    it('accepts the definition of the node component and operation', () => {
        expect(
            isOperationDefinitionForNode(
                {componentName: 'microsoftTeams', name: 'sendChannelMessage'},
                {componentName: 'microsoftTeams', type: 'microsoftTeams/v1/sendChannelMessage'}
            )
        ).toBe(true);
    });

    it('matches a task dispatcher definition by its name', () => {
        expect(
            isOperationDefinitionForNode({name: 'loop'}, {componentName: 'loop', taskDispatcher: true, type: 'loop/v1'})
        ).toBe(true);

        expect(
            isOperationDefinitionForNode(
                {name: 'condition'},
                {componentName: 'loop', taskDispatcher: true, type: 'loop/v1'}
            )
        ).toBe(false);
    });

    it('matches a cluster element definition by its component', () => {
        expect(
            isOperationDefinitionForNode(
                {componentName: 'openAi', name: 'model'},
                {clusterElementType: 'model', componentName: 'openAi', type: 'openAi/v1/model'}
            )
        ).toBe(true);

        expect(
            isOperationDefinitionForNode(
                {componentName: 'anthropic', name: 'model'},
                {clusterElementType: 'model', componentName: 'openAi', type: 'openAi/v1/model'}
            )
        ).toBe(false);
    });

    it('accepts a definition when the node type has no operation yet', () => {
        expect(
            isOperationDefinitionForNode(
                {componentName: 'microsoftExcel', name: 'appendRow'},
                {componentName: 'microsoftExcel', type: 'microsoftExcel/v2'}
            )
        ).toBe(true);
    });

    it('rejects a missing definition or node', () => {
        expect(
            isOperationDefinitionForNode(undefined, {componentName: 'microsoftExcel', type: 'microsoftExcel/v2'})
        ).toBe(false);

        expect(isOperationDefinitionForNode({componentName: 'microsoftExcel', name: 'appendRow'}, undefined)).toBe(
            false
        );
    });
});
