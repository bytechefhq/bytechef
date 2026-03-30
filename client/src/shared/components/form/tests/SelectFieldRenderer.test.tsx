import {fireEvent, render, screen, waitFor} from '@/shared/util/test-utils';
import {describe, expect, it} from 'vitest';

import {SelectFieldRenderer} from '../SelectFieldRenderer';
import {createMockForm} from './testUtils';

const mockOptions = [
    {label: 'Red', value: 'red'},
    {label: 'Blue', value: 'blue'},
    {label: 'Green', value: 'green'},
];

describe('SelectFieldRenderer', () => {
    describe('Single Select Mode', () => {
        it('should render with fieldLabel', () => {
            const {form, wrapper} = createMockForm();

            render(
                <SelectFieldRenderer
                    form={form}
                    formInput={{fieldLabel: 'Choose Color', fieldOptions: mockOptions}}
                    name="color"
                />,
                {wrapper}
            );

            expect(screen.getByText('Choose Color')).toBeInTheDocument();
        });

        it('should render with fieldName when fieldLabel is not provided', () => {
            const {form, wrapper} = createMockForm();

            render(
                <SelectFieldRenderer
                    form={form}
                    formInput={{fieldName: 'colorField', fieldOptions: mockOptions}}
                    name="color"
                />,
                {wrapper}
            );

            expect(screen.getByText('colorField')).toBeInTheDocument();
        });

        it('should render with name when fieldLabel and fieldName are not provided', () => {
            const {form, wrapper} = createMockForm();

            render(<SelectFieldRenderer form={form} formInput={{fieldOptions: mockOptions}} name="fallbackSelect" />, {
                wrapper,
            });

            expect(screen.getByText('fallbackSelect')).toBeInTheDocument();
        });

        it('should render field description when provided', () => {
            const {form, wrapper} = createMockForm();

            render(
                <SelectFieldRenderer
                    form={form}
                    formInput={{
                        fieldDescription: 'Select your favorite color',
                        fieldLabel: 'Color',
                        fieldOptions: mockOptions,
                    }}
                    name="color"
                />,
                {wrapper}
            );

            expect(screen.getByText('Select your favorite color')).toBeInTheDocument();
        });

        it('should render select trigger with default placeholder', () => {
            const {form, wrapper} = createMockForm();

            render(
                <SelectFieldRenderer
                    form={form}
                    formInput={{fieldLabel: 'Color', fieldOptions: mockOptions}}
                    name="color"
                />,
                {wrapper}
            );

            expect(screen.getByText('Select...')).toBeInTheDocument();
        });

        it('should render select trigger with custom placeholder', () => {
            const {form, wrapper} = createMockForm();

            render(
                <SelectFieldRenderer
                    form={form}
                    formInput={{fieldLabel: 'Color', fieldOptions: mockOptions, placeholder: 'Pick a color'}}
                    name="color"
                />,
                {wrapper}
            );

            expect(screen.getByText('Pick a color')).toBeInTheDocument();
        });

        it('should show options when clicked', async () => {
            const {form, wrapper} = createMockForm();

            render(
                <SelectFieldRenderer
                    form={form}
                    formInput={{fieldLabel: 'Color', fieldOptions: mockOptions}}
                    name="color"
                />,
                {wrapper}
            );

            fireEvent.click(screen.getByRole('combobox'));

            await waitFor(() => {
                expect(screen.getByRole('option', {name: 'Red'})).toBeInTheDocument();
                expect(screen.getByRole('option', {name: 'Blue'})).toBeInTheDocument();
                expect(screen.getByRole('option', {name: 'Green'})).toBeInTheDocument();
            });
        });

        it('should update form value when option is selected', async () => {
            const {form, formRef, wrapper} = createMockForm();

            render(
                <SelectFieldRenderer
                    form={form}
                    formInput={{fieldLabel: 'Color', fieldOptions: mockOptions}}
                    name="selectedColor"
                />,
                {wrapper}
            );

            fireEvent.click(screen.getByRole('combobox'));

            await waitFor(() => {
                expect(screen.getByRole('option', {name: 'Blue'})).toBeInTheDocument();
            });

            fireEvent.click(screen.getByRole('option', {name: 'Blue'}));

            expect(formRef.current?.getValues('selectedColor')).toBe('blue');
        });

        it('should display selected value from form', () => {
            const {form, wrapper} = createMockForm({color: 'green'});

            render(
                <SelectFieldRenderer
                    form={form}
                    formInput={{fieldLabel: 'Color', fieldOptions: mockOptions}}
                    name="color"
                />,
                {wrapper}
            );

            expect(screen.getByText('Green')).toBeInTheDocument();
        });
    });

    describe('Multiple Choice Mode', () => {
        it('should render checkboxes when multipleChoice is true', () => {
            const {form, wrapper} = createMockForm();

            render(
                <SelectFieldRenderer
                    form={form}
                    formInput={{fieldLabel: 'Colors', fieldOptions: mockOptions, multipleChoice: true}}
                    name="colors"
                />,
                {wrapper}
            );

            const checkboxes = screen.getAllByRole('checkbox');
            expect(checkboxes).toHaveLength(3);
        });

        it('should render option labels in multiple choice mode', () => {
            const {form, wrapper} = createMockForm();

            render(
                <SelectFieldRenderer
                    form={form}
                    formInput={{fieldLabel: 'Colors', fieldOptions: mockOptions, multipleChoice: true}}
                    name="colors"
                />,
                {wrapper}
            );

            expect(screen.getByText('Red')).toBeInTheDocument();
            expect(screen.getByText('Blue')).toBeInTheDocument();
            expect(screen.getByText('Green')).toBeInTheDocument();
        });

        it('should update form value when checkbox is checked', () => {
            const {form, formRef, wrapper} = createMockForm({colors: []});

            render(
                <SelectFieldRenderer
                    form={form}
                    formInput={{fieldLabel: 'Colors', fieldOptions: mockOptions, multipleChoice: true}}
                    name="colors"
                />,
                {wrapper}
            );

            const checkboxes = screen.getAllByRole('checkbox');
            fireEvent.click(checkboxes[0]);

            expect(formRef.current?.getValues('colors')).toContain('red');
        });

        it('should allow multiple selections', () => {
            const {form, formRef, wrapper} = createMockForm({colors: []});

            render(
                <SelectFieldRenderer
                    form={form}
                    formInput={{fieldLabel: 'Colors', fieldOptions: mockOptions, multipleChoice: true}}
                    name="colors"
                />,
                {wrapper}
            );

            const checkboxes = screen.getAllByRole('checkbox');
            fireEvent.click(checkboxes[0]);
            fireEvent.click(checkboxes[1]);

            const values = formRef.current?.getValues('colors') as string[];
            expect(values).toContain('red');
            expect(values).toContain('blue');
        });

        it('should remove value when checkbox is unchecked', () => {
            const {form, formRef, wrapper} = createMockForm({colors: ['red', 'blue']});

            render(
                <SelectFieldRenderer
                    form={form}
                    formInput={{fieldLabel: 'Colors', fieldOptions: mockOptions, multipleChoice: true}}
                    name="colors"
                />,
                {wrapper}
            );

            const checkboxes = screen.getAllByRole('checkbox');
            fireEvent.click(checkboxes[0]);

            const values = formRef.current?.getValues('colors') as string[];
            expect(values).not.toContain('red');
            expect(values).toContain('blue');
        });

        it('should reflect checked state from form values', () => {
            const {form, wrapper} = createMockForm({colors: ['red', 'green']});

            render(
                <SelectFieldRenderer
                    form={form}
                    formInput={{fieldLabel: 'Colors', fieldOptions: mockOptions, multipleChoice: true}}
                    name="colors"
                />,
                {wrapper}
            );

            const checkboxes = screen.getAllByRole('checkbox');
            expect(checkboxes[0]).toBeChecked();
            expect(checkboxes[1]).not.toBeChecked();
            expect(checkboxes[2]).toBeChecked();
        });

        it('should respect maxSelection limit', () => {
            const {form, formRef, wrapper} = createMockForm({colors: ['red', 'blue']});

            render(
                <SelectFieldRenderer
                    form={form}
                    formInput={{
                        fieldLabel: 'Colors',
                        fieldOptions: mockOptions,
                        maxSelection: 2,
                        multipleChoice: true,
                    }}
                    name="colors"
                />,
                {wrapper}
            );

            const checkboxes = screen.getAllByRole('checkbox');
            fireEvent.click(checkboxes[2]);

            const values = formRef.current?.getValues('colors') as string[];
            expect(values).toHaveLength(2);
            expect(values).not.toContain('green');
        });

        it('should allow selection when under maxSelection limit', () => {
            const {form, formRef, wrapper} = createMockForm({colors: ['red']});

            render(
                <SelectFieldRenderer
                    form={form}
                    formInput={{
                        fieldLabel: 'Colors',
                        fieldOptions: mockOptions,
                        maxSelection: 2,
                        multipleChoice: true,
                    }}
                    name="colors"
                />,
                {wrapper}
            );

            const checkboxes = screen.getAllByRole('checkbox');
            fireEvent.click(checkboxes[1]);

            const values = formRef.current?.getValues('colors') as string[];
            expect(values).toContain('blue');
        });

        it('should handle non-array form values gracefully', () => {
            const {form, wrapper} = createMockForm({colors: 'invalid'});

            render(
                <SelectFieldRenderer
                    form={form}
                    formInput={{fieldLabel: 'Colors', fieldOptions: mockOptions, multipleChoice: true}}
                    name="colors"
                />,
                {wrapper}
            );

            const checkboxes = screen.getAllByRole('checkbox');
            checkboxes.forEach((checkbox) => {
                expect(checkbox).not.toBeChecked();
            });
        });
    });

    describe('Empty Options', () => {
        it('should handle empty fieldOptions for single select', () => {
            const {form, wrapper} = createMockForm();

            render(
                <SelectFieldRenderer form={form} formInput={{fieldLabel: 'Empty', fieldOptions: []}} name="empty" />,
                {wrapper}
            );

            expect(screen.getByText('Select...')).toBeInTheDocument();
        });

        it('should handle undefined fieldOptions for single select', () => {
            const {form, wrapper} = createMockForm();

            render(<SelectFieldRenderer form={form} formInput={{fieldLabel: 'Empty'}} name="empty" />, {wrapper});

            expect(screen.getByText('Select...')).toBeInTheDocument();
        });

        it('should handle empty fieldOptions for multiple choice', () => {
            const {form, wrapper} = createMockForm();

            render(
                <SelectFieldRenderer
                    form={form}
                    formInput={{fieldLabel: 'Empty', fieldOptions: [], multipleChoice: true}}
                    name="empty"
                />,
                {wrapper}
            );

            const checkboxes = screen.queryAllByRole('checkbox');
            expect(checkboxes).toHaveLength(0);
        });
    });
});
