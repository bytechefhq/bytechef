import {LayoutDirectionType} from '@/shared/constants';
import {FitViewOptions, Node, Rect, Viewport} from '@xyflow/react';

export const INITIAL_FIT_VIEW_OPTIONS: FitViewOptions = {
    duration: 0,
    minZoom: 0.2,
    padding: {bottom: '16px', left: '16px', right: '16px', top: '64px'},
};

interface IsCrossAxisOverflowingProps {
    bounds: Rect;
    flowHeight: number;
    flowWidth: number;
    layoutDirection: LayoutDirectionType;
    viewport: Viewport;
}

export function isCrossAxisOverflowing({
    bounds,
    flowHeight,
    flowWidth,
    layoutDirection,
    viewport,
}: IsCrossAxisOverflowingProps): boolean {
    if (layoutDirection === 'LR') {
        const top = viewport.y + bounds.y * viewport.zoom;
        const bottom = top + bounds.height * viewport.zoom;

        return top < 0 || bottom > flowHeight;
    }

    const left = viewport.x + bounds.x * viewport.zoom;
    const right = left + bounds.width * viewport.zoom;

    return left < 0 || right > flowWidth;
}

export function getTopLevelTaskNames(definition?: string): string[] {
    if (!definition) {
        return [];
    }

    try {
        const parsedDefinition = JSON.parse(definition) as {tasks?: Array<{name?: string}>};

        return (parsedDefinition.tasks ?? [])
            .map((task) => task.name)
            .filter((taskName): taskName is string => !!taskName);
    } catch {
        return [];
    }
}

export function isWorkflowLaidOut(nodes: Node[], topLevelTaskNames: string[]): boolean {
    if (nodes.length === 0) {
        return false;
    }

    const nodeIds = new Set(nodes.map((node) => node.id));

    return topLevelTaskNames.every((taskName) => nodeIds.has(taskName));
}
