import {FieldValues, Path} from 'react-hook-form/dist/types';
import {FormState, UseFormRegister} from 'react-hook-form/dist/types/form';

import {
    ArrayPropertyModel,
    BooleanPropertyModel,
    DatePropertyModel,
    DateTimePropertyModel,
    DynamicPropertiesPropertyModel,
    IntegerPropertyModel,
    NullPropertyModel,
    NumberPropertyModel,
    ObjectPropertyModel,
    OneOfPropertyModel,
    StringPropertyModel,
    TimePropertyModel,
} from '../../middleware/definition-registry';
import Input from '../Input/Input';

type Property = ArrayPropertyModel &
    BooleanPropertyModel &
    DatePropertyModel &
    DateTimePropertyModel &
    DynamicPropertiesPropertyModel &
    IntegerPropertyModel &
    NumberPropertyModel &
    NullPropertyModel &
    ObjectPropertyModel &
    OneOfPropertyModel &
    StringPropertyModel &
    TimePropertyModel;

interface PropertiesProps<
    TProperty extends Property,
    TFieldValues extends FieldValues = FieldValues
> {
    formState: FormState<TFieldValues>;
    path?: string;
    properties?: TProperty[];
    register: UseFormRegister<TFieldValues>;
}

const Properties = <
    TProperty extends Property,
    TFieldValues extends FieldValues = FieldValues
>({
    formState: {errors, touchedFields},
    path = 'parameters',
    properties,
    register,
}: PropertiesProps<TProperty, TFieldValues>): JSX.Element => {
    function isError(propertyName: string) {
        if (
            touchedFields[path] &&
            touchedFields[path]![propertyName] &&
            errors[path] &&
            (errors[path] as never)[propertyName]
        ) {
            return true;
        }

        return false;
    }

    function getFieldPath(propertyName: string) {
        return (path + '.' + propertyName) as Path<TFieldValues>;
    }

    return (
        <>
            {properties &&
                properties.map((property) => {
                    if (property.type === 'STRING') {
                        return (
                            <Input
                                description={property?.description}
                                defaultValue={
                                    property?.defaultValue
                                        ? property.defaultValue + ''
                                        : ''
                                }
                                error={isError(property.name!)}
                                type={property.hidden ? 'hidden' : 'text'}
                                key={property.name}
                                label={property.label}
                                {...register(getFieldPath(property.name!), {
                                    required: property.required!,
                                })}
                            />
                        );
                    }
                })}
        </>
    );
};

Properties.displayName = 'Properties';

export default Properties;
