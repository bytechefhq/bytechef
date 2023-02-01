import React, {MouseEvent} from 'react';
import {PlusCircledIcon} from '@radix-ui/react-icons';
import {getBezierPath, EdgeProps} from 'reactflow';

const foreignObjectSize = 15;

const ButtonEdge = ({
    data,
    id,
    sourceX,
    sourceY,
    targetX,
    targetY,
    sourcePosition,
    targetPosition,
    style = {},
    markerEnd,
}: EdgeProps): JSX.Element => {
    const [edgePath, labelX, labelY] = getBezierPath({
        sourceX,
        sourceY,
        sourcePosition,
        targetX,
        targetY,
        targetPosition,
    });

    const onEdgeClick = (event: MouseEvent) => {
        event.stopPropagation();

        data.onClick();
    };

    return (
        <>
            <path
                // eslint-disable-next-line tailwindcss/no-custom-classname
                className="react-flow__edge-path"
                d={edgePath}
                id={id}
                style={style}
                markerEnd={markerEnd}
            />

            <foreignObject
                height={foreignObjectSize}
                requiredExtensions="http://www.w3.org/1999/xhtml"
                width={foreignObjectSize}
                x={labelX - foreignObjectSize / 2}
                y={labelY - foreignObjectSize / 2}
            >
                <PlusCircledIcon
                    onClick={(event) => onEdgeClick(event)}
                    className="rounded-lg bg-slate-50 text-blue-600"
                />
            </foreignObject>
        </>
    );
};

export default ButtonEdge;
