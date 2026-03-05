import {PropertyAllType} from '@/shared/types';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {FormProvider, useForm} from 'react-hook-form';
import {describe, expect, it, vi} from 'vitest';

import FormControlledObjectEntries from './FormControlledObjectEntries';

vi.mock('../Property', () => ({
    default: ({property, toolsMode}: {property: PropertyAllType; toolsMode?: boolean}) => (
        <div data-testid={`property-${property.name}`} data-tools-mode={String(!!toolsMode)}>
            {property.label}
        </div>
    ),
}));

const objectProperty: PropertyAllType = {
    additionalProperties: [{controlType: 'TEXT', name: 'value', type: 'STRING'} as PropertyAllType],
    name: 'metadata',
    type: 'OBJECT',
} as PropertyAllType;

function Wrapper({
    defaultValues,
    property = objectProperty,
    toolsMode,
}: {
    defaultValues?: Record<string, unknown>;
    property?: PropertyAllType;
    toolsMode?: boolean;
}) {
    const methods = useForm({defaultValues: defaultValues || {metadata: {color: 'blue', size: 'large'}}});

    return (
        <FormProvider {...methods}>
            <FormControlledObjectEntries
                control={methods.control}
                controlPath="metadata"
                formState={methods.formState}
                property={property}
                toolsMode={toolsMode}
            />
        </FormProvider>
    );
}

describe('FormControlledObjectEntries', () => {
    it('renders object entries with key labels', () => {
        render(<Wrapper />);

        expect(screen.getByText('color')).toBeInTheDocument();
        expect(screen.getByText('size')).toBeInTheDocument();
    });

    it('passes toolsMode to child Property components', () => {
        render(<Wrapper toolsMode={true} />);

        const propertyElements = screen.getAllByTestId(/^property-/);

        for (const propertyElement of propertyElements) {
            expect(propertyElement).toHaveAttribute('data-tools-mode', 'true');
        }
    });

    it('shows add UI when additionalProperties is defined', () => {
        render(<Wrapper />);

        expect(screen.getByPlaceholderText('Key name')).toBeInTheDocument();
        expect(screen.getByText('Add')).toBeInTheDocument();
    });

    it('hides add UI when additionalProperties is not defined', () => {
        const propertyWithoutAdditional: PropertyAllType = {
            name: 'metadata',
            type: 'OBJECT',
        } as PropertyAllType;

        render(<Wrapper property={propertyWithoutAdditional} />);

        expect(screen.queryByPlaceholderText('Key name')).not.toBeInTheDocument();
    });

    it('adds a new entry when key is typed and Add is clicked', async () => {
        render(<Wrapper />);

        const keyInput = screen.getByPlaceholderText('Key name');

        await userEvent.type(keyInput, 'newKey');
        await userEvent.click(screen.getByText('Add'));

        expect(screen.getByText('newKey')).toBeInTheDocument();
    });

    it('does not add a duplicate entry', async () => {
        render(<Wrapper />);

        const keyInput = screen.getByPlaceholderText('Key name');

        await userEvent.type(keyInput, 'color');
        await userEvent.click(screen.getByText('Add'));

        // Should still only have one 'color' entry
        expect(screen.getAllByText('color')).toHaveLength(1);
    });

    it('disables Add button when key input is empty', () => {
        render(<Wrapper />);

        const addButton = screen.getByText('Add').closest('button');

        expect(addButton).toBeDisabled();
    });
});
