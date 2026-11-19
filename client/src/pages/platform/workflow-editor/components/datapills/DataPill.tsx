import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {resolveArrayIndexTemplate} from '@/pages/platform/workflow-editor/utils/dataPillArrayIndex';
import {buildDataPillPath, getDataPillSampleValue} from '@/pages/platform/workflow-editor/utils/dataPillPath';
import {TYPE_ICONS} from '@/shared/typeIcons';
import {DataPillDragPayloadType, PropertyAllType} from '@/shared/types';
import {handleActivationKeyDown} from '@/shared/util/keyboard-utils';
import {DragEvent, MouseEvent} from 'react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

import useDataPillPanelStore from '../../stores/useDataPillPanelStore';

interface HandleDataPillClickProps {
    workflowNodeName: string;
    propertyName?: string;
    parentPropertyName?: string;
    pathSegments?: Array<string>;
}

interface DataPillProps {
    componentIcon?: string;
    workflowNodeName: string;
    onClick?: (event: MouseEvent<HTMLDivElement>) => void;
    parentProperty?: PropertyAllType;
    pathSegments?: Array<string>;
    property?: PropertyAllType;
    root?: boolean;
    /* eslint-disable  @typescript-eslint/no-explicit-any */
    sampleOutput?: any;
}

const DataPillSampleValue = ({sampleOutput}: {sampleOutput: string | number | boolean | object | null}) => {
    const sampleOutputString =
        typeof sampleOutput === 'object' && sampleOutput !== null ? JSON.stringify(sampleOutput) : String(sampleOutput);

    if (sampleOutputString.length > 27) {
        return (
            <Tooltip>
                <TooltipTrigger asChild>
                    <span className="flex-1 truncate text-xs text-muted-foreground">{sampleOutputString}</span>
                </TooltipTrigger>

                <TooltipContent className="max-h-96 max-w-96 overflow-y-scroll break-all whitespace-pre-wrap">
                    {sampleOutputString}
                </TooltipContent>
            </Tooltip>
        );
    }

    return <span className="flex-1 text-xs text-muted-foreground">{sampleOutputString}</span>;
};

const buildMentionId = ({
    parentPropertyName,
    pathSegments,
    propertyName,
    workflowNodeName,
}: HandleDataPillClickProps): string => {
    if (!propertyName) {
        return workflowNodeName;
    }

    const parentSegments = parentPropertyName ? [parentPropertyName, propertyName] : [propertyName];

    return resolveArrayIndexTemplate(buildDataPillPath(workflowNodeName, pathSegments || parentSegments));
};

const DataPill = ({
    componentIcon,
    parentProperty,
    pathSegments,
    property,
    root = false,
    sampleOutput,
    workflowNodeName,
}: DataPillProps) => {
    const {pillTarget} = useWorkflowNodeDetailsPanelStore(
        useShallow((state) => ({
            pillTarget: state.pillTarget,
        }))
    );

    const setIsDraggingDataPill = useDataPillPanelStore((state) => state.setIsDraggingDataPill);

    const subProperties = property?.properties || property?.items;

    if (!property?.name && property?.controlType === 'ARRAY_BUILDER') {
        property.name = '[index]';
    }

    const handleDataPillClick = ({
        parentPropertyName,
        pathSegments,
        propertyName,
        workflowNodeName,
    }: HandleDataPillClickProps) => {
        if (!pillTarget || !pillTarget.acceptsPill()) {
            return;
        }

        pillTarget.insertPill(
            buildMentionId({
                parentPropertyName,
                pathSegments,
                propertyName,
                workflowNodeName,
            })
        );
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

    const getSubPropertyPathSegments = (subPropertyName = '[index]') => [
        ...(pathSegments || [property?.name || '[index]']),
        subPropertyName,
    ];

    if (root) {
        return (
            <div className="flex w-full items-center gap-2">
                <div
                    className={twMerge(
                        'inline-flex cursor-pointer items-center space-x-2 rounded-full border bg-surface-neutral-secondary px-2 py-0.5 text-sm hover:bg-surface-main dark:hover:bg-surface-neutral-tertiary',
                        !pillTarget && 'cursor-not-allowed'
                    )}
                    draggable
                    onClick={() => handleDataPillClick({workflowNodeName})}
                    onDragEnd={handleDragEnd}
                    onDragStart={(event) => handleDragStart(event, {workflowNodeName})}
                    onKeyDown={handleActivationKeyDown}
                    role="button"
                    tabIndex={0}
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
                            'mr-auto inline-flex cursor-pointer items-center rounded-full border bg-surface-neutral-secondary px-2 py-0.5 text-sm hover:bg-surface-main dark:hover:bg-surface-neutral-tertiary',
                            !pillTarget && 'cursor-not-allowed'
                        )}
                        data-name={property?.name || workflowNodeName}
                        draggable
                        onClick={() =>
                            handleDataPillClick({
                                parentPropertyName: parentProperty?.name,
                                pathSegments,
                                propertyName: property?.name || '[index]',
                                workflowNodeName,
                            })
                        }
                        onDragEnd={handleDragEnd}
                        onDragStart={(event) =>
                            handleDragStart(event, {
                                parentPropertyName: parentProperty?.name,
                                pathSegments,
                                propertyName: property?.name || '[index]',
                                workflowNodeName,
                            })
                        }
                        onKeyDown={handleActivationKeyDown}
                        role="button"
                        tabIndex={0}
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
                    <TooltipContent className="max-w-72 break-normal whitespace-normal" side="right" sideOffset={8}>
                        <span className="block">{property.description}</span>
                    </TooltipContent>
                )}
            </Tooltip>

            {!!subProperties?.length && (
                <ul className="mt-2 flex flex-col space-y-2 border-l border-l-border/50 pl-4">
                    {subProperties?.map((subProperty, index) => {
                        let sampleValue;

                        if (typeof sampleOutput === 'object') {
                            sampleValue = getDataPillSampleValue(
                                sampleOutput,
                                getSubPropertyPathSegments(subProperty.name)
                            );
                        } else {
                            sampleValue = sampleOutput;
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
                                className="flex items-center gap-2"
                                key={`${workflowNodeName}-${subProperty.name}-${index}`}
                            >
                                <DataPill
                                    componentIcon={componentIcon}
                                    parentProperty={property}
                                    pathSegments={getSubPropertyPathSegments(subProperty.name)}
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
