import {render} from '@/shared/util/test-utils';
import {describe, expect, it} from 'vitest';

import {HiddenFieldRenderer} from '../HiddenFieldRenderer';
import {createMockForm} from './testUtils';

describe('HiddenFieldRenderer', () => {
    it('should render a hidden input element', () => {
        const {form, wrapper} = createMockForm();

        render(<HiddenFieldRenderer form={form} formInput={{}} name="hiddenField" />, {wrapper});

        const hiddenInput = document.querySelector('input[type="hidden"]');
        expect(hiddenInput).toBeInTheDocument();
    });

    it('should use form field value when available', () => {
        const {form, wrapper} = createMockForm({hiddenField: 'form-value'});

        render(<HiddenFieldRenderer form={form} formInput={{}} name="hiddenField" />, {wrapper});

        const hiddenInput = document.querySelector('input[type="hidden"]') as HTMLInputElement;
        expect(hiddenInput.value).toBe('form-value');
    });

    it('should use defaultValue when form field value is undefined', () => {
        const {form, wrapper} = createMockForm();

        render(<HiddenFieldRenderer form={form} formInput={{defaultValue: 'default-value'}} name="hiddenField" />, {
            wrapper,
        });

        const hiddenInput = document.querySelector('input[type="hidden"]') as HTMLInputElement;
        expect(hiddenInput.value).toBe('default-value');
    });

    it('should prefer form field value over defaultValue', () => {
        const {form, wrapper} = createMockForm({hiddenField: 'form-value'});

        render(<HiddenFieldRenderer form={form} formInput={{defaultValue: 'default-value'}} name="hiddenField" />, {
            wrapper,
        });

        const hiddenInput = document.querySelector('input[type="hidden"]') as HTMLInputElement;
        expect(hiddenInput.value).toBe('form-value');
    });

    it('should render empty string when both form value and defaultValue are undefined', () => {
        const {form, wrapper} = createMockForm();

        render(<HiddenFieldRenderer form={form} formInput={{}} name="hiddenField" />, {wrapper});

        const hiddenInput = document.querySelector('input[type="hidden"]') as HTMLInputElement;
        expect(hiddenInput.value).toBe('');
    });

    it('should convert numeric form value to string', () => {
        const {form, wrapper} = createMockForm({hiddenField: 12345});

        render(<HiddenFieldRenderer form={form} formInput={{}} name="hiddenField" />, {wrapper});

        const hiddenInput = document.querySelector('input[type="hidden"]') as HTMLInputElement;
        expect(hiddenInput.value).toBe('12345');
    });

    it('should use string defaultValue', () => {
        const {form, wrapper} = createMockForm();

        render(<HiddenFieldRenderer form={form} formInput={{defaultValue: '99999'}} name="hiddenField" />, {wrapper});

        const hiddenInput = document.querySelector('input[type="hidden"]') as HTMLInputElement;
        expect(hiddenInput.value).toBe('99999');
    });

    it('should handle boolean form value', () => {
        const {form, wrapper} = createMockForm({hiddenField: true});

        render(<HiddenFieldRenderer form={form} formInput={{}} name="hiddenField" />, {wrapper});

        const hiddenInput = document.querySelector('input[type="hidden"]') as HTMLInputElement;
        expect(hiddenInput.value).toBe('true');
    });

    it('should not be visible to the user', () => {
        const {form, wrapper} = createMockForm({hiddenField: 'secret'});

        const {container} = render(<HiddenFieldRenderer form={form} formInput={{}} name="hiddenField" />, {wrapper});

        const hiddenInput = container.querySelector('input[type="hidden"]');
        expect(hiddenInput).toBeInTheDocument();

        const visibleElements = container.querySelectorAll('label, p, span:not(:empty)');
        expect(visibleElements.length).toBe(0);
    });
});
