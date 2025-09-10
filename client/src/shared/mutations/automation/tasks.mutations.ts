import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query';

export type TaskType = {
    id: string;
    name: string;
    description: string;
    createdBy: string;
    createdDate: string;
    lastModifiedBy: string;
    lastModifiedDate: string;
    version: number;
};

export type TaskInputType = {
    id?: string;
    name: string;
    description?: string;
    version?: number;
};

export const TaskKeys = {
    task: (id: string) => [...TaskKeys.tasks, id],
    tasks: ['tasks'] as const,
};

interface CreateTaskMutationProps {
    onSuccess?: (result: TaskType, variables: TaskInputType) => void;
    onError?: (error: Error, variables: TaskInputType) => void;
}

export const useCreateTaskMutation = (mutationProps?: CreateTaskMutationProps) => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (task: TaskInputType) => {
            const response = await fetch('/graphql', {
                body: JSON.stringify({
                    query: `
                        mutation createTask($task: TaskInput!) {
                            createTask(task: $task) {
                                id
                                name
                                description
                            }
                        }
                    `,
                    variables: {task},
                }),
                headers: {
                    'Content-Type': 'application/json',
                },
                method: 'POST',
            });

            const json = await response.json();

            if (json.errors) {
                throw new Error(json.errors[0].message);
            }

            return json.data.createTask;
        },
        onError: mutationProps?.onError,
        onSuccess: (data, variables) => {
            queryClient.invalidateQueries({queryKey: TaskKeys.tasks});
            mutationProps?.onSuccess?.(data, variables);
        },
    });
};

interface UpdateTaskMutationProps {
    onSuccess?: (result: TaskType, variables: TaskInputType) => void;
    onError?: (error: Error, variables: TaskInputType) => void;
}

export const useUpdateTaskMutation = (mutationProps?: UpdateTaskMutationProps) => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (task: TaskInputType) => {
            const response = await fetch('/graphql', {
                body: JSON.stringify({
                    query: `
                        mutation updateTask($task: TaskInput!) {
                            updateTask(task: $task) {
                                id
                                name
                                description
                                version
                            }
                        }
                    `,
                    variables: {task},
                }),
                headers: {
                    'Content-Type': 'application/json',
                },
                method: 'POST',
            });

            const json = await response.json();

            if (json.errors) {
                throw new Error(json.errors[0].message);
            }

            return json.data.updateTask;
        },
        onError: mutationProps?.onError,
        onSuccess: (data, variables) => {
            queryClient.invalidateQueries({queryKey: TaskKeys.tasks});
            if (variables.id) {
                queryClient.invalidateQueries({queryKey: TaskKeys.task(variables.id)});
            }
            mutationProps?.onSuccess?.(data, variables);
        },
    });
};

interface DeleteTaskMutationProps {
    onSuccess?: (result: boolean, variables: string) => void;
    onError?: (error: Error, variables: string) => void;
}

export const useDeleteTaskMutation = (mutationProps?: DeleteTaskMutationProps) => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (id: string) => {
            const response = await fetch('/graphql', {
                body: JSON.stringify({
                    query: `
                        mutation deleteTask($id: ID!) {
                            deleteTask(id: $id)
                        }
                    `,
                    variables: {id},
                }),
                headers: {
                    'Content-Type': 'application/json',
                },
                method: 'POST',
            });

            const json = await response.json();

            if (json.errors) {
                throw new Error(json.errors[0].message);
            }

            return json.data.deleteTask;
        },
        onError: mutationProps?.onError,
        onSuccess: (data, variables) => {
            queryClient.invalidateQueries({queryKey: TaskKeys.tasks});
            queryClient.invalidateQueries({queryKey: TaskKeys.task(variables)});
            mutationProps?.onSuccess?.(data, variables);
        },
    });
};

export const useGetTaskQuery = (id: string, enabled?: boolean) =>
    useQuery<TaskType, Error>({
        enabled: enabled === undefined ? true : enabled,
        queryFn: async () => {
            const response = await fetch('/graphql', {
                body: JSON.stringify({
                    query: `
                        query task($id: ID!) {
                            task(id: $id) {
                                id
                                name
                                description
                                createdBy
                                createdDate
                                lastModifiedBy
                                lastModifiedDate
                                version
                            }
                        }
                    `,
                    variables: {id},
                }),
                headers: {
                    'Content-Type': 'application/json',
                },
                method: 'POST',
            });
            const json = await response.json();

            if (json.errors) {
                throw new Error(json.errors[0].message);
            }

            return json.data.task;
        },
        queryKey: TaskKeys.task(id),
    });

export const useGetTasksQuery = () =>
    useQuery<TaskType[], Error>({
        queryFn: async () => {
            const response = await fetch('/graphql', {
                body: JSON.stringify({
                    query: `
                        query {
                            tasks {
                                id
                                name
                                description
                                createdBy
                                createdDate
                                lastModifiedBy
                                lastModifiedDate
                                version
                            }
                        }
                    `,
                }),
                headers: {
                    'Content-Type': 'application/json',
                },
                method: 'POST',
            });
            const json = await response.json();

            if (json.errors) {
                throw new Error(json.errors[0].message);
            }

            return json.data.tasks;
        },
        queryKey: TaskKeys.tasks,
    });
