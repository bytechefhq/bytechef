import {describe, expect, it} from 'vitest';

import {
    ApprovalFieldType,
    canRenderApprovalFormInChat,
    getApprovalFormDefaultValues,
    toApprovalFormData,
    validateApprovalForm,
} from '@/utils/approval-form';

describe('canRenderApprovalFormInChat', () => {
    it('accepts an empty form and the plain field types', () => {
        expect(canRenderApprovalFormInChat([])).toBe(true);
        expect(
            canRenderApprovalFormInChat([
                {fieldName: 'amount', fieldType: ApprovalFieldType.NUMBER_INPUT},
                {fieldName: 'reason', fieldType: ApprovalFieldType.TEXTAREA},
                {fieldName: 'team', fieldType: ApprovalFieldType.SELECT, multipleChoice: true},
            ])
        ).toBe(true);
    });

    it('rejects file inputs, custom HTML and fields without a name', () => {
        expect(canRenderApprovalFormInChat([{fieldName: 'receipt', fieldType: ApprovalFieldType.FILE_INPUT}])).toBe(
            false
        );
        expect(canRenderApprovalFormInChat([{fieldName: 'note', fieldType: ApprovalFieldType.CUSTOM_HTML}])).toBe(
            false
        );
        expect(canRenderApprovalFormInChat([{fieldType: ApprovalFieldType.INPUT}])).toBe(false);
    });
});

describe('getApprovalFormDefaultValues', () => {
    it('maps defaults to the value each control edits', () => {
        expect(
            getApprovalFormDefaultValues([
                {defaultValue: 'true', fieldName: 'urgent', fieldType: ApprovalFieldType.CHECKBOX},
                {defaultValue: 'false', fieldName: 'notify', fieldType: ApprovalFieldType.CHECKBOX},
                {fieldName: 'teams', fieldType: ApprovalFieldType.SELECT, multipleChoice: true},
                {defaultValue: 42, fieldName: 'amount', fieldType: ApprovalFieldType.NUMBER_INPUT},
                {defaultValue: 'abc', fieldName: 'requestId', fieldType: ApprovalFieldType.HIDDEN_FIELD},
                {fieldName: 'reason', fieldType: ApprovalFieldType.TEXTAREA},
            ])
        ).toEqual({amount: '42', notify: false, reason: '', requestId: 'abc', teams: [], urgent: true});
    });
});

describe('validateApprovalForm', () => {
    const inputs = [
        {fieldName: 'reason', fieldType: ApprovalFieldType.INPUT, required: true},
        {fieldName: 'confirmed', fieldType: ApprovalFieldType.CHECKBOX, required: true},
        {
            fieldName: 'teams',
            fieldType: ApprovalFieldType.SELECT,
            maxSelection: 2,
            minSelection: 1,
            multipleChoice: true,
        },
        {fieldName: 'amount', fieldType: ApprovalFieldType.NUMBER_INPUT},
    ];

    it('reports required, selection count and number errors', () => {
        expect(validateApprovalForm(inputs, {amount: 'ten', confirmed: false, reason: ' ', teams: []})).toEqual({
            amount: 'Enter a number',
            confirmed: 'This field is required',
            reason: 'This field is required',
            teams: 'Select at least 1 option',
        });

        expect(validateApprovalForm(inputs, {confirmed: true, reason: 'ok', teams: ['a', 'b', 'c']})).toEqual({
            teams: 'Select at most 2 options',
        });
    });

    it('accepts a complete form', () => {
        expect(validateApprovalForm(inputs, {amount: '10', confirmed: true, reason: 'ok', teams: ['a']})).toEqual({});
    });
});

describe('toApprovalFormData', () => {
    it('converts numbers and dates and drops empty ones', () => {
        const data = toApprovalFormData(
            [
                {fieldName: 'amount', fieldType: ApprovalFieldType.NUMBER_INPUT},
                {fieldName: 'limit', fieldType: ApprovalFieldType.NUMBER_INPUT},
                {fieldName: 'dueDate', fieldType: ApprovalFieldType.DATE_PICKER},
                {fieldName: 'startAt', fieldType: ApprovalFieldType.DATETIME_PICKER},
                {fieldName: 'endAt', fieldType: ApprovalFieldType.DATETIME_PICKER},
                {fieldName: 'urgent', fieldType: ApprovalFieldType.CHECKBOX},
                {fieldName: 'reason', fieldType: ApprovalFieldType.TEXTAREA},
            ],
            {
                amount: ' 12.5 ',
                dueDate: '2026-11-02',
                endAt: '',
                limit: '',
                reason: 'ok',
                startAt: '2026-11-02T09:30',
                urgent: true,
            }
        );

        expect(data).toEqual({
            amount: 12.5,
            dueDate: new Date(2026, 10, 2).toISOString(),
            reason: 'ok',
            startAt: new Date(2026, 10, 2, 9, 30).toISOString(),
            urgent: true,
        });
    });
});
