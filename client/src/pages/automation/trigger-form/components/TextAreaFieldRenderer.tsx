import {FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Textarea} from '@/components/ui/textarea';
import {TriggerFormInput} from '@/shared/middleware/platform/configuration';
import {UseFormReturn} from 'react-hook-form';

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
                    <FormLabel>{label}</FormLabel>

                    {fieldDescription && <FormDescription>{fieldDescription}</FormDescription>}

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
