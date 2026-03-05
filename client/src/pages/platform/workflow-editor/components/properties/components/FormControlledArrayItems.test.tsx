import {PropertyAllType} from '@/shared/types';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import React from 'react';
import {FormProvider, useForm} from 'react-hook-form';
import {describe, expect, it, vi} from 'vitest';

import FormControlledArrayItems from './FormControlledArrayItems';

vi.mock('../Property', () => ({
    default: ({
        deletePropertyButton,
        property,
        toolsMode,
    }: {
        deletePropertyButton?: React.ReactNode;
        property: PropertyAllType;
        toolsMode?: boolean;
    }) => (
        <div data-testid={`property-${property.name}`} data-tools-mode={String(!!toolsMode)}>
            {property.label}

            {deletePropertyButton}
        </div>
    ),
}));

const arrayProperty: PropertyAllType = {
    items: [{controlType: 'TEXT', name: 'item', type: 'STRING'} as PropertyAllType],
    name: 'tags',
    type: 'ARRAY',
} as PropertyAllType;

function Wrapper({
    defaultValues,
    property = arrayProperty,
    toolsMode,
}: {
    defaultValues?: Record<string, unknown>;
    property?: PropertyAllType;
    toolsMode?: boolean;
}) {
    const methods = useForm({defaultValues: defaultValues || {tags: ['first', 'second']}});

    return (
        <FormProvider {...methods}>
            <FormControlledArrayItems
                control={methods.control}
                controlPath="tags"
                formState={methods.formState}
                property={property}
                toolsMode={toolsMode}
            />
        </FormProvider>
    );
}

describe('FormControlledArrayItems', () => {
    it('renders array items with labels', () => {
        render(<Wrapper />);

        expect(screen.getByText('Item 0')).toBeInTheDocument();
        expect(screen.getByText('Item 1')).toBeInTheDocument();
    });

    it('renders add item button', () => {
        render(<Wrapper />);

        expect(screen.getByText('Add item')).toBeInTheDocument();
    });

    it('renders nothing when item definition is missing', () => {
        const propertyWithNoItems: PropertyAllType = {
            name: 'empty',
            type: 'ARRAY',
        } as PropertyAllType;

        const {container} = render(<Wrapper defaultValues={{tags: ['value']}} property={propertyWithNoItems} />);

        expect(container.querySelector('ul')).not.toBeInTheDocument();
    });

    it('passes toolsMode to child Property components', () => {
        render(<Wrapper toolsMode={true} />);

        const propertyElements = screen.getAllByTestId(/^property-/);

        for (const propertyElement of propertyElements) {
            expect(propertyElement).toHaveAttribute('data-tools-mode', 'true');
        }
    });

    it('adds a new item when add button is clicked', async () => {
        render(<Wrapper />);

        expect(screen.getAllByTestId(/^property-/)).toHaveLength(2);

        await userEvent.click(screen.getByText('Add item'));

        expect(screen.getAllByTestId(/^property-/)).toHaveLength(3);
    });

    it('removes an item when delete button is clicked', async () => {
        render(<Wrapper />);

        const deleteButtons = screen.getAllByRole('button', {name: ''});
        // First button-like elements are the delete (X) icons within each item
        const firstDeleteButton = deleteButtons[0];

        await userEvent.click(firstDeleteButton);

        expect(screen.getAllByTestId(/^property-/)).toHaveLength(1);
    });
});
