import {Checkbox} from '@/components/ui/checkbox';
import {FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {TriggerFormInput} from '@/shared/middleware/platform/configuration';
import {UseFormReturn} from 'react-hook-form';

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
                    {fieldDescription && <FormDescription>{fieldDescription}</FormDescription>}

                    <div className="flex items-center space-x-2">
                        <FormControl>
                            <Checkbox checked={!!field.value} onCheckedChange={field.onChange} />
                        </FormControl>

                        <FormLabel className="font-normal">{placeholder || label}</FormLabel>
                    </div>

                    <FormMessage />
                </FormItem>
            )}
            rules={{required}}
        />
    );
};
