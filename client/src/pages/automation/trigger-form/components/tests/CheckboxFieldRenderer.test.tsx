import {fireEvent, render, screen} from '@/shared/util/test-utils';
import {describe, expect, it} from 'vitest';

import {CheckboxFieldRenderer} from '../CheckboxFieldRenderer';
import {createMockForm} from './testUtils';

describe('CheckboxFieldRenderer', () => {
    it('should render with fieldLabel', () => {
        const {form, wrapper} = createMockForm();

        render(<CheckboxFieldRenderer form={form} formInput={{fieldLabel: 'Accept Terms'}} name="acceptTerms" />, {
            wrapper,
        });

        expect(screen.getByText('Accept Terms')).toBeInTheDocument();
    });

    it('should render with fieldName when fieldLabel is not provided', () => {
        const {form, wrapper} = createMockForm();

        render(<CheckboxFieldRenderer form={form} formInput={{fieldName: 'termsField'}} name="acceptTerms" />, {
            wrapper,
        });

        expect(screen.getByText('termsField')).toBeInTheDocument();
    });

    it('should render with name when fieldLabel and fieldName are not provided', () => {
        const {form, wrapper} = createMockForm();

        render(<CheckboxFieldRenderer form={form} formInput={{}} name="fallbackName" />, {wrapper});

        expect(screen.getByText('fallbackName')).toBeInTheDocument();
    });

    it('should render placeholder as label when provided', () => {
        const {form, wrapper} = createMockForm();

        render(
            <CheckboxFieldRenderer
                form={form}
                formInput={{fieldLabel: 'Original Label', placeholder: 'Placeholder Label'}}
                name="checkbox"
            />,
            {wrapper}
        );

        expect(screen.getByText('Placeholder Label')).toBeInTheDocument();
    });

    it('should render field description when provided', () => {
        const {form, wrapper} = createMockForm();

        render(
            <CheckboxFieldRenderer
                form={form}
                formInput={{fieldDescription: 'This is a helpful description', fieldLabel: 'Checkbox'}}
                name="checkbox"
            />,
            {wrapper}
        );

        expect(screen.getByText('This is a helpful description')).toBeInTheDocument();
    });

    it('should not render field description when not provided', () => {
        const {form, wrapper} = createMockForm();

        render(<CheckboxFieldRenderer form={form} formInput={{fieldLabel: 'Checkbox'}} name="checkbox" />, {wrapper});

        expect(screen.queryByText('This is a helpful description')).not.toBeInTheDocument();
    });

    it('should render checkbox input', () => {
        const {form, wrapper} = createMockForm();

        render(<CheckboxFieldRenderer form={form} formInput={{fieldLabel: 'Subscribe'}} name="subscribe" />, {wrapper});

        expect(screen.getByRole('checkbox')).toBeInTheDocument();
    });

    it('should toggle checkbox when clicked', () => {
        const {form, formRef, wrapper} = createMockForm();

        render(<CheckboxFieldRenderer form={form} formInput={{fieldLabel: 'Subscribe'}} name="subscribe" />, {wrapper});

        const checkbox = screen.getByRole('checkbox');
        expect(checkbox).not.toBeChecked();

        fireEvent.click(checkbox);

        expect(formRef.current?.getValues('subscribe')).toBe(true);
    });

    it('should reflect checked state from form default value', () => {
        const {form, wrapper} = createMockForm({subscribe: true});

        render(<CheckboxFieldRenderer form={form} formInput={{fieldLabel: 'Subscribe'}} name="subscribe" />, {wrapper});

        expect(screen.getByRole('checkbox')).toBeChecked();
    });
});
