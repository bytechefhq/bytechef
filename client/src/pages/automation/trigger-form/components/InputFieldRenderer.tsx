import {FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {FieldType} from '@/pages/automation/trigger-form/TriggerForm';
import {TriggerFormInput} from '@/shared/middleware/platform/configuration';
import {UseFormReturn} from 'react-hook-form';

const getInputTypeFromFieldType = (fieldType?: number): string => {
    if (fieldType === undefined) {
        return 'text';
    }

    const fieldTypeMap: Record<number, string> = {
        [FieldType.EMAIL_INPUT]: 'email',
        [FieldType.NUMBER_INPUT]: 'number',
        [FieldType.PASSWORD_INPUT]: 'password',
    };

    return fieldTypeMap[fieldType] ?? 'text';
};

interface InputFieldRendererProps {
    form: UseFormReturn<Record<string, unknown>>;
    formInput: Partial<TriggerFormInput>;
    name: string;
}

export const InputFieldRenderer = ({form, formInput, name}: InputFieldRendererProps) => {
    const {fieldDescription, fieldLabel, fieldName, fieldType, placeholder, required} = formInput;

    const label = fieldLabel || fieldName || name;
    const type = getInputTypeFromFieldType(fieldType);

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
