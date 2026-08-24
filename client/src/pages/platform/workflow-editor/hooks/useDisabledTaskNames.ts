import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import {getEffectivelyDisabledTaskNames} from '../utils/getEffectivelyDisabledTaskNames';

const EMPTY_DISABLED_TASK_NAMES: Set<string> = new Set();

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

export default function useDisabledTaskNames(): Set<string> {
    const definition = useWorkflowDataStore((state) => state.workflow.definition);

    return readDisabledTaskNames(definition);
}
