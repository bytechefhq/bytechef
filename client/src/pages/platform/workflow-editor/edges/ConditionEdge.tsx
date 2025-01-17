import {EdgeProps} from '@xyflow/react';

export default function ConditionEdge({id, sourceX, sourceY, targetX, targetY}: EdgeProps) {
    const offsetY = 30;

    const edgePath = `
        M${sourceX},${sourceY}
        V${sourceY + offsetY}
        H${targetX}
        V${targetY}
    `;

    return (
        <g>
            <path className="fill-none stroke-gray-300 stroke-2" d={edgePath} id={id} style={{borderRadius: '30px'}} />
        </g>
    );
}
