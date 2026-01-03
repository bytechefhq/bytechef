import {FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {UseFormReturn} from 'react-hook-form';

import {FieldType, FormInputType} from '../util/triggerForm-utils';

interface InputFieldRendererProps {
    form: UseFormReturn<Record<string, unknown>>;
    formInput: Partial<FormInputType>;
    name: string;
}

export const InputFieldRenderer = ({form, formInput, name}: InputFieldRendererProps) => {
    const {fieldDescription, fieldLabel, fieldName, fieldType, placeholder, required} = formInput;

    const label = fieldLabel || fieldName || name;

    const type =
        fieldType === FieldType.EMAIL_INPUT
            ? 'email'
            : fieldType === FieldType.NUMBER_INPUT
              ? 'number'
              : fieldType === FieldType.PASSWORD_INPUT
                ? 'password'
                : 'text';

    return (
        <FormField
            control={form.control}
            name={name}
            render={({field}) => (
                <FormItem className="space-y-2">
                    <FormLabel>{label}</FormLabel>

                    {fieldDescription && <FormDescription>{fieldDescription}</FormDescription>}

                    <FormControl>
                        <Input
                            placeholder={placeholder}
                            type={type}
                            {...field}
                            value={
                                typeof field.value === 'string' || typeof field.value === 'number' ? field.value : ''
                            }
                        />
                    </FormControl>

                    <FormMessage />
                </FormItem>
            )}
            rules={{required}}
        />
    );
};
