import {EdgeLabelRenderer} from '@xyflow/react';

import {BinaryCaseLabelAnchorType, BinaryCaseLabelI} from './computeBinaryCaseLabel';

const SELF_ANCHOR_BY_ANCHOR: Record<BinaryCaseLabelAnchorType, string> = {
    bottomCenter: 'translate(-50%, -100%)',
    topCenter: 'translate(-50%, 0%)',
};

interface BinaryCaseLabelProps {
    edgeId: string;
    label: BinaryCaseLabelI;
}

export default function BinaryCaseLabel({edgeId, label}: BinaryCaseLabelProps) {
    return (
        <EdgeLabelRenderer key={`${edgeId}-binary-case-label`}>
            <span
                className="pointer-events-none absolute text-xs leading-none font-bold text-muted-foreground"
                style={{transform: `translate(${label.x}px, ${label.y}px) ${SELF_ANCHOR_BY_ANCHOR[label.anchor]}`}}
            >
                {label.text}
            </span>
        </EdgeLabelRenderer>
    );
}
