import {ComponentDefinitionModel} from '@/middleware/helios/execution';
import {useNodeDetailsDialogStore} from '@/pages/automation/project/stores/useNodeDetailsDialogStore';
import {TYPE_ICONS} from '@/shared/typeIcons';
import {PropertyType} from '@/types/projectTypes';
import {MouseEvent} from 'react';
import {twMerge} from 'tailwind-merge';

const DataPill = ({
    arrayIndex,
    component,
    componentAlias,
    parentProperty,
    property,
}: {
    arrayIndex?: number;
    component: ComponentDefinitionModel;
    componentAlias: string;
    onClick?: (event: MouseEvent<HTMLDivElement>) => void;
    parentProperty?: PropertyType;
    property: PropertyType;
}) => {
    const subProperties = property.properties || property.items;

    const {focusedInput} = useNodeDetailsDialogStore();

    const mentionInput = focusedInput?.getEditor().getModule('mention');

    const addDataPillToInput = (
        property: PropertyType,
        parentProperty?: PropertyType,
        arrayIndex?: number
    ) => {
        const parentPropertyName = parentProperty?.name;

        const dataPillName = parentPropertyName
            ? `${parentPropertyName}/${
                  property.name ? property.name : `[${arrayIndex}]`
              }`
            : `${property.name}`;

        mentionInput.insertItem(
            {
                componentIcon: component.icon,
                id: property.name,
                value: dataPillName,
            },
            true,
            {blotName: 'property-mention'}
        );
    };

    return (
        <li
            className={twMerge(
                'mr-auto',
                subProperties &&
                    'flex flex-col space-y-2 border-0 bg-transparent p-0 hover:cursor-default hover:bg-transparent'
            )}
        >
            <div
                className="mr-auto flex cursor-pointer items-center rounded-full border bg-gray-100 px-2 py-0.5 text-sm hover:bg-gray-50"
                data-name={property.name}
                draggable
                onDragStart={(event) =>
                    event.dataTransfer.setData('name', property.name!)
                }
                onClick={() =>
                    addDataPillToInput(property, parentProperty, arrayIndex)
                }
            >
                <span className="mr-2" title={property.type}>
                    {TYPE_ICONS[property.type as keyof typeof TYPE_ICONS]}
                </span>

                {property.name ? property.name : `[${arrayIndex}]`}
            </div>

            {subProperties?.length ? (
                <ul className="mt-2 flex flex-col space-y-2 border-l border-gray-200 pl-4">
                    {subProperties?.map((subProperty, index) => (
                        <DataPill
                            arrayIndex={index}
                            component={component}
                            componentAlias={componentAlias}
                            key={`${componentAlias}-${subProperty.name}-${index}`}
                            property={subProperty}
                            parentProperty={property}
                        />
                    ))}
                </ul>
            ) : (
                ''
            )}
        </li>
    );
};

export default DataPill;
