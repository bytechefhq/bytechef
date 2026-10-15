import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useAiAgentEvalsStore} from '@/pages/platform/cluster-element-editor/ai-agent-evals/stores/useAiAgentEvalsStore';
import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {NodeDataType} from '@/shared/types';
import {Handle, Position} from '@xyflow/react';
import {
    BrushCleaningIcon,
    ChevronsDownUpIcon,
    ExternalLinkIcon,
    FlaskConicalIcon,
    LockIcon,
    LockOpenIcon,
    PlayIcon,
    SparklesIcon,
    ZapIcon,
} from 'lucide-react';
import {ReactNode, memo, useCallback} from 'react';
import {twMerge} from 'tailwind-merge';

import {useClusterElementsCanvasDialogStore} from '../components/stores/useClusterElementsCanvasDialogStore';
import {useWorkflowEditor} from '../providers/workflowEditorProvider';
import useClusterFrameCollapsedStore from '../stores/useClusterFrameCollapsedStore';
import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import {clearClusterElementPositions} from '../utils/clearAllClusterElementPositions';
import {
    CLUSTER_FRAME_BORDER_WIDTH,
    CLUSTER_FRAME_HEADER_HEIGHT,
    DEFAULT_CLUSTER_FRAME_CONTENT_ORIGIN,
} from '../utils/clusterFrame/clusterFrameGeometry';
import {mapHandlePosition} from '../utils/directionUtils';
import {getTask} from '../utils/getTask';
import saveWorkflowDefinition from '../utils/saveWorkflowDefinition';
import styles from './NodeTypes.module.css';

const HEADER_BUTTON_CLASSNAME =
    'nodrag flex items-center gap-1 rounded px-2 py-1 text-xs text-content-neutral-secondary hover:bg-surface-neutral-secondary';

interface ClusterFrameShellProps {
    children: ReactNode;
    data: NodeDataType;
    nodeId: string;
    nodeMenu?: ReactNode;
}

const ClusterFrameShell = ({children, data, nodeId, nodeMenu}: ClusterFrameShellProps) => {
    const layoutDirection = useLayoutDirectionStore((state) => state.layoutDirection);
    const locked = useWorkflowEditorStore((state) => state.clusterFrameLockedByRootId?.[nodeId] !== false);
    const setClusterFrameLocked = useWorkflowEditorStore((state) => state.setClusterFrameLocked);
    const setClusterElementsCanvasOpen = useWorkflowEditorStore((state) => state.setClusterElementsCanvasOpen);
    const setRootClusterElementNodeData = useWorkflowEditorStore((state) => state.setRootClusterElementNodeData);
    const workflowId = useWorkflowDataStore((state) => state.workflow.id);
    const setClusterFrameCollapsed = useClusterFrameCollapsedStore((state) => state.setClusterFrameCollapsed);
    const setTestingPanelOpen = useClusterElementsCanvasDialogStore((state) => state.setTestingPanelOpen);
    const setEvalsPanelOpen = useAiAgentEvalsStore((state) => state.setEvalsPanelOpen);
    const setContext = useCopilotStore((state) => state.setContext);
    const setCopilotPanelOpen = useCopilotPanelStore((state) => state.setCopilotPanelOpen);
    const ai = useApplicationInfoStore((state) => state.ai);
    const ff_1570 = useFeatureFlagsStore()('ff-1570');
    const ff_4070 = useFeatureFlagsStore()('ff-4070');
    const ff_4553 = useFeatureFlagsStore()('ff-4553');

    const {updateWorkflowMutation} = useWorkflowEditor();

    const clusterFrame = data.clusterFrame;
    const isAiAgentClusterRoot = data.componentName === 'aiAgent';
    const copilotEnabled = ff_4070 && ai.copilot.enabled && ff_1570;

    const handleToggleLock = useCallback(() => {
        setClusterFrameLocked(nodeId, !locked);
    }, [locked, nodeId, setClusterFrameLocked]);

    const handleCollapse = useCallback(() => {
        if (workflowId) {
            setClusterFrameCollapsed(workflowId, nodeId, true);
        }
    }, [nodeId, setClusterFrameCollapsed, workflowId]);

    const handleResetLayout = useCallback(() => {
        if (!updateWorkflowMutation) {
            return;
        }

        const {workflow} = useWorkflowDataStore.getState();

        if (!workflow.definition) {
            return;
        }

        const workflowDefinitionTasks = JSON.parse(workflow.definition).tasks ?? [];

        const clusterRootTask = getTask({tasks: workflowDefinitionTasks, workflowNodeName: nodeId});

        if (!clusterRootTask?.clusterElements) {
            return;
        }

        const clearedClusterElements = clearClusterElementPositions(clusterRootTask.clusterElements);

        void saveWorkflowDefinition({
            nodeData: {
                ...clusterRootTask,
                clusterElements: clearedClusterElements,
                componentName: clusterRootTask.type.split('/')[0],
                workflowNodeName: nodeId,
            },
            updateWorkflowMutation,
        });
    }, [nodeId, updateWorkflowMutation]);

    const handleOpenEvals = useCallback(() => {
        setRootClusterElementNodeData(data);
        setEvalsPanelOpen(true);
        setClusterElementsCanvasOpen(true);
    }, [data, setClusterElementsCanvasOpen, setEvalsPanelOpen, setRootClusterElementNodeData]);

    const handleOpenPlayground = useCallback(() => {
        setRootClusterElementNodeData(data);
        setTestingPanelOpen(true);
    }, [data, setRootClusterElementNodeData, setTestingPanelOpen]);

    const handleOpenCopilot = useCallback(() => {
        setRootClusterElementNodeData(data);

        const {context: currentContext, generateConversationId, resetMessages} = useCopilotStore.getState();

        resetMessages();
        generateConversationId();

        setContext({
            ...currentContext,
            mode: MODE.ASK,
            parameters: {taskName: data.name},
            source: Source.CLUSTER_ELEMENT,
        });

        setCopilotPanelOpen(true);
    }, [data, setContext, setCopilotPanelOpen, setRootClusterElementNodeData]);

    if (!clusterFrame) {
        return <>{children}</>;
    }

    const contentOrigin = clusterFrame.contentOrigin ?? DEFAULT_CLUSTER_FRAME_CONTENT_ORIGIN;

    return (
        <div
            className="group relative rounded-lg border-2 border-dashed border-stroke-neutral-secondary bg-surface-neutral-secondary/40"
            data-nodetype="clusterFrame"
            data-testid="cluster-frame-shell"
            style={{height: clusterFrame.height, width: clusterFrame.width}}
        >
            {nodeMenu && (
                <div
                    className="nodrag invisible absolute top-2 -left-8 z-10 group-hover:visible focus-within:visible"
                    onMouseDown={(event) => event.stopPropagation()}
                >
                    {nodeMenu}
                </div>
            )}

            <div className="flex items-center justify-between px-3" style={{height: CLUSTER_FRAME_HEADER_HEIGHT}}>
                <span className="text-sm font-semibold text-content-neutral-secondary">{data.label}</span>

                {updateWorkflowMutation && (
                    <div className="flex items-center gap-1">
                        {isAiAgentClusterRoot && (
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <a
                                        aria-label="Manage AI Skills"
                                        className={twMerge(HEADER_BUTTON_CLASSNAME, 'relative')}
                                        href="/automation/settings/ai/skills"
                                        rel="noreferrer"
                                        target="_blank"
                                    >
                                        <ZapIcon className="size-3.5" />

                                        <ExternalLinkIcon
                                            aria-hidden
                                            className="absolute top-0 right-0 size-2 text-content-neutral-secondary/70"
                                        />
                                    </a>
                                </TooltipTrigger>

                                <TooltipContent>Manage AI Skills</TooltipContent>
                            </Tooltip>
                        )}

                        {ff_4553 && isAiAgentClusterRoot && (
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <button
                                        aria-label="Agent Evals"
                                        className={HEADER_BUTTON_CLASSNAME}
                                        onClick={handleOpenEvals}
                                        type="button"
                                    >
                                        <FlaskConicalIcon className="size-3.5" />
                                    </button>
                                </TooltipTrigger>

                                <TooltipContent>Agent Evals</TooltipContent>
                            </Tooltip>
                        )}

                        {isAiAgentClusterRoot && (
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <button
                                        aria-label="Test agent"
                                        className={HEADER_BUTTON_CLASSNAME}
                                        onClick={handleOpenPlayground}
                                        type="button"
                                    >
                                        <PlayIcon className="size-3.5" />
                                    </button>
                                </TooltipTrigger>

                                <TooltipContent>Test agent</TooltipContent>
                            </Tooltip>
                        )}

                        {copilotEnabled && (
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <button
                                        aria-label="Open Copilot panel"
                                        className={HEADER_BUTTON_CLASSNAME}
                                        onClick={handleOpenCopilot}
                                        type="button"
                                    >
                                        <SparklesIcon className="size-3.5" />
                                    </button>
                                </TooltipTrigger>

                                <TooltipContent>Open Copilot panel</TooltipContent>
                            </Tooltip>
                        )}

                        <button
                            aria-label={locked ? 'Unlock node movement' : 'Lock node movement'}
                            className={HEADER_BUTTON_CLASSNAME}
                            onClick={handleToggleLock}
                            type="button"
                        >
                            {locked ? <LockIcon className="size-3.5" /> : <LockOpenIcon className="size-3.5" />}
                        </button>

                        <button
                            aria-label="Reset layout"
                            className={HEADER_BUTTON_CLASSNAME}
                            onClick={handleResetLayout}
                            type="button"
                        >
                            <BrushCleaningIcon className="size-3.5" />
                        </button>

                        <button
                            aria-label="Collapse cluster elements"
                            className={HEADER_BUTTON_CLASSNAME}
                            onClick={handleCollapse}
                            type="button"
                        >
                            <ChevronsDownUpIcon className="size-3.5" />
                        </button>
                    </div>
                )}
            </div>

            <Handle
                className={styles.handle}
                id={`${nodeId}-top`}
                position={mapHandlePosition(Position.Top, layoutDirection)}
                type="target"
            />

            <Handle
                className={styles.handle}
                id={`${nodeId}-bottom`}
                position={mapHandlePosition(Position.Bottom, layoutDirection)}
                type="source"
            />

            <div
                style={{
                    marginLeft: contentOrigin.x - CLUSTER_FRAME_BORDER_WIDTH,
                    marginTop: contentOrigin.y - CLUSTER_FRAME_HEADER_HEIGHT - CLUSTER_FRAME_BORDER_WIDTH,
                }}
            >
                {children}
            </div>
        </div>
    );
};

export default memo(ClusterFrameShell);
