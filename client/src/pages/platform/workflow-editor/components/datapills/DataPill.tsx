import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import getNestedObject from '@/pages/platform/workflow-editor/utils/getNestedObject';
import {TYPE_ICONS} from '@/shared/typeIcons';
import {PropertyAllType} from '@/shared/types';
import {Editor} from '@tiptap/react';
import resolvePath from 'object-resolve-path';
import {MouseEvent} from 'react';
import {twMerge} from 'tailwind-merge';

import {encodePath, transformPathForObjectAccess} from '../../utils/encodingUtils';

interface HandleDataPillClickProps {
    workflowNodeName: string;
    propertyName?: string;
    parentPropertyName?: string;
    path?: string;
}

interface DataPillProps {
    componentIcon?: string;
    workflowNodeName: string;
    onClick?: (event: MouseEvent<HTMLDivElement>) => void;
    parentProperty?: PropertyAllType;
    property?: PropertyAllType;
    path?: string;
    root?: boolean;
    /* eslint-disable  @typescript-eslint/no-explicit-any */
    sampleOutput?: any;
}

const DataPillSampleValue = ({sampleOutput}: {sampleOutput: string | number | boolean | null}) => {
    const sampleOutputString = String(sampleOutput);

    if (sampleOutputString.length > 27) {
        return (
            <Tooltip>
                <TooltipTrigger asChild>
                    <span className="flex-1 truncate text-xs text-muted-foreground">{sampleOutputString}</span>
                </TooltipTrigger>

                <TooltipContent className="max-h-96 max-w-96 overflow-y-scroll whitespace-pre-wrap break-all">
                    {sampleOutputString}
                </TooltipContent>
            </Tooltip>
        );
    }

    return <span className="flex-1 text-xs text-muted-foreground">{sampleOutputString}</span>;
};

const DataPill = ({
    componentIcon,
    parentProperty,
    path,
    property,
    root = false,
    sampleOutput,
    workflowNodeName,
}: DataPillProps) => {
    const {currentComponent, focusedInput} = useWorkflowNodeDetailsPanelStore();

    const mentionInput: Editor | null = focusedInput;

    const subProperties = property?.properties || property?.items;

    if (!property?.name && property?.controlType === 'ARRAY_BUILDER') {
        property.name = '[index]';
    }

    const handleDataPillClick = ({
        parentPropertyName,
        path,
        propertyName,
        workflowNodeName,
    }: HandleDataPillClickProps) => {
        if (!mentionInput) {
            return;
        }

        const dataPillName = parentPropertyName
            ? `${parentPropertyName}.${propertyName}`
            : `${propertyName || workflowNodeName}`;

        let value = workflowNodeName;

        if (propertyName) {
            value = `${workflowNodeName}.${path || dataPillName}`;
        }

        if (value.includes('/')) {
            value = value.replaceAll('/', '.').replaceAll('.[index]', '[0]');
        }

        const parameters = currentComponent?.parameters || {};

        // Prevents adding a 2nd datapill to a non-string property
        if (Object.keys(parameters).length) {
            const attributes = mentionInput.view.props.attributes as {[name: string]: string};

            const encodedPath = encodePath(attributes.path);

            const path = transformPathForObjectAccess(encodedPath);

            const paramValue = resolvePath(parameters, path);

            if (attributes.type !== 'STRING' && paramValue && !paramValue.startsWith('=')) {
                return;
            }
        }

        mentionInput
            .chain()
            .focus()
            .insertContent({
                attrs: {
                    id: transformPathForObjectAccess(value),
                },
                type: 'mention',
            })
            .run();
    };

    const getSubPropertyPath = (subPropertyName = '[index]') =>
        path ? `${path}/${subPropertyName}` : `${property?.name || '[index]'}/${subPropertyName}`;

    if (root) {
        return (
            <div className="flex w-full items-center space-x-2">
                <div
                    className={twMerge(
                        'inline-flex cursor-pointer items-center space-x-2 rounded-full border bg-surface-neutral-secondary px-2 py-0.5 text-sm hover:bg-surface-main',
                        !mentionInput && 'cursor-not-allowed'
                    )}
                    draggable
                    onClick={() => handleDataPillClick({workflowNodeName})}
                >
                    <span className="mr-2" title={property?.type}>
                        {TYPE_ICONS[property?.type as keyof typeof TYPE_ICONS]}
                    </span>

                    <span>{workflowNodeName}</span>
                </div>

                {sampleOutput !== undefined && typeof sampleOutput !== 'object' && (
                    <DataPillSampleValue sampleOutput={sampleOutput} />
                )}
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
            <Tooltip>
                <TooltipTrigger asChild>
                    <div
                        className={twMerge(
                            'mr-auto flex cursor-pointer items-center rounded-full border bg-surface-neutral-secondary px-2 py-0.5 text-sm hover:bg-surface-main',
                            !mentionInput && 'cursor-not-allowed'
                        )}
                        data-name={property?.name || workflowNodeName}
                        draggable
                        onClick={() =>
                            handleDataPillClick({
                                parentPropertyName: parentProperty?.name,
                                path,
                                propertyName: property?.name || '[index]',
                                workflowNodeName,
                            })
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
                </TooltipTrigger>

                {property?.description && (
                    <TooltipContent className="mr-2 max-w-72 whitespace-normal break-normal">
                        <span className="block">{property.description}</span>
                    </TooltipContent>
                )}
            </Tooltip>

            {!!subProperties?.length && (
                <ul className="mt-2 flex flex-col space-y-2 border-l border-l-border/50 pl-4">
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

                        const showSampleValue =
                            sampleValue !== undefined &&
                            (sampleValue === null ||
                                typeof sampleValue !== 'object' ||
                                (typeof sampleValue === 'object' &&
                                    sampleValue !== null &&
                                    Object.keys(sampleValue).length === 0));

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

                                {showSampleValue && <DataPillSampleValue sampleOutput={sampleValue} />}
                            </div>
                        );
                    })}
                </ul>
            )}
        </li>
    );
};

export default DataPill;
