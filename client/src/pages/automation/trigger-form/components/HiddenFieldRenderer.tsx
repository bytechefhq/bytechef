import {FormField} from '@/components/ui/form';
import {TriggerFormInput} from '@/shared/middleware/platform/configuration';
import {UseFormReturn} from 'react-hook-form';

interface HiddenFieldRendererProps {
    form: UseFormReturn<Record<string, unknown>>;
    formInput: Partial<TriggerFormInput>;
    name: string;
}

export const HiddenFieldRenderer = ({form, formInput, name}: HiddenFieldRendererProps) => {
    const {defaultValue} = formInput;

    return (
        <FormField
            control={form.control}
            name={name}
            render={({field}) => (
                <input type="hidden" {...field} value={field.value?.toString() ?? defaultValue?.toString() ?? ''} />
            )}
        />
    );
};
