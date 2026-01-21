import {FieldType} from '@/pages/automation/trigger-form/TriggerForm';
import {render, screen} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

import {DateTimeFieldRenderer} from '../DateTimeFieldRenderer';
import {createMockForm} from './testUtils';

vi.mock('@/components/DatePicker/DatePicker', () => ({
    default: ({onChange, value}: {onChange: (date: Date | undefined) => void; value?: Date}) => (
        <input
            data-testid="date-picker"
            onChange={(event) => onChange(event.target.value ? new Date(event.target.value) : undefined)}
            type="date"
            value={value ? value.toISOString().split('T')[0] : ''}
        />
    ),
}));

vi.mock('@/components/DateTimePicker/DateTimePicker', () => ({
    default: ({onChange, value}: {onChange: (date: Date | undefined) => void; value?: Date}) => (
        <input
            data-testid="datetime-picker"
            onChange={(event) => onChange(event.target.value ? new Date(event.target.value) : undefined)}
            type="datetime-local"
            value={value ? value.toISOString().slice(0, 16) : ''}
        />
    ),
}));

describe('DateTimeFieldRenderer', () => {
    it('should render with fieldLabel', () => {
        const {form, wrapper} = createMockForm();

        render(
            <DateTimeFieldRenderer
                form={form}
                formInput={{fieldLabel: 'Select Date', fieldType: FieldType.DATE_PICKER}}
                name="dateField"
            />,
            {wrapper}
        );

        expect(screen.getByText('Select Date')).toBeInTheDocument();
    });

    it('should render with fieldName when fieldLabel is not provided', () => {
        const {form, wrapper} = createMockForm();

        render(
            <DateTimeFieldRenderer
                form={form}
                formInput={{fieldName: 'eventDate', fieldType: FieldType.DATE_PICKER}}
                name="dateField"
            />,
            {wrapper}
        );

        expect(screen.getByText('eventDate')).toBeInTheDocument();
    });

    it('should render with name when fieldLabel and fieldName are not provided', () => {
        const {form, wrapper} = createMockForm();

        render(
            <DateTimeFieldRenderer form={form} formInput={{fieldType: FieldType.DATE_PICKER}} name="fallbackDate" />,
            {wrapper}
        );

        expect(screen.getByText('fallbackDate')).toBeInTheDocument();
    });

    it('should render field description when provided', () => {
        const {form, wrapper} = createMockForm();

        render(
            <DateTimeFieldRenderer
                form={form}
                formInput={{
                    fieldDescription: 'Select the event date',
                    fieldLabel: 'Event Date',
                    fieldType: FieldType.DATE_PICKER,
                }}
                name="eventDate"
            />,
            {wrapper}
        );

        expect(screen.getByText('Select the event date')).toBeInTheDocument();
    });

    it('should not render field description when not provided', () => {
        const {form, wrapper} = createMockForm();

        render(
            <DateTimeFieldRenderer
                form={form}
                formInput={{fieldLabel: 'Event Date', fieldType: FieldType.DATE_PICKER}}
                name="eventDate"
            />,
            {wrapper}
        );

        expect(screen.queryByText('Select the event date')).not.toBeInTheDocument();
    });

    it('should render DatePicker for DATE_PICKER fieldType', () => {
        const {form, wrapper} = createMockForm();

        render(
            <DateTimeFieldRenderer
                form={form}
                formInput={{fieldLabel: 'Date Only', fieldType: FieldType.DATE_PICKER}}
                name="dateOnly"
            />,
            {wrapper}
        );

        expect(screen.getByTestId('date-picker')).toBeInTheDocument();
        expect(screen.queryByTestId('datetime-picker')).not.toBeInTheDocument();
    });

    it('should render DateTimePicker for DATETIME_PICKER fieldType', () => {
        const {form, wrapper} = createMockForm();

        render(
            <DateTimeFieldRenderer
                form={form}
                formInput={{fieldLabel: 'Date and Time', fieldType: FieldType.DATETIME_PICKER}}
                name="dateTime"
            />,
            {wrapper}
        );

        expect(screen.getByTestId('datetime-picker')).toBeInTheDocument();
        expect(screen.queryByTestId('date-picker')).not.toBeInTheDocument();
    });

    it('should render DateTimePicker for non-DATE_PICKER fieldTypes', () => {
        const {form, wrapper} = createMockForm();

        render(
            <DateTimeFieldRenderer form={form} formInput={{fieldLabel: 'Default DateTime'}} name="defaultDateTime" />,
            {wrapper}
        );

        expect(screen.getByTestId('datetime-picker')).toBeInTheDocument();
    });

    it('should display existing date value for DatePicker', () => {
        const testDate = '2024-06-15T10:00:00.000Z';
        const {form, wrapper} = createMockForm({dateField: testDate});

        render(
            <DateTimeFieldRenderer
                form={form}
                formInput={{fieldLabel: 'Date', fieldType: FieldType.DATE_PICKER}}
                name="dateField"
            />,
            {wrapper}
        );

        const datePicker = screen.getByTestId('date-picker');
        expect(datePicker).toHaveValue('2024-06-15');
    });

    it('should display existing datetime value for DateTimePicker', () => {
        const testDateTime = '2024-06-15T14:30:00.000Z';
        const {form, wrapper} = createMockForm({dateTimeField: testDateTime});

        render(
            <DateTimeFieldRenderer
                form={form}
                formInput={{fieldLabel: 'DateTime', fieldType: FieldType.DATETIME_PICKER}}
                name="dateTimeField"
            />,
            {wrapper}
        );

        const dateTimePicker = screen.getByTestId('datetime-picker');
        expect(dateTimePicker).toHaveValue('2024-06-15T14:30');
    });
});
