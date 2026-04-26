import DatePicker from '@/components/DatePicker/DatePicker';
import DateTimePicker from '@/components/DateTimePicker/DateTimePicker';
import {FormControl, FormField, FormItem, FormMessage} from '@/components/ui/form';
import {FieldType} from '@/shared/constants';
import {TriggerFormInput} from '@/shared/middleware/automation/workflow/execution';
import {UseFormReturn} from 'react-hook-form';

import {FormLabelWithDescription} from './FormLabelWithDescription';

interface DateTimeFieldRendererProps {
    form: UseFormReturn<Record<string, unknown>>;
    formInput: Partial<TriggerFormInput>;
    name: string;
}

export const DateTimeFieldRenderer = ({form, formInput, name}: DateTimeFieldRendererProps) => {
    const {fieldDescription, fieldLabel, fieldName, fieldType, required} = formInput;

    const label = fieldLabel || fieldName || name;

    return (
        <FormField
            control={form.control}
            name={name}
            render={({field}) => (
                <FormItem className="space-y-2">
                    <FormLabelWithDescription description={fieldDescription} label={label} />

                    <FormControl>
                        {fieldType === FieldType.DATE_PICKER ? (
                            <DatePicker
                                onChange={(date) => field.onChange(date?.toISOString())}
                                value={field.value ? new Date(field.value as string) : undefined}
                            />
                        ) : (
                            <DateTimePicker
                                onChange={(date) => field.onChange(date?.toISOString())}
                                value={field.value ? new Date(field.value as string) : undefined}
                            />
                        )}
                    </FormControl>

                    <FormMessage />
                </FormItem>
            )}
            rules={{required}}
        />
    );
};
