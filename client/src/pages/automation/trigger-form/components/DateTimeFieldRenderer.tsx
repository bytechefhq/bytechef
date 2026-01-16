import DatePicker from '@/components/DatePicker/DatePicker';
import DateTimePicker from '@/components/DateTimePicker/DateTimePicker';
import {FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {FieldType} from '@/pages/automation/trigger-form/TriggerForm';
import {TriggerFormInput} from '@/shared/middleware/platform/configuration';
import {UseFormReturn} from 'react-hook-form';

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
                    <FormLabel>{label}</FormLabel>

                    {fieldDescription && <FormDescription>{fieldDescription}</FormDescription>}

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
