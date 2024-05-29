import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import getNestedObject from '@/pages/platform/workflow-editor/utils/getNestedObject';
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
    /* eslint-disable  @typescript-eslint/no-explicit-any */
    sampleOutput?: any;
}) => {
    const {focusedInput} = useWorkflowNodeDetailsPanelStore();

    const mentionInput = focusedInput?.getEditor().getModule('mention');

    const subProperties = property?.properties || property?.items;

    if (!property?.name && property?.controlType === 'ARRAY_BUILDER') {
        property.name = '[index]';
    }

    const addDataPillToInput = (
        workflowNodeName: string,
        propertyName?: string,
        parentPropertyName?: string,
        path?: string
    ) => {
        const dataPillName = parentPropertyName
            ? `${parentPropertyName}.${propertyName}`
            : `${propertyName || workflowNodeName}`;

        const value = propertyName
            ? `${workflowNodeName}.${(path || dataPillName).replaceAll('/', '.')}`
            : workflowNodeName;

        mentionInput.insertItem(
            {
                componentIcon,
                id: propertyName || workflowNodeName,
                value,
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
                onClick={() =>
                    addDataPillToInput(workflowNodeName, property?.name || '[index]', parentProperty?.name, path)
                }
                onDragStart={(event) => event.dataTransfer.setData('name', property?.name || workflowNodeName)}
            >
                {property?.name && (
                    <span className="mr-2" title={property?.type}>
                        {TYPE_ICONS[property?.type as keyof typeof TYPE_ICONS]}
                    </span>
                )}

                {!property?.name && (
                    <span className="mr-2" title={property?.type}>
                        {TYPE_ICONS.INTEGER}
                    </span>
                )}

                {property?.name || '[index]'}
            </div>

            {!!subProperties?.length && (
                <ul className="mt-2 flex flex-col space-y-2 border-l border-gray-200 pl-4">
                    {subProperties?.map((subProperty, index) => {
                        let sampleValue;

                        if (typeof sampleOutput === 'object') {
                            sampleValue = getNestedObject(
                                sampleOutput,
                                `${getSubPropertyPath(subProperty.name).replaceAll('/', '.')}`
                            );
                        } else {
                            sampleValue = sampleOutput;
                        }

                        if (typeof sampleValue === 'string') {
                            sampleValue =
                                (sampleValue as string).substring(0, 27) +
                                ((sampleValue as string).length > 27 ? '...' : '');
                        }

                        return (
                            <div
                                className="flex items-center space-x-2"
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

                                {(sampleValue || sampleValue === 0 || sampleValue === false) &&
                                    typeof sampleValue !== 'object' && (
                                        <div className="flex-1 text-xs text-muted-foreground">
                                            {sampleValue === true
                                                ? 'true'
                                                : sampleValue === false
                                                  ? false
                                                  : sampleValue}
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
