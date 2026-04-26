import {Checkbox} from '@/components/ui/checkbox';
import {FormControl, FormField, FormItem, FormMessage} from '@/components/ui/form';
import {TriggerFormInput} from '@/shared/middleware/automation/workflow/execution';
import {UseFormReturn} from 'react-hook-form';

import {FormLabelWithDescription} from './FormLabelWithDescription';

interface CheckboxFieldRendererProps {
    form: UseFormReturn<Record<string, unknown>>;
    formInput: Partial<TriggerFormInput>;
    name: string;
}

export const CheckboxFieldRenderer = ({form, formInput, name}: CheckboxFieldRendererProps) => {
    const {fieldDescription, fieldLabel, fieldName, placeholder, required} = formInput;

    const label = fieldLabel || fieldName || name;

    return (
        <FormField
            control={form.control}
            name={name}
            render={({field}) => (
                <FormItem className="space-y-2">
                    <div className="flex items-center space-x-2">
                        <FormControl>
                            <Checkbox checked={!!field.value} onCheckedChange={field.onChange} />
                        </FormControl>

                        <FormLabelWithDescription description={fieldDescription} label={placeholder || label} />
                    </div>

                    <FormMessage />
                </FormItem>
            )}
            rules={{required}}
        />
    );
};
