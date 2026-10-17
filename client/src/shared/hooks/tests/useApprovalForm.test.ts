import {FieldType} from '@/shared/constants';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {mutateAsyncMock, resumeJobMutationState, useGetApprovalFormQueryMock} = vi.hoisted(() => ({
    mutateAsyncMock: vi.fn(),
    resumeJobMutationState: {error: null as Error | null, isPending: false, isSuccess: false},
    useGetApprovalFormQueryMock: vi.fn(),
}));

vi.mock('@/shared/queries/platform/approvalForms.queries', () => ({
    useGetApprovalFormQuery: useGetApprovalFormQueryMock,
}));

vi.mock('@/shared/mutations/platform/resumeJobs.mutations', () => ({
    useResumeJobMutation: () => ({...resumeJobMutationState, mutateAsync: mutateAsyncMock}),
}));

import useApprovalForm, {toSubmittedValues} from '../useApprovalForm';

describe('toSubmittedValues', () => {
    it('submits a number input as a number', () => {
        const submittedValues = toSubmittedValues(
            [
                {fieldName: 'amount', fieldType: FieldType.NUMBER_INPUT},
                {fieldName: 'price', fieldType: FieldType.NUMBER_INPUT},
            ],
            {amount: '7', price: ' 7.5 '}
        );

        expect(submittedValues).toEqual({amount: 7, price: 7.5});
    });

    it('omits an empty number input and keeps an unparseable one for the server to reject', () => {
        const submittedValues = toSubmittedValues(
            [
                {fieldName: 'amount', fieldType: FieldType.NUMBER_INPUT},
                {fieldName: 'count', fieldType: FieldType.NUMBER_INPUT},
            ],
            {amount: '', count: 'seven'}
        );

        expect(submittedValues).toEqual({count: 'seven'});
    });

    it('leaves the values of other inputs untouched', () => {
        const submittedValues = toSubmittedValues(
            [
                {fieldName: 'reason', fieldType: FieldType.INPUT},
                {fieldName: 'amount', fieldType: FieldType.NUMBER_INPUT},
            ],
            {amount: 3, comment: 'ok', reason: '42'}
        );

        expect(submittedValues).toEqual({amount: 3, comment: 'ok', reason: '42'});
    });
});

describe('useApprovalForm', () => {
    const definition = {
        environmentId: 1,
        formDescription: 'Spend approval',
        formTitle: 'Approve budget',
        inputs: [
            {defaultValue: '5', fieldName: 'amount', fieldType: FieldType.NUMBER_INPUT},
            {defaultValue: 1, fieldName: 'urgent', fieldType: FieldType.CHECKBOX},
            {defaultValue: '<b>note</b>', fieldName: 'note', fieldType: FieldType.CUSTOM_HTML},
        ],
    };

    beforeEach(() => {
        mutateAsyncMock.mockReset();
        resumeJobMutationState.error = null;
        resumeJobMutationState.isPending = false;
        resumeJobMutationState.isSuccess = false;
        useGetApprovalFormQueryMock.mockReset();
        useGetApprovalFormQueryMock.mockReturnValue({data: definition, error: null, isLoading: false});
    });

    it('derives the ui definition and seeds the form defaults from the inputs', () => {
        const {result} = renderHook(() => useApprovalForm('abc123'));

        expect(result.current.uiDefinition).toEqual({
            description: 'Spend approval',
            environmentId: 1,
            environmentName: expect.any(String),
            inputs: definition.inputs,
            title: 'Approve budget',
        });
        expect(result.current.form.getValues()).toEqual({amount: '5', urgent: true});
    });

    it('falls back to a default title when the definition has none', () => {
        useGetApprovalFormQueryMock.mockReturnValue({data: {inputs: undefined}, error: null, isLoading: false});

        const {result} = renderHook(() => useApprovalForm('abc123'));

        expect(result.current.uiDefinition).toMatchObject({
            environmentName: null,
            inputs: [],
            title: 'Approval Request',
        });
    });

    it('resumes the job with the converted values and reports the decision', async () => {
        const onSubmitted = vi.fn();

        mutateAsyncMock.mockResolvedValue(undefined);

        const {result} = renderHook(() => useApprovalForm('abc123', {onSubmitted}));

        await act(async () => {
            await result.current.handleSubmit({amount: ' 12 '}, true);
        });

        expect(mutateAsyncMock).toHaveBeenCalledWith({approved: true, data: {amount: 12}, id: 'abc123'});
        expect(onSubmitted).toHaveBeenCalledWith(true);
        expect(result.current.approved).toBe(true);
    });

    it('keeps the decision unset and surfaces the mutation error when resuming fails', async () => {
        const onSubmitted = vi.fn();

        mutateAsyncMock.mockRejectedValue(new Error('gone'));
        resumeJobMutationState.error = new Error('Approval expired');

        const {result} = renderHook(() => useApprovalForm('abc123', {onSubmitted}));

        await act(async () => {
            await result.current.handleSubmit({}, false);
        });

        expect(onSubmitted).not.toHaveBeenCalled();
        expect(result.current.approved).toBeNull();
        expect(result.current.submitError).toBe('Approval expired');
    });

    it('does nothing without an approval id', async () => {
        const {result} = renderHook(() => useApprovalForm(undefined));

        await act(async () => {
            await result.current.handleSubmit({}, true);
        });

        expect(mutateAsyncMock).not.toHaveBeenCalled();
    });

    it('routes the decision through submitOverride instead of resuming the job', async () => {
        const onSubmitted = vi.fn();
        const submitOverride = vi.fn().mockResolvedValue(undefined);

        const {result} = renderHook(() => useApprovalForm('abc123', {onSubmitted, submitOverride}));

        await act(async () => {
            await result.current.handleSubmit({amount: '3'}, false);
        });

        expect(submitOverride).toHaveBeenCalledWith({amount: 3}, false);
        expect(mutateAsyncMock).not.toHaveBeenCalled();
        expect(onSubmitted).toHaveBeenCalledWith(false);
        expect(result.current.approved).toBe(false);
        expect(result.current.submitted).toBe(true);
        expect(result.current.submitting).toBe(false);
        expect(result.current.submitError).toBeNull();
    });

    it('surfaces a submitOverride failure without marking the approval submitted', async () => {
        const submitOverride = vi.fn().mockRejectedValue(new Error('Already resolved'));

        const {result} = renderHook(() => useApprovalForm('abc123', {submitOverride}));

        await act(async () => {
            await result.current.handleSubmit({}, true);
        });

        expect(result.current.submitError).toBe('Already resolved');
        expect(result.current.submitted).toBe(false);
        expect(result.current.approved).toBeNull();
    });

    it('uses a generic message when submitOverride rejects with a non-error value', async () => {
        const submitOverride = vi.fn().mockRejectedValue('nope');

        const {result} = renderHook(() => useApprovalForm('abc123', {submitOverride}));

        await act(async () => {
            await result.current.handleSubmit({}, true);
        });

        expect(result.current.submitError).toBe('Failed to submit the approval.');
    });
});
