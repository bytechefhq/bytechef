import {useWorkflowNodeDetailsPanelStore} from '@/pages/automation/project/stores/useWorkflowNodeDetailsPanelStore';
import {TYPE_ICONS} from '@/shared/typeIcons';
import {PropertyType} from '@/types/projectTypes';
import {MouseEvent} from 'react';
import InlineSVG from 'react-inlinesvg';
import {twMerge} from 'tailwind-merge';

const DataPill = ({
    arrayIndex,
    componentAlias,
    componentIcon,
    parentProperty,
    path,
    property,
}: {
    arrayIndex?: number;
    componentIcon?: string;
    componentAlias: string;
    onClick?: (event: MouseEvent<HTMLDivElement>) => void;
    parentProperty?: PropertyType;
    property?: PropertyType;
    path?: string;
}) => {
    const {focusedInput} = useWorkflowNodeDetailsPanelStore();

    const mentionInput = focusedInput?.getEditor().getModule('mention');

    const subProperties = property?.properties || property?.items;

    const addDataPillToInput = (
        componentAlias: string,
        propertyName?: string,
        parentPropertyName?: string,
        path?: string,
        arrayIndex?: number
    ) => {
        const dataPillName = parentPropertyName
            ? `${parentPropertyName}/${propertyName || `[${arrayIndex}]`}`
            : `${propertyName || componentAlias}`;

        mentionInput.insertItem(
            {
                componentIcon: componentIcon,
                id: propertyName || componentAlias,
                value: propertyName ? `${componentAlias}/${path || dataPillName}` : componentAlias,
            },
            true,
            {blotName: 'property-mention'}
        );
    };

    const getSubPropertyPath = (subPropertyName = '[0]') =>
        path ? `${path}/${subPropertyName}` : `${property?.name || `[${arrayIndex}]`}/${subPropertyName}`;

    if (!property && componentIcon) {
        return (
            <div
                className="inline-flex cursor-pointer items-center space-x-2 rounded-full border bg-gray-100 px-2 py-0.5 text-sm hover:bg-gray-50"
                onClick={() => addDataPillToInput(componentAlias)}
            >
                <InlineSVG className="h-6 w-6" src={componentIcon} />

                <span>{componentAlias}</span>
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
                data-name={property?.name || componentAlias}
                draggable
                onClick={() =>
                    addDataPillToInput(componentAlias, property?.name, parentProperty?.name, path, arrayIndex)
                }
                onDragStart={(event) => event.dataTransfer.setData('name', property?.name || componentAlias)}
            >
                <span className="mr-2" title={property?.type}>
                    {TYPE_ICONS[property?.type as keyof typeof TYPE_ICONS]}
                </span>

                {property?.name ? property?.name : `[${arrayIndex}]` || componentAlias}
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
