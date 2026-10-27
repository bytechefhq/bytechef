import {describe, expect, it} from 'vitest';

import resolveFallbackTab from '../resolveFallbackTab';

const baseProps = {
    actionFetched: true,
    activeTab: 'properties',
    connectionsCount: 0,
    operationDataMissing: false,
    operationDefinition: {properties: [{name: 'url'}]},
    operationProperties: [{name: 'url'}],
    showOutputTab: true,
    triggerFetched: false,
};

describe('resolveFallbackTab', () => {
    it('should keep the properties tab when the operation has properties', () => {
        expect(resolveFallbackTab(baseProps)).toBeUndefined();
    });

    it('should fall back to description when the operation has no properties', () => {
        expect(resolveFallbackTab({...baseProps, operationDefinition: {properties: []}})).toBe('description');
    });

    it('should fall back to description when the fetched action has no operation properties', () => {
        expect(resolveFallbackTab({...baseProps, operationProperties: undefined})).toBe('description');
    });

    it('should fall back to description when a task dispatcher has no properties', () => {
        expect(
            resolveFallbackTab({
                ...baseProps,
                operationDefinition: undefined,
                taskDispatcher: true,
                taskDispatcherDefinition: {properties: []},
            })
        ).toBe('description');
    });

    it('should keep the properties tab when a task dispatcher has properties', () => {
        expect(
            resolveFallbackTab({
                ...baseProps,
                operationDefinition: undefined,
                taskDispatcher: true,
                taskDispatcherDefinition: {properties: [{name: 'condition'}]},
            })
        ).toBeUndefined();
    });

    it('should keep the properties tab while the task dispatcher definition is loading', () => {
        expect(
            resolveFallbackTab({...baseProps, operationDefinition: undefined, taskDispatcher: true})
        ).toBeUndefined();
    });

    it('should fall back to description for the manual trigger', () => {
        expect(resolveFallbackTab({...baseProps, componentName: 'manual'})).toBe('description');
    });

    it('should fall back to description from the connection tab when the node has no connections', () => {
        expect(resolveFallbackTab({...baseProps, activeTab: 'connection'})).toBe('description');
    });

    it('should keep the connection tab when the node has connections', () => {
        expect(resolveFallbackTab({...baseProps, activeTab: 'connection', connectionsCount: 1})).toBeUndefined();
    });

    it('should fall back to description from a hidden output tab', () => {
        expect(resolveFallbackTab({...baseProps, activeTab: 'output', showOutputTab: false})).toBe('description');
    });
});
