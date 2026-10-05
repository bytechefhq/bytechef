import {Input} from '@/components/Input/Input';
import {FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {useFormContext} from 'react-hook-form';

interface NameFormFieldProps {
    placeholder?: string;
}

const NameFormField = ({placeholder}: NameFormFieldProps) => {
    const {control} = useFormContext();

    return (
        <FormField
            control={control}
            name="name"
            render={({field}) => (
                <FormItem>
                    <FormLabel>Name</FormLabel>

                    <FormControl>
                        <Input placeholder={placeholder} {...field} />
                    </FormControl>

                    <FormMessage />
                </FormItem>
            )}
            rules={{required: true}}
        />
    );
};

export default NameFormField;
