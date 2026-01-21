import {fireEvent, render, screen} from '@/shared/util/test-utils';
import {describe, expect, it} from 'vitest';

import {TextAreaFieldRenderer} from '../TextAreaFieldRenderer';
import {createMockForm} from './testUtils';

describe('TextAreaFieldRenderer', () => {
    it('should render with fieldLabel', () => {
        const {form, wrapper} = createMockForm();

        render(<TextAreaFieldRenderer form={form} formInput={{fieldLabel: 'Description'}} name="description" />, {
            wrapper,
        });

        expect(screen.getByText('Description')).toBeInTheDocument();
    });

    it('should render with fieldName when fieldLabel is not provided', () => {
        const {form, wrapper} = createMockForm();

        render(<TextAreaFieldRenderer form={form} formInput={{fieldName: 'descriptionField'}} name="description" />, {
            wrapper,
        });

        expect(screen.getByText('descriptionField')).toBeInTheDocument();
    });

    it('should render with name when fieldLabel and fieldName are not provided', () => {
        const {form, wrapper} = createMockForm();

        render(<TextAreaFieldRenderer form={form} formInput={{}} name="fallbackTextArea" />, {wrapper});

        expect(screen.getByText('fallbackTextArea')).toBeInTheDocument();
    });

    it('should render field description when provided', () => {
        const {form, wrapper} = createMockForm();

        render(
            <TextAreaFieldRenderer
                form={form}
                formInput={{fieldDescription: 'Enter a detailed description', fieldLabel: 'Description'}}
                name="description"
            />,
            {wrapper}
        );

        expect(screen.getByText('Enter a detailed description')).toBeInTheDocument();
    });

    it('should not render field description when not provided', () => {
        const {form, wrapper} = createMockForm();

        render(<TextAreaFieldRenderer form={form} formInput={{fieldLabel: 'Description'}} name="description" />, {
            wrapper,
        });

        expect(screen.queryByText('Enter a detailed description')).not.toBeInTheDocument();
    });

    it('should render textarea element', () => {
        const {form, wrapper} = createMockForm();

        render(<TextAreaFieldRenderer form={form} formInput={{fieldLabel: 'Notes'}} name="notes" />, {wrapper});

        expect(screen.getByRole('textbox')).toBeInTheDocument();
    });

    it('should display placeholder when provided', () => {
        const {form, wrapper} = createMockForm();

        render(
            <TextAreaFieldRenderer
                form={form}
                formInput={{fieldLabel: 'Comments', placeholder: 'Enter your comments here...'}}
                name="comments"
            />,
            {wrapper}
        );

        const textarea = screen.getByRole('textbox');
        expect(textarea).toHaveAttribute('placeholder', 'Enter your comments here...');
    });

    it('should update form value when typing', () => {
        const {form, formRef, wrapper} = createMockForm();

        render(<TextAreaFieldRenderer form={form} formInput={{fieldLabel: 'Notes'}} name="notes" />, {wrapper});

        const textarea = screen.getByRole('textbox');
        fireEvent.change(textarea, {target: {value: 'This is a test note.'}});

        expect(formRef.current?.getValues('notes')).toBe('This is a test note.');
    });

    it('should display existing string value from form', () => {
        const {form, wrapper} = createMockForm({notes: 'Existing note content'});

        render(<TextAreaFieldRenderer form={form} formInput={{fieldLabel: 'Notes'}} name="notes" />, {wrapper});

        const textarea = screen.getByRole('textbox');
        expect(textarea).toHaveValue('Existing note content');
    });

    it('should display empty string for non-string values', () => {
        const {form, wrapper} = createMockForm({notes: {complex: 'object'}});

        render(<TextAreaFieldRenderer form={form} formInput={{fieldLabel: 'Notes'}} name="notes" />, {wrapper});

        const textarea = screen.getByRole('textbox');
        expect(textarea).toHaveValue('');
    });

    it('should handle multiline text input', () => {
        const {form, formRef, wrapper} = createMockForm();
        const multilineText = 'Line 1\nLine 2\nLine 3';

        render(<TextAreaFieldRenderer form={form} formInput={{fieldLabel: 'Content'}} name="content" />, {wrapper});

        const textarea = screen.getByRole('textbox');
        fireEvent.change(textarea, {target: {value: multilineText}});

        expect(formRef.current?.getValues('content')).toBe(multilineText);
    });

    it('should preserve existing multiline value', () => {
        const multilineText = 'First line\nSecond line\nThird line';
        const {form, wrapper} = createMockForm({content: multilineText});

        render(<TextAreaFieldRenderer form={form} formInput={{fieldLabel: 'Content'}} name="content" />, {wrapper});

        const textarea = screen.getByRole('textbox');
        expect(textarea).toHaveValue(multilineText);
    });

    it('should handle empty string value', () => {
        const {form, wrapper} = createMockForm({text: ''});

        render(<TextAreaFieldRenderer form={form} formInput={{fieldLabel: 'Text'}} name="text" />, {wrapper});

        const textarea = screen.getByRole('textbox');
        expect(textarea).toHaveValue('');
    });

    it('should handle special characters in text', () => {
        const specialText = '<script>alert("test")</script> & "quotes" \'apostrophes\'';
        const {form, formRef, wrapper} = createMockForm();

        render(<TextAreaFieldRenderer form={form} formInput={{fieldLabel: 'Special'}} name="special" />, {wrapper});

        const textarea = screen.getByRole('textbox');
        fireEvent.change(textarea, {target: {value: specialText}});

        expect(formRef.current?.getValues('special')).toBe(specialText);
    });

    it('should handle numeric value by converting to empty string', () => {
        const {form, wrapper} = createMockForm({numericField: 12345});

        render(<TextAreaFieldRenderer form={form} formInput={{fieldLabel: 'Numeric'}} name="numericField" />, {
            wrapper,
        });

        const textarea = screen.getByRole('textbox');
        expect(textarea).toHaveValue('');
    });
});
