import {FormControl, FormDescription, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Controller, UseFormReturn} from 'react-hook-form';

import {FormInputType} from '../util/triggerForm-utils';

interface FileInputFieldRendererProps {
    form: UseFormReturn<Record<string, unknown>>;
    formInput: Partial<FormInputType>;
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
                    <FormLabel>{label}</FormLabel>

                    {fieldDescription && <FormDescription>{fieldDescription}</FormDescription>}

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
