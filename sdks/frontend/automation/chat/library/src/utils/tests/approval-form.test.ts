import {afterEach, beforeEach, describe, expect, it} from 'vitest';

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

describe('date defaults west of UTC', () => {
    const originalTimeZone = process.env.TZ;

    beforeEach(() => {
        process.env.TZ = 'America/Los_Angeles';
    });

    afterEach(() => {
        process.env.TZ = originalTimeZone;
    });

    const inputs = [
        {defaultValue: '2026-11-02', fieldName: 'dueDate', fieldType: ApprovalFieldType.DATE_PICKER},
        {defaultValue: '2026-11-02', fieldName: 'startDay', fieldType: ApprovalFieldType.DATETIME_PICKER},
        {defaultValue: '2026-11-02T09:30:45Z', fieldName: 'startAt', fieldType: ApprovalFieldType.DATETIME_PICKER},
    ];

    it('shows a date-only default on the same day', () => {
        const values = getApprovalFormDefaultValues(inputs);

        expect(values.dueDate).toBe('2026-11-02');
        expect(values.startDay).toBe('2026-11-02T00:00');
    });

    it('sends untouched date defaults exactly as defined', () => {
        expect(toApprovalFormData(inputs, getApprovalFormDefaultValues(inputs))).toEqual({
            dueDate: '2026-11-02',
            startAt: '2026-11-02T09:30:45Z',
            startDay: '2026-11-02',
        });
    });

    it('converts a date the reviewer edited', () => {
        const values = {...getApprovalFormDefaultValues(inputs), dueDate: '2026-11-05'};

        expect(toApprovalFormData(inputs, values, new Set(['dueDate'])).dueDate).toBe(
            new Date(2026, 10, 5).toISOString()
        );
    });
});

describe('email validation', () => {
    const inputs = [{fieldName: 'approver', fieldType: ApprovalFieldType.EMAIL_INPUT}];

    it('rejects a value the server would reject', () => {
        expect(validateApprovalForm(inputs, {approver: 'not an email'})).toEqual({
            approver: 'Enter an email address',
        });
        expect(validateApprovalForm(inputs, {approver: 'ann@'})).toEqual({approver: 'Enter an email address'});
    });

    it('accepts an email address and an empty optional field', () => {
        expect(validateApprovalForm(inputs, {approver: 'ann@example.com'})).toEqual({});
        expect(validateApprovalForm(inputs, {approver: ''})).toEqual({});
    });
});
