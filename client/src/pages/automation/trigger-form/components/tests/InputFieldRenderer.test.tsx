import {FieldType} from '@/pages/automation/trigger-form/TriggerForm';
import {fireEvent, render, screen} from '@/shared/util/test-utils';
import {describe, expect, it} from 'vitest';

import {InputFieldRenderer} from '../InputFieldRenderer';
import {createMockForm} from './testUtils';

describe('InputFieldRenderer', () => {
    it('should render with fieldLabel', () => {
        const {form, wrapper} = createMockForm();

        render(<InputFieldRenderer form={form} formInput={{fieldLabel: 'Username'}} name="username" />, {wrapper});

        expect(screen.getByText('Username')).toBeInTheDocument();
    });

    it('should render with fieldName when fieldLabel is not provided', () => {
        const {form, wrapper} = createMockForm();

        render(<InputFieldRenderer form={form} formInput={{fieldName: 'userField'}} name="username" />, {wrapper});

        expect(screen.getByText('userField')).toBeInTheDocument();
    });

    it('should render with name when fieldLabel and fieldName are not provided', () => {
        const {form, wrapper} = createMockForm();

        render(<InputFieldRenderer form={form} formInput={{}} name="fallbackInput" />, {wrapper});

        expect(screen.getByText('fallbackInput')).toBeInTheDocument();
    });

    it('should render field description when provided', () => {
        const {form, wrapper} = createMockForm();

        render(
            <InputFieldRenderer
                form={form}
                formInput={{fieldDescription: 'Enter your full name', fieldLabel: 'Name'}}
                name="name"
            />,
            {wrapper}
        );

        expect(screen.getByText('Enter your full name')).toBeInTheDocument();
    });

    it('should not render field description when not provided', () => {
        const {form, wrapper} = createMockForm();

        render(<InputFieldRenderer form={form} formInput={{fieldLabel: 'Name'}} name="name" />, {wrapper});

        expect(screen.queryByText('Enter your full name')).not.toBeInTheDocument();
    });

    it('should render text input by default', () => {
        const {form, wrapper} = createMockForm();

        render(<InputFieldRenderer form={form} formInput={{fieldLabel: 'Text'}} name="textField" />, {wrapper});

        const input = screen.getByRole('textbox');
        expect(input).toHaveAttribute('type', 'text');
    });

    it('should render email input for EMAIL_INPUT fieldType', () => {
        const {form, wrapper} = createMockForm();

        render(
            <InputFieldRenderer
                form={form}
                formInput={{fieldLabel: 'Email', fieldType: FieldType.EMAIL_INPUT}}
                name="email"
            />,
            {wrapper}
        );

        const input = document.querySelector('input[type="email"]');
        expect(input).toBeInTheDocument();
    });

    it('should render number input for NUMBER_INPUT fieldType', () => {
        const {form, wrapper} = createMockForm();

        render(
            <InputFieldRenderer
                form={form}
                formInput={{fieldLabel: 'Age', fieldType: FieldType.NUMBER_INPUT}}
                name="age"
            />,
            {wrapper}
        );

        const input = screen.getByRole('spinbutton');
        expect(input).toHaveAttribute('type', 'number');
    });

    it('should render password input for PASSWORD_INPUT fieldType', () => {
        const {form, wrapper} = createMockForm();

        render(
            <InputFieldRenderer
                form={form}
                formInput={{fieldLabel: 'Password', fieldType: FieldType.PASSWORD_INPUT}}
                name="password"
            />,
            {wrapper}
        );

        const input = document.querySelector('input[type="password"]');
        expect(input).toBeInTheDocument();
    });

    it('should render text input for INPUT fieldType', () => {
        const {form, wrapper} = createMockForm();

        render(
            <InputFieldRenderer
                form={form}
                formInput={{fieldLabel: 'General', fieldType: FieldType.INPUT}}
                name="general"
            />,
            {wrapper}
        );

        const input = screen.getByRole('textbox');
        expect(input).toHaveAttribute('type', 'text');
    });

    it('should display placeholder when provided', () => {
        const {form, wrapper} = createMockForm();

        render(
            <InputFieldRenderer
                form={form}
                formInput={{fieldLabel: 'Search', placeholder: 'Type to search...'}}
                name="search"
            />,
            {wrapper}
        );

        const input = screen.getByRole('textbox');
        expect(input).toHaveAttribute('placeholder', 'Type to search...');
    });

    it('should update form value when typing', () => {
        const {form, formRef, wrapper} = createMockForm();

        render(<InputFieldRenderer form={form} formInput={{fieldLabel: 'Name'}} name="name" />, {wrapper});

        const input = screen.getByRole('textbox');
        fireEvent.change(input, {target: {value: 'John Doe'}});

        expect(formRef.current?.getValues('name')).toBe('John Doe');
    });

    it('should display existing string value from form', () => {
        const {form, wrapper} = createMockForm({name: 'Jane Doe'});

        render(<InputFieldRenderer form={form} formInput={{fieldLabel: 'Name'}} name="name" />, {wrapper});

        const input = screen.getByRole('textbox');
        expect(input).toHaveValue('Jane Doe');
    });

    it('should display existing numeric value from form', () => {
        const {form, wrapper} = createMockForm({age: 25});

        render(
            <InputFieldRenderer
                form={form}
                formInput={{fieldLabel: 'Age', fieldType: FieldType.NUMBER_INPUT}}
                name="age"
            />,
            {wrapper}
        );

        const input = screen.getByRole('spinbutton');
        expect(input).toHaveValue(25);
    });

    it('should display empty string for non-string/non-number values', () => {
        const {form, wrapper} = createMockForm({field: {complex: 'object'}});

        render(<InputFieldRenderer form={form} formInput={{fieldLabel: 'Field'}} name="field" />, {wrapper});

        const input = screen.getByRole('textbox');
        expect(input).toHaveValue('');
    });

    it('should handle undefined fieldType gracefully', () => {
        const {form, wrapper} = createMockForm();

        render(<InputFieldRenderer form={form} formInput={{fieldLabel: 'Undefined Type'}} name="undefinedType" />, {
            wrapper,
        });

        const input = screen.getByRole('textbox');
        expect(input).toHaveAttribute('type', 'text');
    });
});
