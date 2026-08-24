import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import {getEffectivelyDisabledTaskNames} from '../utils/getEffectivelyDisabledTaskNames';

const EMPTY_DISABLED_TASK_NAMES: Set<string> = new Set();

/**
 * Single-entry module-level cache keyed on the definition string.
 *
 * The hook is called once per node component, so a `useMemo` alone is per-instance: on a
 * 60-node workflow every definition change (any parameter save restringifies it) meant 60
 * `JSON.parse` calls over the whole definition, 60 full recursive walks, and 60 distinct `Set`
 * identities feeding downstream `useMemo` dependencies. Caching here collapses that to one
 * parse and one walk, and every node receives the same `Set` instance.
 *
 * Module state rather than a store selector: the workflow data store is a temporal (undo/redo)
 * store whose history is keyed on `workflow.definition`, so parking derived state in it would
 * mean either polluting the history payload or a second source of truth to keep in step. A
 * cache keyed on the very string the value is derived from cannot go stale, and module-level
 * mutable state is already the pattern next door in `workflowMutationGuard`.
 */
let cachedDefinition: string | undefined;
let cachedDisabledTaskNames: Set<string> = EMPTY_DISABLED_TASK_NAMES;

function readDisabledTaskNames(definition: string | undefined): Set<string> {
    if (!definition) {
        return EMPTY_DISABLED_TASK_NAMES;
    }

    if (definition === cachedDefinition) {
        return cachedDisabledTaskNames;
    }

    let disabledTaskNames: Set<string>;

    try {
        const parsedDefinition = JSON.parse(definition);

        disabledTaskNames = getEffectivelyDisabledTaskNames(parsedDefinition.tasks ?? []);
    } catch {
        disabledTaskNames = EMPTY_DISABLED_TASK_NAMES;
    }

    cachedDefinition = definition;
    cachedDisabledTaskNames = disabledTaskNames;

    return disabledTaskNames;
}

/**
 * Returns the set of task names that are effectively disabled -- either directly, or by sitting
 * under a disabled ancestor. Falls back to an empty set when the definition hasn't loaded yet or
 * fails to parse.
 */
export default function useDisabledTaskNames(): Set<string> {
    const definition = useWorkflowDataStore((state) => state.workflow.definition);

    return readDisabledTaskNames(definition);
}
