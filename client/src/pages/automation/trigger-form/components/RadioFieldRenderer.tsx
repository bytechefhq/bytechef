import {FormDescription, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Label} from '@/components/ui/label';
import {RadioGroup, RadioGroupItem} from '@/components/ui/radio-group';
import {UseFormReturn} from 'react-hook-form';

import {FormInputType} from '../util/triggerForm-utils';

interface RadioFieldRendererProps {
    form: UseFormReturn<Record<string, unknown>>;
    formInput: Partial<FormInputType>;
    name: string;
}

export const RadioFieldRenderer = ({form, formInput, name}: RadioFieldRendererProps) => {
    const {fieldDescription, fieldLabel, fieldName, fieldOptions, required} = formInput;

    const options = fieldOptions || [];
    const label = fieldLabel || fieldName || name;

    return (
        <FormField
            control={form.control}
            name={name}
            render={({field}) => (
                <FormItem className="space-y-2">
                    <FormLabel>{label}</FormLabel>

                    {fieldDescription && <FormDescription>{fieldDescription}</FormDescription>}

                    <div className="flex flex-col gap-2">
                        <RadioGroup
                            onValueChange={field.onChange}
                            value={typeof field.value === 'string' ? field.value : undefined}
                        >
                            <div className="flex items-center space-x-2">
                                {options.map((opt) => (
                                    <div className="flex items-center space-x-2" key={opt.value}>
                                        <RadioGroupItem id={`${name}-${opt.value}`} value={opt.value} />

                                        <Label htmlFor={`${name}-${opt.value}`}>{opt.label}</Label>
                                    </div>
                                ))}
                            </div>
                        </RadioGroup>
                    </div>

                    <FormMessage />
                </FormItem>
            )}
            rules={{required}}
        />
    );
};
