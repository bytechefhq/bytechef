import {LayoutDirectionType, NODE_WIDTH} from '@/shared/constants';
import {Position} from '@xyflow/react';

const LR_POSITION_MAP: Record<Position, Position> = {
    [Position.Bottom]: Position.Right,
    [Position.Left]: Position.Top,
    [Position.Right]: Position.Bottom,
    [Position.Top]: Position.Left,
};

export function mapHandlePosition(tbPosition: Position, direction: LayoutDirectionType): Position {
    if (direction === 'TB') {
        return tbPosition;
    }

    return LR_POSITION_MAP[tbPosition];
}

export function getDefaultSourcePosition(direction: LayoutDirectionType): Position {
    return direction === 'TB' ? Position.Bottom : Position.Right;
}

export function getDefaultTargetPosition(direction: LayoutDirectionType): Position {
    return direction === 'TB' ? Position.Top : Position.Left;
}

export function getCrossAxis(direction: LayoutDirectionType): 'x' | 'y' {
    return direction === 'TB' ? 'x' : 'y';
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
export function getCrossAxisNodeSize(direction: LayoutDirectionType): number {
    return NODE_WIDTH;
}
