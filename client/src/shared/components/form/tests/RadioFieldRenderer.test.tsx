import {fireEvent, render, screen} from '@/shared/util/test-utils';
import {describe, expect, it} from 'vitest';

import {RadioFieldRenderer} from '../RadioFieldRenderer';
import {createMockForm} from './testUtils';

const mockOptions = [
    {label: 'Option A', value: 'optionA'},
    {label: 'Option B', value: 'optionB'},
    {label: 'Option C', value: 'optionC'},
];

describe('RadioFieldRenderer', () => {
    it('should render with fieldLabel', () => {
        const {form, wrapper} = createMockForm();

        render(
            <RadioFieldRenderer
                form={form}
                formInput={{fieldLabel: 'Select Option', fieldOptions: mockOptions}}
                name="selection"
            />,
            {wrapper}
        );

        expect(screen.getByText('Select Option')).toBeInTheDocument();
    });

    it('should render with fieldName when fieldLabel is not provided', () => {
        const {form, wrapper} = createMockForm();

        render(
            <RadioFieldRenderer
                form={form}
                formInput={{fieldName: 'choiceField', fieldOptions: mockOptions}}
                name="selection"
            />,
            {wrapper}
        );

        expect(screen.getByText('choiceField')).toBeInTheDocument();
    });

    it('should render with name when fieldLabel and fieldName are not provided', () => {
        const {form, wrapper} = createMockForm();

        render(<RadioFieldRenderer form={form} formInput={{fieldOptions: mockOptions}} name="fallbackRadio" />, {
            wrapper,
        });

        expect(screen.getByText('fallbackRadio')).toBeInTheDocument();
    });

    it('should render field description when provided', () => {
        const {form, wrapper} = createMockForm();

        render(
            <RadioFieldRenderer
                form={form}
                formInput={{
                    fieldDescription: 'Choose your preferred option',
                    fieldLabel: 'Preference',
                    fieldOptions: mockOptions,
                }}
                name="preference"
            />,
            {wrapper}
        );

        expect(screen.getByText('Choose your preferred option')).toBeInTheDocument();
    });

    it('should not render field description when not provided', () => {
        const {form, wrapper} = createMockForm();

        render(
            <RadioFieldRenderer
                form={form}
                formInput={{fieldLabel: 'Preference', fieldOptions: mockOptions}}
                name="preference"
            />,
            {wrapper}
        );

        expect(screen.queryByText('Choose your preferred option')).not.toBeInTheDocument();
    });

    it('should render all radio options', () => {
        const {form, wrapper} = createMockForm();

        render(
            <RadioFieldRenderer
                form={form}
                formInput={{fieldLabel: 'Options', fieldOptions: mockOptions}}
                name="options"
            />,
            {wrapper}
        );

        expect(screen.getByText('Option A')).toBeInTheDocument();
        expect(screen.getByText('Option B')).toBeInTheDocument();
        expect(screen.getByText('Option C')).toBeInTheDocument();
    });

    it('should render radio buttons for each option', () => {
        const {form, wrapper} = createMockForm();

        render(
            <RadioFieldRenderer
                form={form}
                formInput={{fieldLabel: 'Options', fieldOptions: mockOptions}}
                name="options"
            />,
            {wrapper}
        );

        const radioButtons = screen.getAllByRole('radio');
        expect(radioButtons).toHaveLength(3);
    });

    it('should update form value when radio option is selected', () => {
        const {form, formRef, wrapper} = createMockForm();

        render(
            <RadioFieldRenderer
                form={form}
                formInput={{fieldLabel: 'Options', fieldOptions: mockOptions}}
                name="selectedOption"
            />,
            {wrapper}
        );

        const optionB = screen.getByRole('radio', {name: 'Option B'});
        fireEvent.click(optionB);

        expect(formRef.current?.getValues('selectedOption')).toBe('optionB');
    });

    it('should reflect selected value from form', () => {
        const {form, wrapper} = createMockForm({selectedOption: 'optionB'});

        render(
            <RadioFieldRenderer
                form={form}
                formInput={{fieldLabel: 'Options', fieldOptions: mockOptions}}
                name="selectedOption"
            />,
            {wrapper}
        );

        const optionB = screen.getByRole('radio', {name: 'Option B'});
        expect(optionB).toBeChecked();
    });

    it('should render empty list when no options provided', () => {
        const {form, wrapper} = createMockForm();

        render(<RadioFieldRenderer form={form} formInput={{fieldLabel: 'Empty Options'}} name="emptyOptions" />, {
            wrapper,
        });

        const radioButtons = screen.queryAllByRole('radio');
        expect(radioButtons).toHaveLength(0);
    });

    it('should render empty list when fieldOptions is undefined', () => {
        const {form, wrapper} = createMockForm();

        render(
            <RadioFieldRenderer
                form={form}
                formInput={{fieldLabel: 'No Options', fieldOptions: undefined}}
                name="noOptions"
            />,
            {wrapper}
        );

        const radioButtons = screen.queryAllByRole('radio');
        expect(radioButtons).toHaveLength(0);
    });

    it('should have correct id for each radio option', () => {
        const {form, wrapper} = createMockForm();

        render(
            <RadioFieldRenderer
                form={form}
                formInput={{fieldLabel: 'Options', fieldOptions: mockOptions}}
                name="radioGroup"
            />,
            {wrapper}
        );

        expect(document.getElementById('radioGroup-optionA')).toBeInTheDocument();
        expect(document.getElementById('radioGroup-optionB')).toBeInTheDocument();
        expect(document.getElementById('radioGroup-optionC')).toBeInTheDocument();
    });

    it('should allow only one option to be selected', () => {
        const {form, wrapper} = createMockForm();

        render(
            <RadioFieldRenderer
                form={form}
                formInput={{fieldLabel: 'Options', fieldOptions: mockOptions}}
                name="singleSelect"
            />,
            {wrapper}
        );

        const optionA = screen.getByRole('radio', {name: 'Option A'});
        const optionB = screen.getByRole('radio', {name: 'Option B'});

        fireEvent.click(optionA);
        expect(optionA).toBeChecked();
        expect(optionB).not.toBeChecked();

        fireEvent.click(optionB);
        expect(optionA).not.toBeChecked();
        expect(optionB).toBeChecked();
    });

    it('should handle non-string form values gracefully', () => {
        const {form, wrapper} = createMockForm({radioField: 123});

        render(
            <RadioFieldRenderer
                form={form}
                formInput={{fieldLabel: 'Options', fieldOptions: mockOptions}}
                name="radioField"
            />,
            {wrapper}
        );

        const radioButtons = screen.getAllByRole('radio');
        radioButtons.forEach((radio) => {
            expect(radio).not.toBeChecked();
        });
    });
});
