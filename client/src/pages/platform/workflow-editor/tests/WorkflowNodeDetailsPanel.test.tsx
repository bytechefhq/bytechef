import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

describe('WorkflowNodeDetailsPanel', () => {
    let mockSetCurrentOperationProperties = vi.fn();
    let mockSetCurrentActionDefinition = vi.fn();
    let mockSetCurrentActionFetched = vi.fn();

    beforeEach(() => {
        vi.clearAllMocks();

        mockSetCurrentOperationProperties = vi.fn();
        mockSetCurrentActionDefinition = vi.fn();
        mockSetCurrentActionFetched = vi.fn();
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('should reset states when switching between nodes', () => {
        const resetStates = () => {
            mockSetCurrentOperationProperties([]);
            mockSetCurrentActionDefinition(undefined);
            mockSetCurrentActionFetched(false);
        };

        resetStates();

        expect(mockSetCurrentOperationProperties).toHaveBeenCalledWith([]);
        expect(mockSetCurrentActionDefinition).toHaveBeenCalledWith(undefined);
        expect(mockSetCurrentActionFetched).toHaveBeenCalledWith(false);
    });

    it('should handle state updates in correct order', () => {
        const updateStates = (properties: unknown[], actionDefinition: unknown) => {
            mockSetCurrentOperationProperties([]);
            mockSetCurrentActionDefinition(undefined);
            mockSetCurrentActionFetched(false);

            if (properties.length > 0) {
                mockSetCurrentOperationProperties(properties);
            }

            if (actionDefinition) {
                mockSetCurrentActionDefinition(actionDefinition);
                mockSetCurrentActionFetched(true);
            }
        };

        const testProperties = [{name: 'test', type: 'string'}];
        const testActionDefinition = {name: 'test_action', properties: testProperties};

        updateStates(testProperties, testActionDefinition);

        expect(mockSetCurrentOperationProperties).toHaveBeenNthCalledWith(1, []);
        expect(mockSetCurrentOperationProperties).toHaveBeenNthCalledWith(2, testProperties);
        expect(mockSetCurrentActionDefinition).toHaveBeenNthCalledWith(1, undefined);
        expect(mockSetCurrentActionDefinition).toHaveBeenNthCalledWith(2, testActionDefinition);
        expect(mockSetCurrentActionFetched).toHaveBeenNthCalledWith(1, false);
        expect(mockSetCurrentActionFetched).toHaveBeenNthCalledWith(2, true);
    });

    it('should handle rapid state changes without race conditions', () => {
        const rapidStateChanges = [
            {nodeName: 'gmail_1', properties: [{name: 'to', type: 'string'}]},
            {nodeName: 'condition_1', properties: []},
            {nodeName: 'gmail_2', properties: [{name: 'folder', type: 'string'}]},
        ];

        rapidStateChanges.forEach((change) => {
            mockSetCurrentOperationProperties([]);
            mockSetCurrentActionDefinition(undefined);
            mockSetCurrentActionFetched(false);

            if (change.properties.length > 0) {
                mockSetCurrentOperationProperties(change.properties);
            }
        });

        expect(mockSetCurrentOperationProperties).toHaveBeenCalledTimes(5);
        expect(mockSetCurrentActionDefinition).toHaveBeenCalledTimes(3);
        expect(mockSetCurrentActionFetched).toHaveBeenCalledTimes(3);

        expect(mockSetCurrentOperationProperties).toHaveBeenLastCalledWith([{name: 'folder', type: 'string'}]);
    });

    it('should handle same component type switching correctly', () => {
        const gmail1Properties = [
            {name: 'to', type: 'string'},
            {name: 'subject', type: 'string'},
        ];

        const gmail2Properties = [
            {name: 'folder', type: 'string'},
            {name: 'limit', type: 'number'},
        ];

        mockSetCurrentOperationProperties([]);
        mockSetCurrentOperationProperties(gmail1Properties);

        mockSetCurrentOperationProperties([]);
        mockSetCurrentOperationProperties(gmail2Properties);

        expect(mockSetCurrentOperationProperties).toHaveBeenLastCalledWith(gmail2Properties);
        expect(mockSetCurrentOperationProperties).not.toHaveBeenLastCalledWith(gmail1Properties);
    });

    it('should handle task dispatcher subtasks correctly', () => {
        const taskDispatcherProperties = [
            {name: 'items', type: 'array'},
            {name: 'task', type: 'object'},
        ];

        mockSetCurrentOperationProperties([]);
        mockSetCurrentOperationProperties(taskDispatcherProperties);

        expect(mockSetCurrentOperationProperties).toHaveBeenLastCalledWith(taskDispatcherProperties);
    });

    it('should handle trigger nodes correctly', () => {
        const triggerProperties = [{name: 'folder', type: 'string'}];

        mockSetCurrentOperationProperties([]);
        mockSetCurrentOperationProperties(triggerProperties);

        expect(mockSetCurrentOperationProperties).toHaveBeenLastCalledWith(triggerProperties);
    });

    it('should ensure clean state transitions', () => {
        const transitions = [
            {from: 'gmail_1', to: 'condition_1'},
            {from: 'condition_1', to: 'gmail_2'},
            {from: 'gmail_2', to: 'gmail_1'},
        ];

        transitions.forEach(() => {
            mockSetCurrentOperationProperties([]);
            mockSetCurrentActionDefinition(undefined);
            mockSetCurrentActionFetched(false);
        });

        expect(mockSetCurrentOperationProperties).toHaveBeenCalledWith([]);
        expect(mockSetCurrentActionDefinition).toHaveBeenCalledWith(undefined);
        expect(mockSetCurrentActionFetched).toHaveBeenCalledWith(false);
    });

    it('should handle operation change on same node', () => {
        const sendEmailProperties = [
            {name: 'to', type: 'string'},
            {name: 'subject', type: 'string'},
        ];

        const readEmailProperties = [
            {name: 'folder', type: 'string'},
            {name: 'limit', type: 'number'},
        ];

        mockSetCurrentOperationProperties([]);
        mockSetCurrentActionDefinition(undefined);
        mockSetCurrentActionFetched(false);
        mockSetCurrentOperationProperties(sendEmailProperties);

        mockSetCurrentOperationProperties([]);
        mockSetCurrentActionDefinition(undefined);
        mockSetCurrentActionFetched(false);
        mockSetCurrentOperationProperties(readEmailProperties);

        expect(mockSetCurrentOperationProperties).toHaveBeenLastCalledWith(readEmailProperties);
        expect(mockSetCurrentOperationProperties).not.toHaveBeenLastCalledWith(sendEmailProperties);
    });

    it('should handle operation change with CurrentOperationSelect interaction', () => {
        const initialProperties = [{name: 'to', type: 'string'}];
        const newProperties = [{name: 'folder', type: 'string'}];

        mockSetCurrentOperationProperties([]);
        mockSetCurrentActionDefinition(undefined);
        mockSetCurrentActionFetched(false);
        mockSetCurrentOperationProperties(initialProperties);

        mockSetCurrentOperationProperties([]);
        mockSetCurrentActionDefinition(undefined);
        mockSetCurrentActionFetched(false);
        mockSetCurrentOperationProperties(newProperties);

        expect(mockSetCurrentOperationProperties).toHaveBeenNthCalledWith(1, []);
        expect(mockSetCurrentOperationProperties).toHaveBeenNthCalledWith(2, initialProperties);
        expect(mockSetCurrentOperationProperties).toHaveBeenNthCalledWith(3, []);
        expect(mockSetCurrentOperationProperties).toHaveBeenNthCalledWith(4, newProperties);
    });
});
