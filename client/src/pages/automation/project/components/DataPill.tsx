import {ComponentDefinitionModel} from '@/middleware/helios/execution';
import {useWorkflowNodeDetailsPanelStore} from '@/pages/automation/project/stores/useWorkflowNodeDetailsPanelStore';
import {TYPE_ICONS} from '@/shared/typeIcons';
import {PropertyType} from '@/types/projectTypes';
import {MouseEvent} from 'react';
import {twMerge} from 'tailwind-merge';

const DataPill = ({
    arrayIndex,
    componentAlias,
    componentDefinition,
    parentProperty,
    property,
}: {
    arrayIndex?: number;
    componentDefinition: ComponentDefinitionModel;
    componentAlias: string;
    onClick?: (event: MouseEvent<HTMLDivElement>) => void;
    parentProperty?: PropertyType;
    property: PropertyType;
}) => {
    const subProperties = property.properties || property.items;

    const {focusedInput} = useWorkflowNodeDetailsPanelStore();

    const mentionInput = focusedInput?.getEditor().getModule('mention');

    const addDataPillToInput = (
        componentAlias: string,
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
                componentIcon: componentDefinition.icon,
                id: property.name,
                value: `${componentAlias}/${dataPillName}`,
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
                onClick={() =>
                    addDataPillToInput(
                        componentAlias,
                        property,
                        parentProperty,
                        arrayIndex
                    )
                }
                onDragStart={(event) =>
                    event.dataTransfer.setData('name', property.name!)
                }
            >
                <span className="mr-2" title={property.type}>
                    {TYPE_ICONS[property.type as keyof typeof TYPE_ICONS]}
                </span>

                {property.name ? property.name : `[${arrayIndex}]`}
            </div>

            {subProperties?.length && (
                <ul className="mt-2 flex flex-col space-y-2 border-l border-gray-200 pl-4">
                    {subProperties?.map((subProperty, index) => (
                        <DataPill
                            arrayIndex={index}
                            componentAlias={componentAlias}
                            componentDefinition={componentDefinition}
                            key={`${componentAlias}-${subProperty.name}-${index}`}
                            parentProperty={property}
                            property={subProperty}
                        />
                    ))}
                </ul>
            )}
        </li>
    );
};

export default DataPill;
