import {renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => {
    return {
        mockComponentDefinition: {
            description: 'Test component',
            icon: 'test-icon',
            name: 'testComponent',
            title: 'Test Component',
            version: 1,
        },
    };
});

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    useGetComponentDefinitionQuery: () => ({
        data: hoisted.mockComponentDefinition,
    }),
}));

import usePropertyCodeEditorDialogRightPanelConnectionsLabel from '../usePropertyCodeEditorDialogRightPanelConnectionsLabel';

describe('usePropertyCodeEditorDialogRightPanelConnectionsLabel', () => {
    const defaultProps = {
        componentConnection: {
            componentName: 'testComponent',
            componentVersion: 1,
            key: 'test-connection',
            required: true,
            workflowNodeName: 'testNode',
        },
    };

    beforeEach(() => {
        vi.clearAllMocks();
    });

    afterEach(() => {
        vi.clearAllMocks();
    });

    describe('componentDefinition', () => {
        it('should return component definition from query', () => {
            const {result} = renderHook(() => usePropertyCodeEditorDialogRightPanelConnectionsLabel(defaultProps));

            expect(result.current.componentDefinition).toEqual(hoisted.mockComponentDefinition);
        });

        it('should have correct name from component definition', () => {
            const {result} = renderHook(() => usePropertyCodeEditorDialogRightPanelConnectionsLabel(defaultProps));

            expect(result.current.componentDefinition?.name).toBe('testComponent');
        });

        it('should have correct title from component definition', () => {
            const {result} = renderHook(() => usePropertyCodeEditorDialogRightPanelConnectionsLabel(defaultProps));

            expect(result.current.componentDefinition?.title).toBe('Test Component');
        });

        it('should have correct version from component definition', () => {
            const {result} = renderHook(() => usePropertyCodeEditorDialogRightPanelConnectionsLabel(defaultProps));

            expect(result.current.componentDefinition?.version).toBe(1);
        });
    });
});
