import {FormField} from '@/components/ui/form';
import {UseFormReturn} from 'react-hook-form';

import {FormInputType} from '../util/triggerForm-utils';

interface HiddenFieldRendererProps {
    form: UseFormReturn<Record<string, unknown>>;
    formInput: Partial<FormInputType>;
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
