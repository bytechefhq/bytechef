import CreatableSelect from '@/components/CreatableSelect/CreatableSelect';
import {FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {useFormContext} from 'react-hook-form';

interface CategoryFormFieldProps {
    categories?: {name: string}[];
    categoriesLoading: boolean;
}

const CategoryFormField = ({categories, categoriesLoading}: CategoryFormFieldProps) => {
    const {control, setValue} = useFormContext();

    if (categoriesLoading) {
        return null;
    }

    return (
        <FormField
            control={control}
            name="category"
            render={({field}) => (
                <FormItem>
                    <FormLabel>Category</FormLabel>

                    <FormControl>
                        <CreatableSelect
                            field={field}
                            isMulti={false}
                            onCreateOption={(inputValue: string) => {
                                setValue('category', {
                                    label: inputValue,
                                    name: inputValue,
                                    value: inputValue,
                                });
                            }}
                            options={(categories || []).map((category) => ({
                                label: category.name,
                                value: category.name.toLowerCase().replace(/\W/g, ''),
                                ...category,
                            }))}
                            placeholder="Marketing, Sales, Social Media..."
                        />
                    </FormControl>

                    <FormMessage />
                </FormItem>
            )}
        />
    );
};

export default CategoryFormField;
