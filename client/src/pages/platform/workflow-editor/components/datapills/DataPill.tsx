import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {encodePath, transformPathForObjectAccess} from '@/pages/platform/workflow-editor/utils/encodingUtils';
import getNestedObject from '@/pages/platform/workflow-editor/utils/getNestedObject';
import {TYPE_ICONS} from '@/shared/typeIcons';
import {ComponentType, DataPillDragPayloadType, PropertyAllType} from '@/shared/types';
import {Editor} from '@tiptap/react';
import resolvePath from 'object-resolve-path';
import {DragEvent, MouseEvent} from 'react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

import useDataPillPanelStore from '../../stores/useDataPillPanelStore';

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

export const canInsertMentionForProperty = (
    propertyType: string,
    parameters: Record<string, unknown>,
    path: string
): boolean => {
    if (propertyType === 'STRING') {
        return true;
    }

    try {
        const resolvedPath = transformPathForObjectAccess(encodePath(path));
        const existingValue = resolvePath(parameters, resolvedPath);

        return !existingValue || String(existingValue).startsWith('=');
    } catch {
        return true;
    }
};

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

const buildMentionId = ({
    parentPropertyName,
    path,
    propertyName,
    workflowNodeName,
}: HandleDataPillClickProps): string => {
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

    return transformPathForObjectAccess(value);
};

const canInsertDataPill = (mentionInput: Editor | null, currentComponent?: ComponentType): boolean => {
    if (!mentionInput) {
        return false;
    }

    const parameters = currentComponent?.parameters || {};

    if (!Object.keys(parameters).length) {
        return true;
    }

    const attributes = mentionInput.view.props.attributes as {[name: string]: string};

    return canInsertMentionForProperty(attributes.type, parameters, attributes.path);
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
    const {currentComponent, focusedInput} = useWorkflowNodeDetailsPanelStore(
        useShallow((state) => ({
            currentComponent: state.currentComponent,
            focusedInput: state.focusedInput,
        }))
    );

    const setIsDraggingDataPill = useDataPillPanelStore((state) => state.setIsDraggingDataPill);

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

        if (!canInsertDataPill(mentionInput, currentComponent)) {
            return;
        }

        const mentionId = buildMentionId({
            parentPropertyName,
            path,
            propertyName,
            workflowNodeName,
        });

        mentionInput
            .chain()
            .focus()
            .insertContent({
                attrs: {
                    id: mentionId,
                },
                type: 'mention',
            })
            .run();
    };

    const handleDragStart = (event: DragEvent<HTMLDivElement>, props: HandleDataPillClickProps) => {
        const mentionId = buildMentionId(props);

        const payload: DataPillDragPayloadType = {
            mentionId,
        };

        const target = event.currentTarget;
        const clone = target.cloneNode(true) as HTMLDivElement;

        clone.style.position = 'absolute';
        clone.style.top = '-9999px';
        clone.style.left = '-9999px';

        document.body.appendChild(clone);

        event.dataTransfer.setDragImage(clone, clone.offsetWidth / 2, clone.offsetHeight / 2);

        requestAnimationFrame(() => {
            document.body.removeChild(clone);
        });

        event.dataTransfer.setData('application/bytechef-datapill', JSON.stringify(payload));
        event.dataTransfer.effectAllowed = 'copy';

        setIsDraggingDataPill(true);
    };

    const handleDragEnd = () => {
        setIsDraggingDataPill(false);
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
                    onDragEnd={handleDragEnd}
                    onDragStart={(event) => handleDragStart(event, {workflowNodeName})}
                >
                    <span className="pointer-events-none mr-2" title={property?.type}>
                        {TYPE_ICONS[property?.type as keyof typeof TYPE_ICONS]}
                    </span>

                    <span className="pointer-events-none">{workflowNodeName}</span>
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
                            'mr-auto inline-flex cursor-pointer items-center rounded-full border bg-surface-neutral-secondary px-2 py-0.5 text-sm hover:bg-surface-main',
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
                        onDragEnd={handleDragEnd}
                        onDragStart={(event) =>
                            handleDragStart(event, {
                                parentPropertyName: parentProperty?.name,
                                path,
                                propertyName: property?.name || '[index]',
                                workflowNodeName,
                            })
                        }
                    >
                        {property?.name && (
                            <span className="pointer-events-none mr-2" title={property?.type}>
                                {TYPE_ICONS[property?.type as keyof typeof TYPE_ICONS]}
                            </span>
                        )}

                        {!property?.name && (
                            <span className="pointer-events-none mr-2" title={property?.type}>
                                {TYPE_ICONS[property?.type as keyof typeof TYPE_ICONS]}
                            </span>
                        )}

                        <span className="pointer-events-none">{property?.name || '[index]'}</span>
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
