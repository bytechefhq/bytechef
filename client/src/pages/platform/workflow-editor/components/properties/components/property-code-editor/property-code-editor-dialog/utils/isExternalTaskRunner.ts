const GRAALVM_TASK_RUNNER_TYPE = 'graalvm';

const getTaskRunnerType = (parameters: Record<string, unknown> | undefined): string | undefined => {
    const taskRunner = parameters?.taskRunner;

    if (!taskRunner || typeof taskRunner !== 'object') {
        return undefined;
    }

    const {type} = taskRunner as {type?: unknown};

    return typeof type === 'string' ? type : undefined;
};

export const isExternalTaskRunner = (language: string, parameters: Record<string, unknown> | undefined): boolean => {
    if (language.toLowerCase() === 'shell') {
        return true;
    }

    const taskRunnerType = getTaskRunnerType(parameters);

    return taskRunnerType !== undefined && taskRunnerType !== GRAALVM_TASK_RUNNER_TYPE;
};
