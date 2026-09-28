import {DEFAULT_LAYOUT_DIRECTION, LayoutDirectionType} from '@/shared/constants';
import {UpdateWorkflowMutationType} from '@/shared/types';

import useWorkflowDataStore, {runWithoutHistory} from '../stores/useWorkflowDataStore';
import fireWorkflowDefinitionMutation from './fireWorkflowDefinitionMutation';
import stringifyWorkflowDefinition from './stringifyWorkflowDefinition';
import {enqueuePendingSave, isWorkflowMutating} from './workflowMutationGuard';

function isLayoutDirection(value: unknown): value is LayoutDirectionType {
    return value === 'TB' || value === 'LR';
}

export function extractLayoutDirection(definition?: string): LayoutDirectionType | undefined {
    if (!definition) {
        return undefined;
    }

    try {
        const layoutDirection = JSON.parse(definition).metadata?.ui?.layoutDirection;

        return isLayoutDirection(layoutDirection) ? layoutDirection : undefined;
    } catch {
        return undefined;
    }
}

export function applyLayoutDirectionToDefinition(definition: string, layoutDirection: LayoutDirectionType): string {
    if ((extractLayoutDirection(definition) ?? DEFAULT_LAYOUT_DIRECTION) === layoutDirection) {
        return definition;
    }

    let workflowDefinition;

    try {
        workflowDefinition = JSON.parse(definition);
    } catch {
        return definition;
    }

    workflowDefinition.metadata = {
        ...workflowDefinition.metadata,
        ui: {
            ...workflowDefinition.metadata?.ui,
            layoutDirection,
        },
    };

    return stringifyWorkflowDefinition(workflowDefinition);
}

interface SaveLayoutDirectionProps {
    layoutDirection: LayoutDirectionType;
    updateWorkflowMutation: UpdateWorkflowMutationType;
}

export function saveLayoutDirection({layoutDirection, updateWorkflowMutation}: SaveLayoutDirectionProps) {
    const {workflow} = useWorkflowDataStore.getState();

    if (!workflow.id || !workflow.definition) {
        return;
    }

    if (isWorkflowMutating(workflow.id)) {
        enqueuePendingSave(workflow.id, () => saveLayoutDirection({layoutDirection, updateWorkflowMutation}));

        return;
    }

    const updatedDefinition = applyLayoutDirectionToDefinition(workflow.definition, layoutDirection);

    if (updatedDefinition === workflow.definition) {
        return;
    }

    const previousDefinition = workflow.definition;

    runWithoutHistory(() => {
        useWorkflowDataStore.setState((state) => ({
            workflow: {
                ...state.workflow,
                definition: updatedDefinition,
            },
        }));
    });

    fireWorkflowDefinitionMutation({
        definition: updatedDefinition,
        previousDefinition,
        updateWorkflowMutation,
        version: workflow.version,
        workflowId: workflow.id,
    });
}
