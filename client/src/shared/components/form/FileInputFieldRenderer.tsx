import {FormControl, FormItem, FormMessage} from '@/components/ui/form';
import {TriggerFormInput} from '@/shared/middleware/automation/workflow/execution';
import {Controller, UseFormReturn} from 'react-hook-form';

import {FormLabelWithDescription} from './FormLabelWithDescription';

interface FileInputFieldRendererProps {
    form: UseFormReturn<Record<string, unknown>>;
    formInput: Partial<TriggerFormInput>;
    name: string;
}

export const FileInputFieldRenderer = ({form, formInput, name}: FileInputFieldRendererProps) => {
    const {fieldDescription, fieldLabel, fieldName, required} = formInput;

    const label = fieldLabel || fieldName || name;

    return (
        <Controller
            control={form.control}
            name={name}
            render={({field}) => (
                <FormItem className="space-y-2">
                    <FormLabelWithDescription description={fieldDescription} label={label} />

                    <FormControl>
                        <div>
                            <input
                                id={name}
                                onChange={(event) => field.onChange(event.target.files?.[0] || null)}
                                type="file"
                            />
                        </div>
                    </FormControl>

                    <FormMessage />
                </FormItem>
            )}
            rules={{required}}
        />
    );
};
