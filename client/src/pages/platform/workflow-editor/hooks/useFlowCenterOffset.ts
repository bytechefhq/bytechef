import {useStore} from '@xyflow/react';
import {useLayoutEffect, useState} from 'react';

import useOverlayPanelsState from '../hooks/useOverlayPanelsState';
import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import {computeOverlayViewportOffset} from '../utils/overlayPanelViewport';

const TOP_ROW_TOLERANCE_PX = 2;

export default function useFlowCenterOffset(contentWidth = 0): number {
    const [offset, setOffset] = useState(0);

    const layoutDirection = useLayoutDirectionStore((state) => state.layoutDirection);

    const containerWidth = useStore((state) => state.width);
    const domNode = useStore((state) => state.domNode);
    const transform = useStore((state) => state.transform.join());
    const nodePositions = useStore((state) => {
        let positions = '';

        for (const node of state.nodeLookup.values()) {
            positions += `${node.position.x},`;
        }

        return positions;
    });

    const overlayPanelsState = useOverlayPanelsState();

    const overlayWidth = -2 * computeOverlayViewportOffset(overlayPanelsState);

    useLayoutEffect(() => {
        if (!domNode) {
            return;
        }

        const nodeElements = domNode.querySelectorAll<HTMLElement>('.react-flow__node[data-id]');

        const boxRects: Array<Pick<DOMRect, 'left' | 'right' | 'top'>> = [];

        for (const nodeElement of nodeElements) {
            const box = nodeElement.querySelector<HTMLElement>('[data-node-box]');

            if (!box) {
                continue;
            }

            boxRects.push(box.getBoundingClientRect());
        }

        const containerRect = domNode.getBoundingClientRect();
        const containerCenterX = containerRect.left + containerRect.width / 2;
        const visibleRight = containerRect.left + containerRect.width - overlayWidth;
        const visibleCenterX = (containerRect.left + visibleRight) / 2;

        const minimumCenterX = containerRect.left + contentWidth / 2;
        const maximumCenterX = visibleRight - contentWidth / 2;

        const applyCenter = (centerX: number) => {
            const clampedCenterX =
                minimumCenterX > maximumCenterX
                    ? visibleCenterX
                    : Math.min(maximumCenterX, Math.max(minimumCenterX, centerX));

            setOffset(Math.round(clampedCenterX - containerCenterX));
        };

        if (boxRects.length === 0) {
            applyCenter(visibleCenterX);

            return;
        }

        const topY = Math.min(...boxRects.map((boxRect) => boxRect.top));

        const measuredRects =
            layoutDirection === 'LR'
                ? boxRects
                : boxRects.filter((boxRect) => boxRect.top - topY < TOP_ROW_TOLERANCE_PX);

        const flowLeft = Math.max(containerRect.left, Math.min(...measuredRects.map((boxRect) => boxRect.left)));
        const flowRight = Math.min(visibleRight, Math.max(...measuredRects.map((boxRect) => boxRect.right)));

        if (flowLeft >= flowRight) {
            applyCenter(visibleCenterX);

            return;
        }

        const flowCenterX = (flowLeft + flowRight) / 2;

        applyCenter(flowCenterX);
    }, [containerWidth, contentWidth, domNode, layoutDirection, nodePositions, overlayWidth, transform]);

    return offset;
}
