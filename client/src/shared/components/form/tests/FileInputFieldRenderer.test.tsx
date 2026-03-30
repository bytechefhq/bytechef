import {fireEvent, render, screen} from '@/shared/util/test-utils';
import {describe, expect, it} from 'vitest';

import {FileInputFieldRenderer} from '../FileInputFieldRenderer';
import {createMockForm} from './testUtils';

describe('FileInputFieldRenderer', () => {
    it('should render with fieldLabel', () => {
        const {form, wrapper} = createMockForm();

        render(<FileInputFieldRenderer form={form} formInput={{fieldLabel: 'Upload File'}} name="fileUpload" />, {
            wrapper,
        });

        expect(screen.getByText('Upload File')).toBeInTheDocument();
    });

    it('should render with fieldName when fieldLabel is not provided', () => {
        const {form, wrapper} = createMockForm();

        render(<FileInputFieldRenderer form={form} formInput={{fieldName: 'documentFile'}} name="fileUpload" />, {
            wrapper,
        });

        expect(screen.getByText('documentFile')).toBeInTheDocument();
    });

    it('should render with name when fieldLabel and fieldName are not provided', () => {
        const {form, wrapper} = createMockForm();

        render(<FileInputFieldRenderer form={form} formInput={{}} name="fallbackFile" />, {wrapper});

        expect(screen.getByText('fallbackFile')).toBeInTheDocument();
    });

    it('should render field description when provided', () => {
        const {form, wrapper} = createMockForm();

        render(
            <FileInputFieldRenderer
                form={form}
                formInput={{fieldDescription: 'Please upload a PDF document', fieldLabel: 'Document'}}
                name="document"
            />,
            {wrapper}
        );

        expect(screen.getByText('Please upload a PDF document')).toBeInTheDocument();
    });

    it('should not render field description when not provided', () => {
        const {form, wrapper} = createMockForm();

        render(<FileInputFieldRenderer form={form} formInput={{fieldLabel: 'Document'}} name="document" />, {wrapper});

        expect(screen.queryByText('Please upload a PDF document')).not.toBeInTheDocument();
    });

    it('should render file input element', () => {
        const {form, wrapper} = createMockForm();

        render(<FileInputFieldRenderer form={form} formInput={{fieldLabel: 'File'}} name="file" />, {wrapper});

        const fileInput = document.querySelector('input[type="file"]');
        expect(fileInput).toBeInTheDocument();
    });

    it('should have correct id attribute on file input', () => {
        const {form, wrapper} = createMockForm();

        render(<FileInputFieldRenderer form={form} formInput={{fieldLabel: 'Upload'}} name="uploadField" />, {wrapper});

        const fileInput = document.querySelector('input[type="file"]#uploadField');
        expect(fileInput).toBeInTheDocument();
    });

    it('should update form value when file is selected', () => {
        const {form, formRef, wrapper} = createMockForm();

        render(<FileInputFieldRenderer form={form} formInput={{fieldLabel: 'File'}} name="fileField" />, {wrapper});

        const file = new File(['test content'], 'test.txt', {type: 'text/plain'});
        const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;

        fireEvent.change(fileInput, {target: {files: [file]}});

        const formValue = formRef.current?.getValues('fileField');
        expect(formValue).toBeInstanceOf(File);
        expect((formValue as File).name).toBe('test.txt');
    });

    it('should set form value to null when file selection is cancelled', () => {
        const {form, formRef, wrapper} = createMockForm();

        render(<FileInputFieldRenderer form={form} formInput={{fieldLabel: 'File'}} name="fileField" />, {wrapper});

        const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;

        fireEvent.change(fileInput, {target: {files: null}});

        expect(formRef.current?.getValues('fileField')).toBeNull();
    });

    it('should handle different file types', () => {
        const {form, formRef, wrapper} = createMockForm();

        render(<FileInputFieldRenderer form={form} formInput={{fieldLabel: 'Image'}} name="imageField" />, {wrapper});

        const imageFile = new File(['image data'], 'photo.png', {type: 'image/png'});
        const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;

        fireEvent.change(fileInput, {target: {files: [imageFile]}});

        const formValue = formRef.current?.getValues('imageField') as File;
        expect(formValue.name).toBe('photo.png');
        expect(formValue.type).toBe('image/png');
    });
});
