import {useWorkflowNodeDetailsPanelStore} from '@/pages/automation/project/stores/useWorkflowNodeDetailsPanelStore';
import {TYPE_ICONS} from '@/shared/typeIcons';
import {PropertyType} from '@/types/projectTypes';
import {MouseEvent} from 'react';
import {twMerge} from 'tailwind-merge';

const DataPill = ({
    arrayIndex,
    componentIcon,
    parentProperty,
    path,
    property,
    root = false,
    workflowNodeName,
}: {
    arrayIndex?: number;
    componentIcon?: string;
    workflowNodeName: string;
    onClick?: (event: MouseEvent<HTMLDivElement>) => void;
    parentProperty?: PropertyType;
    property?: PropertyType;
    path?: string;
    root?: boolean;
}) => {
    const {focusedInput} = useWorkflowNodeDetailsPanelStore();

    const mentionInput = focusedInput?.getEditor().getModule('mention');

    const subProperties = property?.properties || property?.items;

    if (!property?.name && (property?.type === 'OBJECT' || property?.type === 'ARRAY')) {
        property.name = `[${arrayIndex || 0}]`;
    }

    const addDataPillToInput = (
        workflowNodeName: string,
        propertyName?: string,
        parentPropertyName?: string,
        path?: string,
        arrayIndex?: number
    ) => {
        const dataPillName = parentPropertyName
            ? `${parentPropertyName}/${propertyName || `[${arrayIndex || 0}]`}`
            : `${propertyName || workflowNodeName}`;

        mentionInput.insertItem(
            {
                componentIcon: componentIcon,
                id: propertyName || workflowNodeName,
                value: propertyName ? `${workflowNodeName}/${path || dataPillName}` : workflowNodeName,
            },
            true,
            {blotName: 'property-mention'}
        );
    };

    const getSubPropertyPath = (subPropertyName = '[0]') =>
        path ? `${path}/${subPropertyName}` : `${property?.name || `[${arrayIndex}]`}/${subPropertyName}`;

    if (root) {
        return (
            <div
                className="inline-flex cursor-pointer items-center space-x-2 rounded-full border bg-gray-100 px-2 py-0.5 text-sm hover:bg-gray-50"
                onClick={() => addDataPillToInput(workflowNodeName)}
            >
                <span className="mr-2" title={property?.type}>
                    {TYPE_ICONS[property?.type as keyof typeof TYPE_ICONS]}
                </span>

                <span>{workflowNodeName}</span>
            </div>
        );
    }

    return (
        <li
            className={twMerge(
                'mr-auto',
                subProperties?.length &&
                    'flex flex-col space-y-2 border-0 bg-transparent p-0 hover:cursor-default hover:bg-transparent'
            )}
        >
            <div
                className="mr-auto flex cursor-pointer items-center rounded-full border bg-gray-100 px-2 py-0.5 text-sm hover:bg-gray-50"
                data-name={property?.name || workflowNodeName}
                draggable
                onClick={() =>
                    addDataPillToInput(workflowNodeName, property?.name, parentProperty?.name, path, arrayIndex)
                }
                onDragStart={(event) => event.dataTransfer.setData('name', property?.name || workflowNodeName)}
            >
                <span className="mr-2" title={property?.type}>
                    {TYPE_ICONS[property?.type as keyof typeof TYPE_ICONS]}
                </span>

                {property?.name ? property?.name : `[${arrayIndex || 0}]` || workflowNodeName}
            </div>

            {!!subProperties?.length && (
                <ul className="mt-2 flex flex-col space-y-2 border-l border-gray-200 pl-4">
                    {subProperties?.map((subProperty, index) => (
                        <DataPill
                            arrayIndex={index}
                            componentAlias={componentAlias}
                            componentIcon={componentIcon}
                            key={`${componentAlias}-${subProperty.name}-${index}`}
                            parentProperty={property}
                            path={getSubPropertyPath(subProperty.name)}
                            property={subProperty}
                        />
                    ))}
                </ul>
            )}
        </li>
    );
};

export default DataPill;
