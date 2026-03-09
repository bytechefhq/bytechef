import {describe, expect, it} from 'vitest';

/**
 * Replicates the index calculation logic from calculateNodeInsertIndex.ts.
 *
 * The function computes the correct insertion position in the flat tasks array
 * by subtracting subtasks of all task dispatchers that appear before the target.
 */

interface TaskI {
    name: string;
    parameters?: Record<string, unknown>;
    type: string;
}

function calculateNodeInsertIndex(targetId: string, tasks: TaskI[]): number {
    const nextTaskIndex = tasks.findIndex((task) => task.name === targetId);

    const conditionTasks = tasks.slice(0, nextTaskIndex).filter((task) => task.type.includes('condition/'));
    const loopTasks = tasks.slice(0, nextTaskIndex).filter((task) => task.type.includes('loop/'));
    const branchTasks = tasks.slice(0, nextTaskIndex).filter((task) => task.type.includes('branch/'));
    const eachTasks = tasks.slice(0, nextTaskIndex).filter((task) => task.type.includes('each/'));
    const forkJoinTasks = tasks.slice(0, nextTaskIndex).filter((task) => task.type.includes('fork-join/'));
    const mapTasks = tasks.slice(0, nextTaskIndex).filter((task) => task.type.includes('map/'));

    const tasksInConditions = conditionTasks.reduce((count, conditionTask) => {
        const caseTrueTasks = (conditionTask.parameters?.caseTrue as unknown[])?.length || 0;
        const caseFalseTasks = (conditionTask.parameters?.caseFalse as unknown[])?.length || 0;

        return count + caseTrueTasks + caseFalseTasks;
    }, 0);

    const tasksInLoops = loopTasks.reduce(
        (count, loopTask) => count + ((loopTask.parameters?.iteratee as unknown[])?.length || 0),
        0
    );

    const tasksInBranches = branchTasks.reduce((count, branchTask) => {
        const defaultTasks = (branchTask.parameters?.default as unknown[])?.length || 0;

        const caseTasks = ((branchTask.parameters?.cases as {tasks?: unknown[]}[]) || []).reduce(
            (caseCount, caseItem) => caseCount + (caseItem.tasks?.length || 0),
            0
        );

        return count + defaultTasks + caseTasks;
    }, 0);

    const tasksInEach = eachTasks.reduce((count, eachTask) => {
        if (eachTask.parameters?.iteratee) {
            count += 1;
        }

        return count;
    }, 0);

    const tasksInForkJoins = forkJoinTasks.reduce(
        (count, forkJoinTask) => count + ((forkJoinTask.parameters?.branches as unknown[][])?.flat().length || 0),
        0
    );

    const tasksInMaps = mapTasks.reduce(
        (count, mapTask) => count + ((mapTask.parameters?.iteratee as unknown[])?.length || 0),
        0
    );

    return (
        nextTaskIndex -
        tasksInConditions -
        tasksInLoops -
        tasksInBranches -
        tasksInEach -
        tasksInForkJoins -
        tasksInMaps
    );
}

describe('calculateNodeInsertIndex', () => {
    it('should return correct index for simple linear tasks', () => {
        const tasks: TaskI[] = [
            {name: 'slack_1', type: 'slack/v1/sendMessage'},
            {name: 'http_1', type: 'http/v1/get'},
            {name: 'email_1', type: 'email/v1/send'},
        ];

        expect(calculateNodeInsertIndex('http_1', tasks)).toBe(1);
    });

    it('should subtract map iteratee subtasks from the index', () => {
        const tasks: TaskI[] = [
            {
                name: 'map_1',
                parameters: {iteratee: [{name: 'slack_1'}, {name: 'http_1'}]},
                type: 'map/v1/map',
            },
            {name: 'slack_1', type: 'slack/v1/sendMessage'},
            {name: 'http_1', type: 'http/v1/get'},
            {name: 'email_1', type: 'email/v1/send'},
        ];

        // nextTaskIndex of email_1 = 3
        // tasksInMaps = 2 (slack_1, http_1 in map iteratee)
        // result = 3 - 2 = 1
        expect(calculateNodeInsertIndex('email_1', tasks)).toBe(1);
    });

    it('should subtract loop iteratee subtasks from the index', () => {
        const tasks: TaskI[] = [
            {
                name: 'loop_1',
                parameters: {iteratee: [{name: 'slack_1'}]},
                type: 'loop/v1/loop',
            },
            {name: 'slack_1', type: 'slack/v1/sendMessage'},
            {name: 'email_1', type: 'email/v1/send'},
        ];

        expect(calculateNodeInsertIndex('email_1', tasks)).toBe(1);
    });

    it('should subtract condition subtasks from the index', () => {
        const tasks: TaskI[] = [
            {
                name: 'condition_1',
                parameters: {
                    caseFalse: [{name: 'http_1'}],
                    caseTrue: [{name: 'slack_1'}],
                },
                type: 'condition/v1/condition',
            },
            {name: 'slack_1', type: 'slack/v1/sendMessage'},
            {name: 'http_1', type: 'http/v1/get'},
            {name: 'email_1', type: 'email/v1/send'},
        ];

        // nextTaskIndex of email_1 = 3
        // tasksInConditions = 2 (1 caseTrue + 1 caseFalse)
        // result = 3 - 2 = 1
        expect(calculateNodeInsertIndex('email_1', tasks)).toBe(1);
    });

    it('should handle multiple task dispatchers before the target', () => {
        const tasks: TaskI[] = [
            {
                name: 'map_1',
                parameters: {iteratee: [{name: 'slack_1'}]},
                type: 'map/v1/map',
            },
            {name: 'slack_1', type: 'slack/v1/sendMessage'},
            {
                name: 'loop_1',
                parameters: {iteratee: [{name: 'http_1'}]},
                type: 'loop/v1/loop',
            },
            {name: 'http_1', type: 'http/v1/get'},
            {name: 'email_1', type: 'email/v1/send'},
        ];

        // nextTaskIndex of email_1 = 4
        // tasksInMaps = 1 (slack_1)
        // tasksInLoops = 1 (http_1)
        // result = 4 - 1 - 1 = 2
        expect(calculateNodeInsertIndex('email_1', tasks)).toBe(2);
    });

    it('should not subtract subtasks from dispatchers after the target', () => {
        const tasks: TaskI[] = [
            {name: 'slack_1', type: 'slack/v1/sendMessage'},
            {name: 'email_1', type: 'email/v1/send'},
            {
                name: 'map_1',
                parameters: {iteratee: [{name: 'http_1'}]},
                type: 'map/v1/map',
            },
            {name: 'http_1', type: 'http/v1/get'},
        ];

        // nextTaskIndex of email_1 = 1
        // map_1 is after email_1, so its subtasks are not counted
        // result = 1
        expect(calculateNodeInsertIndex('email_1', tasks)).toBe(1);
    });
});
