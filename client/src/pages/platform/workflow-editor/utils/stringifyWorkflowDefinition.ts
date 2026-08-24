import {SPACE} from '@/shared/constants';

/**
 * A task declares its connections as a map keyed by connection name in the workflow definition, but
 * the REST DTO exposes the same field as an array of ComponentConnection. A DTO shaped task that
 * reaches the definition therefore writes an array the server cannot read, since
 * `ComponentConnection.of` parses the field with `MapUtils.getMap`.
 *
 * Dropping the array shape while serializing keeps the invalid shape out of saved definitions no
 * matter which caller assembled the tasks, and heals definitions that already carry it on their next
 * save.
 */
function dropArrayShapedConnections(this: unknown, key: string, value: unknown): unknown {
    if (key === 'connections' && Array.isArray(value) && isTaskLike(this)) {
        return undefined;
    }

    return value;
}

/**
 * Only tasks and cluster elements own a `connections` declaration. Component parameters are free to
 * hold a value under the same name, so the holder has to look like a task before its connections are
 * dropped. This matches the task detection in flattenDefinitionTasks.
 */
function isTaskLike(value: unknown): boolean {
    if (value === null || typeof value !== 'object') {
        return false;
    }

    const {name, type} = value as {name?: unknown; type?: unknown};

    return typeof name === 'string' && typeof type === 'string' && type.includes('/');
}

export default function stringifyWorkflowDefinition(definition: unknown): string {
    return JSON.stringify(definition, dropArrayShapedConnections, SPACE);
}
