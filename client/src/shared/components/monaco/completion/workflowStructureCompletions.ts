import type {CompletionItemDraftType} from './scriptCompletions';

type StructureKeyType = {
    description: string;
    name: string;
};

type StructureLevelType = 'input' | 'output' | 'root' | 'task' | 'trigger';

const INDEX = /^\d+$/;

const NESTED_TASK_LISTS = new Set(['finalize', 'post', 'pre']);

const BOOLEAN_KEYS: Partial<Record<StructureLevelType, string[]>> = {
    input: ['required'],
    task: ['disabled'],
    trigger: ['disabled'],
};

const WORKFLOW_INPUT_TYPES = ['boolean', 'date', 'date_time', 'integer', 'number', 'string', 'time'];

const STRUCTURE_KEYS: Record<StructureLevelType, StructureKeyType[]> = {
    input: [
        {description: 'The input name, used to read its value.', name: 'name'},
        {description: 'The label shown for the input.', name: 'label'},
        {description: 'The type of the input value.', name: 'type'},
        {description: 'Whether a value must be provided.', name: 'required'},
    ],
    output: [
        {description: 'The output name.', name: 'name'},
        {description: 'The output value, usually an expression such as ${task_1.result}.', name: 'value'},
    ],
    root: [
        {description: 'The workflow name shown in the editor.', name: 'label'},
        {description: 'What the workflow does.', name: 'description'},
        {description: 'The values the workflow is started with.', name: 'inputs'},
        {description: 'The values the workflow returns.', name: 'outputs'},
        {description: 'What starts the workflow.', name: 'triggers'},
        {description: 'The steps the workflow runs, in order.', name: 'tasks'},
        {description: 'How many times a failed task is retried.', name: 'maxRetries'},
    ],
    task: [
        {description: 'The unique task name, used in ${...} references.', name: 'name'},
        {description: 'The label shown in the editor.', name: 'label'},
        {description: 'What the task does.', name: 'description'},
        {description: 'The component and action, for example httpClient/v1/get.', name: 'type'},
        {description: 'The action parameters.', name: 'parameters'},
        {description: 'How many times the task is retried when it fails.', name: 'maxRetries'},
        {description: 'How long the task may run, for example 1m.', name: 'timeout'},
        {description: 'Skip the task when the workflow runs.', name: 'disabled'},
        {description: 'Tasks that run before this task.', name: 'pre'},
        {description: 'Tasks that run after this task.', name: 'post'},
        {description: 'Tasks that run after this task, even when it fails.', name: 'finalize'},
        {description: 'The worker node that runs the task.', name: 'node'},
        {description: 'Editor settings for the task.', name: 'metadata'},
    ],
    trigger: [
        {description: 'The unique trigger name, used in ${...} references.', name: 'name'},
        {description: 'The label shown in the editor.', name: 'label'},
        {description: 'What the trigger does.', name: 'description'},
        {description: 'The component and trigger, for example manual/v1/manual.', name: 'type'},
        {description: 'The trigger parameters.', name: 'parameters'},
        {description: 'How long the trigger may run, for example 1m.', name: 'timeout'},
        {description: 'Keep the trigger from starting the workflow.', name: 'disabled'},
        {description: 'Editor settings for the trigger.', name: 'metadata'},
    ],
};

function isTaskPath(containerPath: string[]): boolean {
    if (containerPath.length < 2 || containerPath.length % 2 !== 0 || containerPath[0] !== 'tasks') {
        return false;
    }

    return containerPath.every((segment, index) => {
        if (index % 2 === 1) {
            return INDEX.test(segment);
        }

        return index === 0 || NESTED_TASK_LISTS.has(segment);
    });
}

function structureLevelOf(containerPath: string[]): StructureLevelType | undefined {
    if (containerPath.length === 0) {
        return 'root';
    }

    if (isTaskPath(containerPath)) {
        return 'task';
    }

    if (containerPath.length !== 2 || !INDEX.test(containerPath[1])) {
        return undefined;
    }

    switch (containerPath[0]) {
        case 'inputs':
            return 'input';
        case 'outputs':
            return 'output';
        case 'triggers':
            return 'trigger';
        default:
            return undefined;
    }
}

export function getStructureKeyCompletions(
    containerPath: string[],
    existingKeys: string[]
): CompletionItemDraftType[] | undefined {
    const level = structureLevelOf(containerPath);

    if (!level) {
        return undefined;
    }

    return STRUCTURE_KEYS[level]
        .filter((structureKey) => !existingKeys.includes(structureKey.name))
        .map((structureKey, index) => ({
            documentation: structureKey.description,
            insertAsSnippet: false,
            insertText: structureKey.name,
            kind: 'property',
            label: structureKey.name,
            sortText: String(index).padStart(2, '0'),
        }));
}

export function getStructureValueCompletions(
    path: string[],
    replaceLength: number,
    toInsertText: (value: unknown) => string | undefined
): CompletionItemDraftType[] | undefined {
    const key = path[path.length - 1];
    const level = structureLevelOf(path.slice(0, -1));

    if (!level || key === undefined) {
        return undefined;
    }

    let values: unknown[] | undefined;

    if (level === 'input' && key === 'type') {
        values = WORKFLOW_INPUT_TYPES;
    } else if (BOOLEAN_KEYS[level]?.includes(key)) {
        values = [true, false];
    }

    if (!values) {
        return undefined;
    }

    return values.flatMap((value) => {
        const insertText = toInsertText(value);

        return insertText === undefined
            ? []
            : [{insertAsSnippet: false, insertText, kind: 'value' as const, label: String(value), replaceLength}];
    });
}
