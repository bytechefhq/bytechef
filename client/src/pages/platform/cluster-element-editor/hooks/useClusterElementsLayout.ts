import {COPILOT_PANEL_WIDTH, DATA_PILL_PANEL_WIDTH, NODE_DETAILS_PANEL_WIDTH} from '@/shared/constants';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {Edge, Node} from '@xyflow/react';
import {useEffect, useMemo, useRef} from 'react';
import {useShallow} from 'zustand/react/shallow';

import {useClusterElementsCanvasDialogStore} from '../../workflow-editor/components/stores/useClusterElementsCanvasDialogStore';
import useDataPillPanelStore from '../../workflow-editor/stores/useDataPillPanelStore';
import useWorkflowEditorStore from '../../workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '../../workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import animateNodePositions from '../../workflow-editor/utils/animateNodePositions';
import {getClusterElementsLayoutElements} from '../../workflow-editor/utils/layoutUtils';
import useClusterElementsDataStore from '../stores/useClusterElementsDataStore';
import useClusterElementNodes from './useClusterElementNodes';

const hasNodeSetChanged = (currentNodes: Array<Node>, layoutNodes: Array<Node>) => {
    if (currentNodes.length !== layoutNodes.length) {
        return true;
    }

    const currentNodeIds = new Set(currentNodes.map((node) => node.id));

    return layoutNodes.some((node) => !currentNodeIds.has(node.id));
};

const useClusterElementsLayout = () => {
    const {rootClusterElementNodeData, setClusterRootComponentDefinition} = useWorkflowEditorStore(
        useShallow((state) => ({
            rootClusterElementNodeData: state.rootClusterElementNodeData,
            setClusterRootComponentDefinition: state.setClusterRootComponentDefinition,
        }))
    );
    const {isNodeDragging, isPositionSaving, layoutResetCounter, nodesLocked} = useClusterElementsDataStore(
        useShallow((state) => ({
            isNodeDragging: state.isNodeDragging,
            isPositionSaving: state.isPositionSaving,
            layoutResetCounter: state.layoutResetCounter,
            nodesLocked: state.nodesLocked,
        }))
    );
    const {workflowNodeDetailsPanelOpen} = useWorkflowNodeDetailsPanelStore(
        useShallow((state) => ({
            workflowNodeDetailsPanelOpen: state.workflowNodeDetailsPanelOpen,
        }))
    );
    const {dataPillPanelOpen} = useDataPillPanelStore(
        useShallow((state) => ({
            dataPillPanelOpen: state.dataPillPanelOpen,
        }))
    );
    const copilotPanelOpen = useClusterElementsCanvasDialogStore((state) => state.copilotPanelOpen);

    const mainClusterRootQueryParameters = useMemo(() => {
        if (!rootClusterElementNodeData?.type || !rootClusterElementNodeData?.componentName) {
            return {
                componentName: '',
                componentVersion: 1,
            };
        }

        return {
            componentName: rootClusterElementNodeData?.componentName,
            componentVersion: Number(rootClusterElementNodeData?.type?.split('/')[1]?.replace(/^v/, '')) || 1,
        };
    }, [rootClusterElementNodeData]);

    const {data: rootClusterElementDefinition} = useGetComponentDefinitionQuery(
        mainClusterRootQueryParameters,
        !!rootClusterElementNodeData?.workflowNodeName
    );

    const {setEdges, setNodes} = useClusterElementsDataStore(
        useShallow((state) => ({
            setEdges: state.setEdges,
            setNodes: state.setNodes,
        }))
    );

    // Dialog: left-16 (64px), w-[calc(100vw-80px)] → right edge at 100vw-16px.
    // Fixed panels at right-0 extend 16px past the dialog's right edge,
    // so only (panelWidth - 16) actually overlaps the visible canvas.
    const dialogRightGap = 16;

    const canvasWidth = useMemo(() => {
        let width = window.innerWidth - 80;

        if (copilotPanelOpen) {
            width -= COPILOT_PANEL_WIDTH;
        }

        if (workflowNodeDetailsPanelOpen) {
            width -= NODE_DETAILS_PANEL_WIDTH;

            if (dataPillPanelOpen) {
                width -= DATA_PILL_PANEL_WIDTH;
            }
        }

        if (copilotPanelOpen || workflowNodeDetailsPanelOpen) {
            width += dialogRightGap;
        }

        return width;
    }, [copilotPanelOpen, dataPillPanelOpen, workflowNodeDetailsPanelOpen]);

    // Dialog uses h-[calc(100vh-64px)]
    const canvasHeight = window.innerHeight - 64;

    const previousCanvasWidthRef = useRef(canvasWidth);
    const cancelAnimationRef = useRef<(() => void) | null>(null);
    const animationTargetNodesRef = useRef<Node[] | null>(null);
    const laidOutLayoutResetCounterRef = useRef(layoutResetCounter);

    const clusterRootIds = useMemo(
        () => (rootClusterElementNodeData?.workflowNodeName ? [rootClusterElementNodeData.workflowNodeName] : []),
        [rootClusterElementNodeData?.workflowNodeName]
    );

    const {definitionsReady, edgesByRootId, nodesByRootId} = useClusterElementNodes(clusterRootIds);

    const rootId = clusterRootIds[0];
    const allNodes = useMemo(() => (rootId ? (nodesByRootId[rootId] ?? []) : []), [nodesByRootId, rootId]);
    const taskEdges = useMemo(() => (rootId ? (edgesByRootId[rootId] ?? []) : []), [edgesByRootId, rootId]);

    useEffect(() => {
        if (
            rootClusterElementDefinition &&
            rootClusterElementNodeData?.workflowNodeName &&
            !isNodeDragging &&
            !isPositionSaving
        ) {
            setClusterRootComponentDefinition(
                rootClusterElementNodeData.workflowNodeName,
                rootClusterElementDefinition
            );
        }
    }, [
        rootClusterElementDefinition,
        rootClusterElementNodeData?.workflowNodeName,
        setClusterRootComponentDefinition,
        isNodeDragging,
        isPositionSaving,
    ]);

    // Structural layout: runs when nodes/edges change, NOT on panel toggle.
    // Wait for nested cluster root definitions before running the first layout
    // to avoid a visible re-layout when they load asynchronously.
    useEffect(() => {
        if (isNodeDragging || isPositionSaving) {
            return;
        }

        if (!definitionsReady) {
            return;
        }

        const layoutNodes = allNodes;
        const edges: Edge[] = taskEdges;

        if (layoutNodes.length === 0) {
            return;
        }

        // A panel-toggle animation still in flight would keep writing its pre-layout snapshot over these nodes,
        // so stop it and lay out from the position it was heading to.
        const currentNodes = animationTargetNodesRef.current || useClusterElementsDataStore.getState().nodes;

        if (cancelAnimationRef.current) {
            cancelAnimationRef.current();

            cancelAnimationRef.current = null;
        }

        animationTargetNodesRef.current = null;

        const layoutResetRequested = layoutResetCounter !== laidOutLayoutResetCounterRef.current;

        if (!nodesLocked && !layoutResetRequested && !hasNodeSetChanged(currentNodes, layoutNodes)) {
            const layoutNodesById = new Map(layoutNodes.map((node) => [node.id, node]));

            setNodes(
                currentNodes.map((node) => {
                    const layoutNode = layoutNodesById.get(node.id);

                    return layoutNode ? {...node, data: layoutNode.data} : node;
                })
            );

            return;
        }

        const currentRootNode = currentNodes.find((node) => node.id === rootClusterElementNodeData?.workflowNodeName);

        const elements = getClusterElementsLayoutElements({
            canvasHeight,
            canvasWidth,
            currentRootPosition: currentRootNode?.position,
            edges,
            nodes: layoutNodes,
        });

        laidOutLayoutResetCounterRef.current = layoutResetCounter;

        setNodes(elements.nodes);
        setEdges(elements.edges);

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [rootClusterElementNodeData, allNodes, layoutResetCounter, definitionsReady]);

    // Panel toggle animation: shift nodes horizontally when canvas width changes
    useEffect(() => {
        const previousWidth = previousCanvasWidthRef.current;

        previousCanvasWidthRef.current = canvasWidth;

        if (previousWidth === canvasWidth) {
            return;
        }

        const currentNodes = useClusterElementsDataStore.getState().nodes;

        if (currentNodes.length === 0) {
            return;
        }

        const currentZoom = useClusterElementsDataStore.getState().canvasZoom;
        const widthDelta = canvasWidth - previousWidth;
        const positionDelta = widthDelta / (2 * currentZoom);

        const targetNodes = currentNodes.map((node) => ({
            ...node,
            position: node.parentId
                ? node.position
                : {
                      ...node.position,
                      x: node.position.x + positionDelta,
                  },
        }));

        if (cancelAnimationRef.current) {
            cancelAnimationRef.current();
        }

        animationTargetNodesRef.current = targetNodes;

        cancelAnimationRef.current = animateNodePositions(currentNodes, targetNodes, (nodes) => {
            if (nodes === targetNodes) {
                animationTargetNodesRef.current = null;
            }

            setNodes(nodes);
        });

        return () => {
            if (cancelAnimationRef.current) {
                cancelAnimationRef.current();
                cancelAnimationRef.current = null;
            }

            animationTargetNodesRef.current = null;
        };
    }, [canvasWidth, setNodes]);
};

export default useClusterElementsLayout;
