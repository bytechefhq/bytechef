import {EdgeProps} from '@xyflow/react';

export default function LoopDecorativeEdge({id, sourceX, sourceY, targetX, targetY}: EdgeProps) {
    const offsetX = 100;
    const middleX = (sourceX + targetX) / 2;
    const middleY = (sourceY + targetY) / 2;

    const path = `M${sourceX},${sourceY + 30} 
                L${middleX - offsetX},${sourceY + 30} 
                L${middleX - offsetX},${middleY} 
                L${middleX - offsetX},${targetY - 22.5} 
                L${targetX + 6},${targetY - 22.5}`;

    return <path className="fill-none stroke-gray-300 stroke-2" d={path} id={id} />;
}
