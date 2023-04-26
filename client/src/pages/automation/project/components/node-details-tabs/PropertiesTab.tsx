import Checkbox from 'components/Checkbox/Checkbox';
import Input from 'components/Input/Input';
import Select from 'components/Select/Select';

import {PropertyType} from '../../types/projectTypes';

const Property = ({property}: {property: PropertyType}) => {
    const {
        controlType,
        defaultValue,
        description,
        label,
        name,
        options,
        required,
    } = property;

    return (
        <li>
            {controlType === 'INPUT_TEXT' && (
                <Input
                    description={description}
                    label={label}
                    name={name!}
                    required={required}
                />
            )}

            {controlType === 'INPUT_INTEGER' && (
                <Input
                    description={description}
                    label={label}
                    name={name!}
                    required={required}
                    type="number"
                />
            )}

            {controlType === 'SELECT' && !!options?.length && (
                <Select
                    description={description}
                    label={label}
                    options={
                        options.map((option) => ({
                            ...option,
                            label: option.name as string,
                            value: option.value,
                            // OpenAPI type generator forces object as the type
                            // eslint-disable-next-line @typescript-eslint/no-explicit-any
                        }))! as any
                    }
                    triggerClassName="w-full border border-gray-300"
                />
            )}

            {controlType === 'CHECKBOX' && (
                <Checkbox
                    // OpenAPI type generator forces object as the type
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                    defaultChecked={defaultValue as any}
                    description={description}
                    label={label}
                    id={name!}
                />
            )}

            {controlType === 'JSON_BUILDER' && (
                <div className="rounded-md bg-gray-100 p-4">
                    <ul>
                        {property.properties?.map(
                            (subProperty: PropertyType) => (
                                <li key={subProperty.name}>
                                    <Property property={subProperty} />
                                </li>
                            )
                        )}
                    </ul>
                </div>
            )}
        </li>
    );
};

const PropertiesTab = ({properties}: {properties: Array<PropertyType>}) => (
    <ul className="mb-4 space-y-2">
        {properties.map((property) => (
            <Property key={property.name} property={property} />
        ))}
    </ul>
);

export default PropertiesTab;
