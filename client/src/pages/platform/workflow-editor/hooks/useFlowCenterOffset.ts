import {useStore} from '@xyflow/react';
import {useLayoutEffect, useState} from 'react';

import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';

const TOP_ROW_TOLERANCE_PX = 2;

export default function useFlowCenterOffset(): number {
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

        if (boxRects.length === 0) {
            setOffset(0);

            return;
        }

        const topY = Math.min(...boxRects.map((boxRect) => boxRect.top));

        const measuredRects =
            layoutDirection === 'LR'
                ? boxRects
                : boxRects.filter((boxRect) => boxRect.top - topY < TOP_ROW_TOLERANCE_PX);

        const flowLeft = Math.min(...measuredRects.map((boxRect) => boxRect.left));
        const flowRight = Math.max(...measuredRects.map((boxRect) => boxRect.right));

        const containerRect = domNode.getBoundingClientRect();
        const containerCenterX = containerRect.left + containerRect.width / 2;

        const flowCenterX = (flowLeft + flowRight) / 2;

        setOffset(Math.round(flowCenterX - containerCenterX));
    }, [containerWidth, domNode, layoutDirection, nodePositions, transform]);

    return offset;
}
