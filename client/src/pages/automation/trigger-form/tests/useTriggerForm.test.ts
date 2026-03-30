import {TriggerForm as TriggerFormType} from '@/shared/middleware/automation/configuration';
import {act, renderHook, waitFor} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {createTestQueryClientWrapper} from '../../../../shared/util/test-utils';
import {FieldType} from '../TriggerForm';
import useTriggerForm from '../hooks/useTriggerForm';

const mockWorkflowExecutionId = 'test-execution-id';
const mockEnvironmentId = '1';

vi.mock('react-router-dom', () => ({
    useParams: () => ({
        environmentId: mockEnvironmentId,
        workflowExecutionId: mockWorkflowExecutionId,
    }),
}));

const {mockGetTriggerForm} = vi.hoisted(() => ({
    mockGetTriggerForm: vi.fn(),
}));

vi.mock('@/shared/middleware/automation/configuration', async (importOriginal) => {
    const actual = (await importOriginal()) as Record<string, unknown>;

    return {
        ...actual,
        TriggerFormApi: vi.fn().mockImplementation(function () {
            return {
                getTriggerForm: mockGetTriggerForm,
            };
        }),
    };
});

const mockDefinition: TriggerFormType = {
    appendAttribution: true,
    buttonLabel: 'Submit Test',
    customFormStyling: '.custom { color: red; }',
    formDescription: 'Test Description',
    formTitle: 'Test Form',
    ignoreBots: true,
    inputs: [
        {
            defaultValue: 'default text',
            fieldLabel: 'Text Field',
            fieldName: 'textField',
            fieldType: FieldType.INPUT,
            required: true,
        },
        {
            defaultValue: 'true',
            fieldLabel: 'Checkbox Field',
            fieldName: 'checkboxField',
            fieldType: FieldType.CHECKBOX,
            required: false,
        },
        {
            fieldLabel: 'Custom HTML',
            fieldName: 'customHtml',
            fieldType: FieldType.CUSTOM_HTML,
            required: false,
        },
    ],
    useWorkflowTimezone: true,
};

describe('useTriggerForm', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        global.fetch = vi.fn();
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('should return loading state initially', () => {
        mockGetTriggerForm.mockReturnValue(new Promise(() => {}));

        const {result} = renderHook(() => useTriggerForm(), {wrapper: createTestQueryClientWrapper()});

        expect(result.current.loading).toBe(true);
        expect(result.current.definition).toBeUndefined();
        expect(result.current.uiDefinition).toBeNull();
    });

    it('should return definition and uiDefinition after data loads', async () => {
        mockGetTriggerForm.mockResolvedValue(mockDefinition);

        const {result} = renderHook(() => useTriggerForm(), {wrapper: createTestQueryClientWrapper()});

        await waitFor(() => expect(result.current.loading).toBe(false));

        expect(result.current.definition).toEqual(mockDefinition);
        expect(result.current.uiDefinition).toEqual({
            appendAttribution: true,
            buttonLabel: 'Submit Test',
            customFormStyling: '.custom { color: red; }',
            inputs: mockDefinition.inputs,
            subtitle: 'Test Description',
            title: 'Test Form',
        });
    });

    it('should apply default values from definition', async () => {
        mockGetTriggerForm.mockResolvedValue(mockDefinition);

        const {result} = renderHook(() => useTriggerForm(), {wrapper: createTestQueryClientWrapper()});

        await waitFor(() => expect(result.current.loading).toBe(false));

        const formValues = result.current.form.getValues();

        expect(formValues.textField).toBe('default text');
        expect(formValues.checkboxField).toBe(true);
    });

    it('should skip CUSTOM_HTML fields when setting default values', async () => {
        mockGetTriggerForm.mockResolvedValue(mockDefinition);

        const {result} = renderHook(() => useTriggerForm(), {wrapper: createTestQueryClientWrapper()});

        await waitFor(() => expect(result.current.loading).toBe(false));

        const formValues = result.current.form.getValues();

        expect(formValues.customHtml).toBeUndefined();
    });

    it('should use fallback values when definition fields are empty', async () => {
        const minimalDefinition: TriggerFormType = {
            ignoreBots: false,
            inputs: [],
            useWorkflowTimezone: false,
        };

        mockGetTriggerForm.mockResolvedValue(minimalDefinition);

        const {result} = renderHook(() => useTriggerForm(), {wrapper: createTestQueryClientWrapper()});

        await waitFor(() => expect(result.current.loading).toBe(false));

        expect(result.current.uiDefinition).toEqual({
            appendAttribution: true,
            buttonLabel: 'Submit',
            customFormStyling: undefined,
            inputs: [],
            subtitle: '',
            title: 'Form',
        });
    });

    it('should return error when query fails', async () => {
        mockGetTriggerForm.mockRejectedValue(new Error('Network error'));

        const {result} = renderHook(() => useTriggerForm(), {wrapper: createTestQueryClientWrapper()});

        await waitFor(() => expect(result.current.error).toBeTruthy());

        expect(result.current.error?.message).toBe('Network error');
    });

    it('should return environmentId and environmentName from params', () => {
        mockGetTriggerForm.mockReturnValue(new Promise(() => {}));

        const {result} = renderHook(() => useTriggerForm(), {wrapper: createTestQueryClientWrapper()});

        expect(result.current.environmentId).toBe(mockEnvironmentId);
        expect(result.current.environmentName).toBeDefined();
    });

    it('should submit JSON body for non-file forms', async () => {
        mockGetTriggerForm.mockResolvedValue(mockDefinition);
        vi.spyOn(global, 'fetch').mockResolvedValue({ok: true} as Response);

        const {result} = renderHook(() => useTriggerForm(), {wrapper: createTestQueryClientWrapper()});

        await waitFor(() => expect(result.current.loading).toBe(false));

        await act(async () => {
            await result.current.handleSubmit({textField: 'hello'});
        });

        expect(result.current.submitted).toBe(true);
        expect(result.current.submitting).toBe(false);
        expect(global.fetch).toHaveBeenCalledWith(
            `/webhooks/${mockWorkflowExecutionId}`,
            expect.objectContaining({
                body: expect.stringContaining('"textField":"hello"'),
                headers: {'Content-Type': 'application/json'},
                method: 'POST',
            })
        );
    });

    it('should submit FormData body when files are present', async () => {
        const fileDefinition: TriggerFormType = {
            ...mockDefinition,
            inputs: [
                {
                    fieldLabel: 'File',
                    fieldName: 'fileField',
                    fieldType: FieldType.FILE_INPUT,
                    required: true,
                },
            ],
        };

        mockGetTriggerForm.mockResolvedValue(fileDefinition);
        vi.spyOn(global, 'fetch').mockResolvedValue({ok: true} as Response);

        const {result} = renderHook(() => useTriggerForm(), {wrapper: createTestQueryClientWrapper()});

        await waitFor(() => expect(result.current.loading).toBe(false));

        const testFile = new File(['content'], 'test.txt', {type: 'text/plain'});

        await act(async () => {
            await result.current.handleSubmit({fileField: testFile});
        });

        expect(result.current.submitted).toBe(true);
        expect(global.fetch).toHaveBeenCalledWith(
            `/webhooks/${mockWorkflowExecutionId}`,
            expect.objectContaining({
                body: expect.any(FormData),
                method: 'POST',
            })
        );
    });

    it('should set submitError when submission fails', async () => {
        mockGetTriggerForm.mockResolvedValue(mockDefinition);
        vi.spyOn(global, 'fetch').mockResolvedValue({
            ok: false,
            statusText: 'Internal Server Error',
        } as Response);

        const {result} = renderHook(() => useTriggerForm(), {wrapper: createTestQueryClientWrapper()});

        await waitFor(() => expect(result.current.loading).toBe(false));

        await act(async () => {
            await result.current.handleSubmit({textField: 'value'});
        });

        expect(result.current.submitted).toBe(false);
        expect(result.current.submitError).toBe('Submission failed: Internal Server Error');
    });

    it('should set submitError when fetch throws', async () => {
        mockGetTriggerForm.mockResolvedValue(mockDefinition);
        vi.spyOn(global, 'fetch').mockRejectedValue(new Error('Network failure'));

        const {result} = renderHook(() => useTriggerForm(), {wrapper: createTestQueryClientWrapper()});

        await waitFor(() => expect(result.current.loading).toBe(false));

        await act(async () => {
            await result.current.handleSubmit({textField: 'value'});
        });

        expect(result.current.submitted).toBe(false);
        expect(result.current.submitError).toBe('Network failure');
    });

    it('should handle FormData with array values', async () => {
        const fileDefinition: TriggerFormType = {
            ...mockDefinition,
            inputs: [
                {
                    fieldLabel: 'Files',
                    fieldName: 'files',
                    fieldType: FieldType.FILE_INPUT,
                    required: true,
                },
            ],
        };

        mockGetTriggerForm.mockResolvedValue(fileDefinition);

        const appendSpy = vi.spyOn(FormData.prototype, 'append');

        vi.spyOn(global, 'fetch').mockResolvedValue({ok: true} as Response);

        const {result} = renderHook(() => useTriggerForm(), {wrapper: createTestQueryClientWrapper()});

        await waitFor(() => expect(result.current.loading).toBe(false));

        const file1 = new File(['a'], 'a.txt', {type: 'text/plain'});
        const file2 = new File(['b'], 'b.txt', {type: 'text/plain'});

        await act(async () => {
            await result.current.handleSubmit({files: [file1, file2]});
        });

        expect(appendSpy).toHaveBeenCalledWith('body.files', file1);
        expect(appendSpy).toHaveBeenCalledWith('body.files', file2);
    });
});
