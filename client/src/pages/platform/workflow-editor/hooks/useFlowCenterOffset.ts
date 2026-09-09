import {useStore} from '@xyflow/react';
import {useLayoutEffect, useState} from 'react';

import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';

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

        let topBox: HTMLElement | undefined;
        let topY = Number.POSITIVE_INFINITY;
        let minLeft = Number.POSITIVE_INFINITY;
        let maxRight = Number.NEGATIVE_INFINITY;

        for (const nodeElement of nodeElements) {
            const box = nodeElement.querySelector<HTMLElement>('[data-node-box]');

            if (!box) {
                continue;
            }

            const {left, right, top} = box.getBoundingClientRect();

            minLeft = Math.min(minLeft, left);
            maxRight = Math.max(maxRight, right);

            if (top < topY) {
                topY = top;
                topBox = box;
            }
        }

        if (!topBox) {
            setOffset(0);

            return;
        }

        const containerRect = domNode.getBoundingClientRect();
        const containerCenterX = containerRect.left + containerRect.width / 2;

        const flowCenterX =
            layoutDirection === 'LR'
                ? (minLeft + maxRight) / 2
                : topBox.getBoundingClientRect().left + topBox.getBoundingClientRect().width / 2;

        setOffset(Math.round(flowCenterX - containerCenterX));
    }, [containerWidth, domNode, layoutDirection, nodePositions, transform]);

    return offset;
}
