import Button from '@/components/Button/Button';
import {HoverCard, HoverCardContent, HoverCardTrigger} from '@/components/ui/hover-card';
import {Skeleton} from '@/components/ui/skeleton';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useGetWorkflowNodeDescriptionQuery} from '@/shared/queries/platform/workflowNodeDescriptions.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {NodeDataType} from '@/shared/types';
import {HoverCardPortal} from '@radix-ui/react-hover-card';
import {useQueryClient} from '@tanstack/react-query';
import {Handle, Position} from '@xyflow/react';
import {ComponentIcon, TrashIcon} from 'lucide-react';
import {memo, useEffect, useMemo, useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import sanitize from 'sanitize-html';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

import {extractClusterElementIcons} from '../../cluster-element-editor/utils/clusterElementsUtils';
import useNodeClickHandler from '../hooks/useNodeClick';
import {useWorkflowEditor} from '../providers/workflowEditorProvider';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import {getTask} from '../utils/getTask';
import handleDeleteTask from '../utils/handleDeleteTask';
import styles from './NodeTypes.module.css';

const AiAgentNode = ({data, id}: {data: NodeDataType; id: string}) => {
    const [hoveredNodeName, setHoveredNodeName] = useState<string | undefined>();
    const [hasIcons, setHasIcons] = useState(false);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const {currentNode} = useWorkflowNodeDetailsPanelStore(
        useShallow((state) => ({
            currentNode: state.currentNode,
        }))
    );
    const workflow = useWorkflowDataStore((state) => state.workflow);
    const clusterElementsCanvasOpen = useWorkflowEditorStore((state) => state.clusterElementsCanvasOpen);
    const queryClient = useQueryClient();
    const {invalidateWorkflowQueries, updateWorkflowMutation} = useWorkflowEditor();

    const memoizedIconsList = useMemo(() => {
        if (!workflow.definition) {
            return {iconsToShow: [], remainingIcons: []};
        }

        const workflowDefinitionTasks = JSON.parse(workflow.definition).tasks;

        const mainClusterRootTask = data?.workflowNodeName
            ? getTask({
                  tasks: workflowDefinitionTasks,
                  workflowNodeName: data.workflowNodeName,
              })
            : undefined;

        if (!mainClusterRootTask?.clusterElements) {
            return {iconsToShow: [], remainingIcons: []};
        }

        const iconsList = extractClusterElementIcons(mainClusterRootTask.clusterElements);

        if (!Array.isArray(iconsList)) {
            return {iconsToShow: [], remainingIcons: []};
        }

        const uniqueIcons = iconsList.reduce((uniqueIconsList, iconItem) => {
            if (!uniqueIconsList.has(iconItem.icon)) {
                uniqueIconsList.set(iconItem.icon, iconItem);
            }

            return uniqueIconsList;
        }, new Map<string, {icon: string; label: string}>());

        const uniqueIconsList = Array.from(uniqueIcons.values());

        return {
            iconsToShow: uniqueIconsList.slice(0, 5),
            remainingIcons: uniqueIconsList.slice(5),
        };
    }, [data?.workflowNodeName, workflow.definition]);

    const {data: workflowNodeDescription} = useGetWorkflowNodeDescriptionQuery(
        {
            environmentId: currentEnvironmentId,
            id: workflow.id!,
            workflowNodeName: hoveredNodeName!,
        },
        hoveredNodeName !== undefined
    );

    const handleNodeClick = useNodeClickHandler(data, id);

    const handleDeleteNodeClick = (data: NodeDataType) => {
        if (data) {
            handleDeleteTask({
                currentNode,
                data,
                invalidateWorkflowQueries: invalidateWorkflowQueries!,
                queryClient,
                updateWorkflowMutation: updateWorkflowMutation!,
                workflow,
            });
        }
    };

    useEffect(() => {
        if (memoizedIconsList.iconsToShow.length > 0) {
            setHasIcons(true);
        } else {
            setHasIcons(false);
        }
    }, [memoizedIconsList.iconsToShow]);

    return (
        <div
            className="nodrag group relative flex min-w-60 cursor-pointer items-center justify-center"
            data-nodetype="aiAgentNode"
            key={id}
        >
            <div className="invisible absolute left-workflow-node-popover-hover pr-4 group-hover:visible">
                <Button
                    className="opacity-100"
                    icon={<TrashIcon />}
                    onClick={() => handleDeleteNodeClick(data)}
                    size="iconSm"
                    title="Delete a node"
                    variant="destructiveGhost"
                />
            </div>

            <HoverCard
                key={id}
                onOpenChange={(open) => {
                    if (open) {
                        setHoveredNodeName(data.name);
                    } else {
                        setHoveredNodeName(undefined);
                    }
                }}
            >
                <HoverCardTrigger>
                    <Button
                        className={twMerge(
                            'size-18 focus-visible:ring-stroke-brand-focus flex w-full flex-col items-center justify-center rounded-md border-2 border-stroke-neutral-tertiary bg-surface-neutral-primary p-4 shadow hover:border-stroke-brand-secondary-hover hover:bg-surface-neutral-primary hover:shadow-none active:bg-surface-neutral-primary'
                        )}
                        onClick={handleNodeClick}
                    >
                        <span className="self-center text-content-neutral-primary [&_svg]:size-9">
                            {data.icon ? data.icon : <ComponentIcon className="size-9 text-content-neutral-primary" />}
                        </span>

                        {memoizedIconsList.iconsToShow.length > 0 && (
                            <ul
                                className={twMerge(
                                    'mt-2 flex min-w-52 items-center justify-start',
                                    !hasIcons && 'hidden'
                                )}
                            >
                                {memoizedIconsList.iconsToShow.map((iconUrlObject, index) => (
                                    <Tooltip key={index}>
                                        <TooltipTrigger asChild>
                                            <li
                                                className="mr-2 flex items-center justify-center rounded-full border bg-surface-neutral-primary p-1 [&_svg]:size-5"
                                                key={index}
                                            >
                                                {iconUrlObject ? (
                                                    <InlineSVG
                                                        className="size-9 flex-none text-content-neutral-primary"
                                                        src={iconUrlObject.icon}
                                                    />
                                                ) : (
                                                    <Skeleton className="size-9 rounded-full" />
                                                )}
                                            </li>
                                        </TooltipTrigger>

                                        <TooltipContent
                                            className="text-pretty border border-stroke-neutral-tertiary bg-surface-neutral-primary text-content-neutral-primary"
                                            side="bottom"
                                        >
                                            {iconUrlObject?.label}
                                        </TooltipContent>
                                    </Tooltip>
                                ))}

                                {memoizedIconsList.remainingIcons.length > 0 && (
                                    <Tooltip>
                                        <TooltipTrigger asChild>
                                            <div className="flex size-7 items-center justify-center self-center rounded-full border border-stroke-neutral-secondary bg-surface-neutral-primary p-1">
                                                <span className="self-center text-xs font-bold text-content-neutral-secondary">
                                                    +{memoizedIconsList.remainingIcons.length}
                                                </span>
                                            </div>
                                        </TooltipTrigger>

                                        <TooltipContent
                                            className="max-w-36 text-pretty border border-stroke-neutral-tertiary bg-surface-neutral-primary text-content-neutral-primary"
                                            side="bottom"
                                        >
                                            <ul>
                                                {memoizedIconsList.remainingIcons.map((iconUrlObject, index) => (
                                                    <li className="my-2" key={index}>
                                                        {iconUrlObject.label}
                                                    </li>
                                                ))}
                                            </ul>
                                        </TooltipContent>
                                    </Tooltip>
                                )}
                            </ul>
                        )}
                    </Button>
                </HoverCardTrigger>

                {!clusterElementsCanvasOpen && (
                    <HoverCardPortal>
                        <HoverCardContent className="w-fit min-w-72 max-w-xl text-sm" side="right">
                            {workflowNodeDescription?.description && (
                                <div
                                    className="flex"
                                    dangerouslySetInnerHTML={{
                                        __html: sanitize(workflowNodeDescription.description, {
                                            allowedAttributes: {
                                                div: ['class'],
                                                table: ['class'],
                                                td: ['class'],
                                                tr: ['class'],
                                            },
                                        }),
                                    }}
                                />
                            )}
                        </HoverCardContent>
                    </HoverCardPortal>
                )}
            </HoverCard>

            <div className="ml-2 flex w-full min-w-max flex-col items-start">
                <span className="font-semibold">{data.title || data.label}</span>

                {data.operationName && <pre className="text-sm">{data.operationName}</pre>}

                <span className="text-sm text-content-neutral-secondary">{data.workflowNodeName}</span>
            </div>

            <Handle
                className={styles.handle}
                isConnectable={false}
                position={Position.Top}
                style={hasIcons ? {left: '120px'} : {left: '36px'}}
                type="target"
            />

            <Handle
                className={styles.handle}
                isConnectable={false}
                position={Position.Bottom}
                style={hasIcons ? {left: '120px'} : {left: '36px'}}
                type="source"
            />
        </div>
    );
};
export default memo(AiAgentNode);
