import {FormControl, FormField, FormItem, FormMessage} from '@/components/ui/form';
import {Textarea} from '@/components/ui/textarea';
import {TriggerFormInput} from '@/shared/middleware/automation/workflow/execution';
import {UseFormReturn} from 'react-hook-form';

import {FormLabelWithDescription} from './FormLabelWithDescription';

interface TextAreaFieldRendererProps {
    form: UseFormReturn<Record<string, unknown>>;
    formInput: Partial<TriggerFormInput>;
    name: string;
}

export const TextAreaFieldRenderer = ({form, formInput, name}: TextAreaFieldRendererProps) => {
    const {fieldDescription, fieldLabel, fieldName, placeholder, required} = formInput;

    const label = fieldLabel || fieldName || name;

    return (
        <FormField
            control={form.control}
            name={name}
            render={({field}) => (
                <FormItem className="space-y-2">
                    <FormLabelWithDescription description={fieldDescription} label={label} />

                    <FormControl>
                        <Textarea
                            placeholder={placeholder}
                            {...field}
                            value={typeof field.value === 'string' ? field.value : ''}
                        />
                    </FormControl>

                    <FormMessage />
                </FormItem>
            )}
            rules={{required}}
        />
    );
};
