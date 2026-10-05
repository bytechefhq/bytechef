import {FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Textarea} from '@/components/ui/textarea';
import {useFormContext} from 'react-hook-form';

interface DescriptionFormFieldProps {
    placeholder?: string;
}

const DescriptionFormField = ({placeholder}: DescriptionFormFieldProps) => {
    const {control} = useFormContext();

    return (
        <FormField
            control={control}
            name="description"
            render={({field}) => (
                <FormItem>
                    <FormLabel>Description</FormLabel>

                    <FormControl>
                        <Textarea placeholder={placeholder} rows={5} {...field} />
                    </FormControl>

                    <FormMessage />
                </FormItem>
            )}
        />
    );
};

export default DescriptionFormField;
