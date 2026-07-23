import {ArrayPropertyType, PropertyAllType} from '@/shared/types';
import {render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ArrayProperty from '../ArrayProperty';

// Mutable hook return so each test can pick which add-trigger branch ArrayProperty renders:
// more than one available type renders the SubPropertyPopover, a single type renders a plain button.
const {hookState} = vi.hoisted(() => ({
    hookState: {
        availablePropertyTypes: [] as Array<{label: string; value: string}>,
        defaultPropertyType: undefined as string | undefined,
    },
}));

const arrayItems: Array<ArrayPropertyType> = [
    {key: 'first', name: '0', type: 'STRING'} as ArrayPropertyType,
    {key: 'second', name: '1', type: 'OBJECT'} as ArrayPropertyType,
];

// Render each item as a bare div so the className ArrayProperty computes is asserted directly.
vi.mock('@/pages/platform/workflow-editor/components/properties/components/ArrayPropertyItem', () => ({
    default: ({className, index}: {className?: string; index: number}) => (
        <div className={className} data-testid={`array-item-${index.toString()}`} />
    ),
}));

vi.mock('@/pages/platform/workflow-editor/components/properties/components/SubPropertyPopover', () => ({
    default: () => <button type="button">Add array item</button>,
}));

vi.mock('@/pages/platform/workflow-editor/components/properties/hooks/useArrayProperty', () => ({
    useArrayProperty: () => ({
        arrayConstraintHint: {variant: 'none'},
        arrayItems,
        availablePropertyTypes: hookState.availablePropertyTypes,
        currentNode: undefined,
        defaultPropertyType: hookState.defaultPropertyType,
        handleAddItemClick: vi.fn(),
        handleDeleteClick: vi.fn(),
        isAddDisabled: false,
        items: undefined,
        name: 'value',
        setArrayItems: vi.fn(),
    }),
}));

const renderArrayProperty = () =>
    render(
        <ArrayProperty
            onDeleteClick={vi.fn()}
            path="value"
            property={{name: 'value', type: 'ARRAY'} as PropertyAllType}
        />
    );

describe('ArrayProperty', () => {
    beforeEach(() => {
        hookState.availablePropertyTypes = [];
        hookState.defaultPropertyType = undefined;
    });

    it('should separate the last item from the add trigger when multiple types render the popover', () => {
        hookState.availablePropertyTypes = [
            {label: 'STRING', value: 'STRING'},
            {label: 'OBJECT', value: 'OBJECT'},
        ];
        hookState.defaultPropertyType = 'STRING';

        renderArrayProperty();

        expect(screen.getByTestId('array-item-1')).toHaveClass('mb-2');
    });

    // A component-defined array with a single item type (for example Mailchimp's marketing_permissions,
    // whose items are one object) renders the plain button instead of the popover. The spacing must not
    // depend on which of the two add-triggers is rendered.
    it('should separate the last item from the add trigger when a single type renders the plain button', () => {
        hookState.availablePropertyTypes = [{label: 'OBJECT', value: 'OBJECT'}];
        hookState.defaultPropertyType = 'OBJECT';

        renderArrayProperty();

        expect(screen.getByTestId('array-item-1')).toHaveClass('mb-2');
    });

    it('should not add the separating margin to items other than the last one', () => {
        hookState.availablePropertyTypes = [{label: 'OBJECT', value: 'OBJECT'}];
        hookState.defaultPropertyType = 'OBJECT';

        renderArrayProperty();

        expect(screen.getByTestId('array-item-0')).not.toHaveClass('mb-2');
    });
});
