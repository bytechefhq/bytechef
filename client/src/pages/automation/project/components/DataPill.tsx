import {useWorkflowNodeDetailsPanelStore} from '@/pages/automation/project/stores/useWorkflowNodeDetailsPanelStore';
import getNestedObject from '@/pages/automation/project/utils/getNestedObject';
import {TYPE_ICONS} from '@/shared/typeIcons';
import {PropertyType} from '@/types/types';
import {MouseEvent} from 'react';
import {twMerge} from 'tailwind-merge';

const DataPill = ({
    componentIcon,
    parentProperty,
    path,
    property,
    root = false,
    sampleOutput,
    workflowNodeName,
}: {
    componentIcon?: string;
    workflowNodeName: string;
    onClick?: (event: MouseEvent<HTMLDivElement>) => void;
    parentProperty?: PropertyType;
    property?: PropertyType;
    path?: string;
    root?: boolean;
    sampleOutput: object;
}) => {
    const {focusedInput} = useWorkflowNodeDetailsPanelStore();

    const mentionInput = focusedInput?.getEditor().getModule('mention');

    const subProperties = property?.properties || property?.items;

    if (!property?.name && (property?.type === 'OBJECT' || property?.type === 'ARRAY')) {
        property.name = '[index]';
    }

    const addDataPillToInput = (
        workflowNodeName: string,
        propertyName?: string,
        parentPropertyName?: string,
        path?: string
    ) => {
        const dataPillName = parentPropertyName
            ? `${parentPropertyName}/${propertyName || '[index]'}`
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

    const getSubPropertyPath = (subPropertyName = '[index]') =>
        path ? `${path}/${subPropertyName}` : `${property?.name || '[index]'}/${subPropertyName}`;

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
                onClick={() => addDataPillToInput(workflowNodeName, property?.name, parentProperty?.name, path)}
                onDragStart={(event) => event.dataTransfer.setData('name', property?.name || workflowNodeName)}
            >
                <span className="mr-2" title={property?.type}>
                    {TYPE_ICONS[property?.type as keyof typeof TYPE_ICONS]}
                </span>

                {property?.name ? property?.name : '[index]' || workflowNodeName}
            </div>

            {!!subProperties?.length && (
                <ul className="mt-2 flex flex-col space-y-2 border-l border-gray-200 pl-4">
                    {subProperties?.map((subProperty, index) => {
                        const value = getNestedObject(
                            sampleOutput,
                            `${getSubPropertyPath(subProperty.name).replaceAll('/', '.')}`
                        );

                        return (
                            <div
                                className="flex items-center space-x-3"
                                key={`${workflowNodeName}-${subProperty.name}-${index}`}
                            >
                                <DataPill
                                    componentIcon={componentIcon}
                                    parentProperty={property}
                                    path={getSubPropertyPath(subProperty.name)}
                                    property={subProperty}
                                    sampleOutput={sampleOutput}
                                    workflowNodeName={workflowNodeName}
                                />

                                {(value || value === 0 || value === false) && typeof value !== 'object' && (
                                    <div className="flex-1 text-xs text-muted-foreground">
                                        {value === true ? 'true' : value === false ? false : value}
                                    </div>
                                )}
                            </div>
                        );
                    })}
                </ul>
            )}
        </li>
    );
};

export default DataPill;
