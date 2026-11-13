import {TooltipProvider} from '@/components/ui/tooltip';
import {i18n} from '@lingui/core';
import {I18nProvider} from '@lingui/react';
import {render, screen, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactNode} from 'react';
import {beforeAll, describe, expect, it, vi} from 'vitest';

import {SchemaRecordType} from '../../utils/types';
import {SchemaArrayControls, SchemaControls} from '../SchemaControls';

vi.mock('@lingui/core/macro', () => ({
    t: (strings: TemplateStringsArray, ...values: unknown[]) => String.raw({raw: strings}, ...values),
}));

vi.mock('@/assets/TwoHashesIcon.svg?react', () => ({
    default: () => <svg />,
}));

const wrapper = ({children}: {children: ReactNode}) => (
    <I18nProvider i18n={i18n}>
        <TooltipProvider>{children}</TooltipProvider>
    </I18nProvider>
);

const getExtraFieldsButton = () => screen.getByTitle('Extra fields');

const getLastSchema = (onChange: ReturnType<typeof vi.fn>) =>
    onChange.mock.calls[onChange.mock.calls.length - 1][0] as SchemaRecordType;

beforeAll(() => {
    i18n.load('en', {});
    i18n.activate('en');

    Element.prototype.hasPointerCapture = Element.prototype.hasPointerCapture || vi.fn();
    Element.prototype.releasePointerCapture = Element.prototype.releasePointerCapture || vi.fn();
});

describe('SchemaControls', () => {
    it('shows the pill title and type, and no key input without a key handler', () => {
        render(
            <SchemaControls onChange={vi.fn()} schema={{title: 'Customer', type: 'string'}} schemakey="customer" />,
            {wrapper}
        );

        expect(screen.getByPlaceholderText('e.g. Invoice Number')).toHaveValue('Customer');
        expect(screen.getByRole('combobox')).toHaveTextContent('String');
        expect(screen.queryByPlaceholderText('e.g. invoiceNumber')).not.toBeInTheDocument();
    });

    it('commits an edited pill title into the schema', async () => {
        const onChange = vi.fn();

        render(<SchemaControls onChange={onChange} schema={{title: 'Old', type: 'string'}} schemakey="key" />, {
            wrapper,
        });

        const titleInput = screen.getByPlaceholderText('e.g. Invoice Number');

        await userEvent.clear(titleInput);
        await userEvent.type(titleInput, 'New title{Enter}');

        expect(getLastSchema(onChange)).toEqual({title: 'New title', type: 'string'});
    });

    it('removes the title from the schema when the pill title is cleared', async () => {
        const onChange = vi.fn();

        render(<SchemaControls onChange={onChange} schema={{title: 'Old', type: 'string'}} schemakey="key" />, {
            wrapper,
        });

        const titleInput = screen.getByPlaceholderText('e.g. Invoice Number');

        await userEvent.clear(titleInput);
        await userEvent.type(titleInput, '{Enter}');

        expect(getLastSchema(onChange)).toEqual({type: 'string'});
    });

    it('renames the pill key through the key input', async () => {
        const onChangeKey = vi.fn();

        render(
            <SchemaControls onChange={vi.fn()} onChangeKey={onChangeKey} schema={{type: 'string'}} schemakey="first" />,
            {wrapper}
        );

        const keyInput = screen.getByPlaceholderText('e.g. invoiceNumber');

        expect(keyInput).toHaveValue('first');

        await userEvent.clear(keyInput);
        await userEvent.type(keyInput, 'second');
        await userEvent.tab();

        expect(onChangeKey).toHaveBeenCalledWith('second');
    });

    it('turns a schema without a type into an object schema', () => {
        const onChange = vi.fn();

        render(<SchemaControls onChange={onChange} schema={{title: 'Untyped'}} schemakey="key" />, {wrapper});

        expect(onChange).toHaveBeenCalledWith({title: 'Untyped', type: 'object'});
    });

    it('drops the fields the new type does not support when the type changes', async () => {
        const onChange = vi.fn();

        render(
            <SchemaControls
                onChange={onChange}
                schema={{description: 'Count', maxLength: 5, title: 'Count', type: 'string'}}
                schemakey="count"
            />,
            {wrapper}
        );

        await userEvent.click(screen.getByRole('combobox'));
        await userEvent.click(await screen.findByRole('option', {name: /Integer/}));

        expect(getLastSchema(onChange)).toEqual({description: 'Count', title: 'Count', type: 'integer'});
    });

    it('offers only object and array types at the root', async () => {
        render(<SchemaControls onChange={vi.fn()} root schema={{type: 'object'}} schemakey="root" />, {wrapper});

        await userEvent.click(screen.getByRole('combobox'));

        const options = await screen.findAllByRole('option');

        expect(options.map((option) => option.textContent)).toEqual(['Object', 'Array']);
    });

    it('calls onDelete from the delete button and hides it without a handler', async () => {
        const onDelete = vi.fn();

        const {rerender} = render(
            <SchemaControls onChange={vi.fn()} onDelete={onDelete} schema={{type: 'string'}} schemakey="key" />,
            {wrapper}
        );

        await userEvent.click(screen.getByTitle('Delete'));

        expect(onDelete).toHaveBeenCalledTimes(1);

        rerender(<SchemaControls onChange={vi.fn()} schema={{type: 'string'}} schemakey="key" />);

        expect(screen.queryByTitle('Delete')).not.toBeInTheDocument();
    });

    describe('adding a property', () => {
        it('adds an empty child property to an object schema when no add handler is given', async () => {
            const onChange = vi.fn();

            render(<SchemaControls onChange={onChange} schema={{properties: {}, type: 'object'}} schemakey="key" />, {
                wrapper,
            });

            await userEvent.click(screen.getByTitle('Add Property'));

            const properties = getLastSchema(onChange).properties as Record<string, unknown>;
            const propertyKeys = Object.keys(properties);

            expect(propertyKeys).toHaveLength(1);
            expect(propertyKeys[0]).toMatch(/^__\d+__$/);
            expect(properties[propertyKeys[0]]).toEqual({});
        });

        it('delegates to the add handler when one is given', async () => {
            const onAdd = vi.fn();
            const onChange = vi.fn();

            render(
                <SchemaControls
                    onAdd={onAdd}
                    onChange={onChange}
                    schema={{properties: {}, type: 'object'}}
                    schemakey="key"
                />,
                {wrapper}
            );

            await userEvent.click(screen.getByTitle('Add Property'));

            expect(onAdd).toHaveBeenCalledTimes(1);
            expect(onChange).not.toHaveBeenCalled();
        });

        it('disables adding for a non-object schema that has an add handler', () => {
            render(<SchemaControls onAdd={vi.fn()} onChange={vi.fn()} schema={{type: 'string'}} schemakey="key" />, {
                wrapper,
            });

            expect(screen.getByTitle('Add Property')).toBeDisabled();
        });

        it('offers no add button for a non-object schema without an add handler', () => {
            render(<SchemaControls onChange={vi.fn()} schema={{type: 'string'}} schemakey="key" />, {wrapper});

            expect(screen.queryByTitle('Add Property')).not.toBeInTheDocument();
        });

        it('labels the root add button "Add Data Pill"', () => {
            render(<SchemaControls onChange={vi.fn()} root schema={{type: 'object'}} schemakey="root" />, {wrapper});

            expect(screen.getByRole('button', {name: /Add Data Pill/})).toBeEnabled();
        });
    });

    describe('extra fields', () => {
        it('counts the extra fields the schema carries', () => {
            render(
                <SchemaControls
                    onChange={vi.fn()}
                    schema={{description: 'A name', maxLength: 10, type: 'string'}}
                    schemakey="name"
                />,
                {wrapper}
            );

            expect(within(getExtraFieldsButton()).getByText('2')).toBeInTheDocument();
        });

        it('shows no count when the schema has no extra fields', () => {
            render(<SchemaControls onChange={vi.fn()} schema={{type: 'string'}} schemakey="name" />, {wrapper});

            expect(getExtraFieldsButton().textContent).toBe('');
        });

        it('does not count the pill title, which is edited inline rather than as an extra field', () => {
            render(
                <SchemaControls
                    onChange={vi.fn()}
                    schema={{description: 'A name', title: 'Name', type: 'string'}}
                    schemakey="name"
                />,
                {wrapper}
            );

            expect(within(getExtraFieldsButton()).getByText('1')).toBeInTheDocument();
        });

        it('does not count required at the root', () => {
            render(
                <SchemaControls
                    onChange={vi.fn()}
                    root
                    schema={{description: 'Root', properties: {}, required: [], type: 'object'}}
                    schemakey="root"
                />,
                {wrapper}
            );

            expect(within(getExtraFieldsButton()).getByText('1')).toBeInTheDocument();
        });

        it('opens the extra fields menu listing the fields the schema carries', async () => {
            render(
                <SchemaControls onChange={vi.fn()} schema={{description: 'A name', type: 'string'}} schemakey="name" />,
                {wrapper}
            );

            await userEvent.click(getExtraFieldsButton());

            expect(await screen.findByText('Add a field')).toBeInTheDocument();
            expect(screen.getByDisplayValue('A name')).toBeInTheDocument();

            await userEvent.click(screen.getByTitle('Close'));

            expect(screen.queryByText('Add a field')).not.toBeInTheDocument();
        });
    });
});

describe('SchemaArrayControls', () => {
    it('offers an add button only when an add handler is given', async () => {
        const onAdd = vi.fn();

        const {rerender} = render(<SchemaArrayControls onAdd={onAdd} onChange={vi.fn()} schema={{type: 'string'}} />, {
            wrapper,
        });

        await userEvent.click(screen.getByTitle('Add'));

        expect(onAdd).toHaveBeenCalledTimes(1);

        rerender(<SchemaArrayControls onChange={vi.fn()} schema={{type: 'string'}} />);

        expect(screen.queryByTitle('Add')).not.toBeInTheDocument();
    });

    it('turns untyped items into an object schema', () => {
        const onChange = vi.fn();

        render(<SchemaArrayControls onChange={onChange} schema={{}} />, {wrapper});

        expect(onChange).toHaveBeenCalledWith({type: 'object'});
    });

    it('changes the item type', async () => {
        const onChange = vi.fn();

        render(<SchemaArrayControls onChange={onChange} schema={{type: 'string'}} />, {wrapper});

        await userEvent.click(screen.getByRole('combobox'));
        await userEvent.click(await screen.findByRole('option', {name: /Boolean/}));

        expect(getLastSchema(onChange)).toEqual({type: 'boolean'});
    });

    it('counts the extra fields of object items but not their title', () => {
        render(
            <SchemaArrayControls
                onChange={vi.fn()}
                schema={{description: 'Row', properties: {}, title: 'Row', type: 'object'}}
            />,
            {wrapper}
        );

        expect(within(getExtraFieldsButton()).getByText('1')).toBeInTheDocument();
    });
});
