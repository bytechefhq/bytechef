import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import {calculateNodeInsertIndexFromTasks} from '../calculateNodeInsertIndex';

describe('calculateNodeInsertIndex', () => {
    it('should return correct index for simple linear tasks', () => {
        const tasks = [
            {name: 'slack_1', type: 'slack/v1/sendMessage'},
            {name: 'http_1', type: 'http/v1/get'},
            {name: 'email_1', type: 'email/v1/send'},
        ] as WorkflowTask[];

        expect(calculateNodeInsertIndexFromTasks('http_1', tasks)).toBe(1);
    });

    it('should subtract map iteratee subtasks from the index', () => {
        const tasks = [
            {
                name: 'map_1',
                parameters: {iteratee: [{name: 'slack_1'}, {name: 'http_1'}]},
                type: 'map/v1/map',
            },
            {name: 'slack_1', type: 'slack/v1/sendMessage'},
            {name: 'http_1', type: 'http/v1/get'},
            {name: 'email_1', type: 'email/v1/send'},
        ] as WorkflowTask[];

        // nextTaskIndex of email_1 = 3
        // tasksInMaps = 2 (slack_1, http_1 in map iteratee)
        // result = 3 - 2 = 1
        expect(calculateNodeInsertIndexFromTasks('email_1', tasks)).toBe(1);
    });

    it('should subtract loop iteratee subtasks from the index', () => {
        const tasks = [
            {
                name: 'loop_1',
                parameters: {iteratee: [{name: 'slack_1'}]},
                type: 'loop/v1/loop',
            },
            {name: 'slack_1', type: 'slack/v1/sendMessage'},
            {name: 'email_1', type: 'email/v1/send'},
        ] as WorkflowTask[];

        expect(calculateNodeInsertIndexFromTasks('email_1', tasks)).toBe(1);
    });

    it('should subtract condition subtasks from the index', () => {
        const tasks = [
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
        ] as WorkflowTask[];

        // nextTaskIndex of email_1 = 3
        // tasksInConditions = 2 (1 caseTrue + 1 caseFalse)
        // result = 3 - 2 = 1
        expect(calculateNodeInsertIndexFromTasks('email_1', tasks)).toBe(1);
    });

    it('should handle multiple task dispatchers before the target', () => {
        const tasks = [
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
        ] as WorkflowTask[];

        // nextTaskIndex of email_1 = 4
        // tasksInMaps = 1 (slack_1)
        // tasksInLoops = 1 (http_1)
        // result = 4 - 1 - 1 = 2
        expect(calculateNodeInsertIndexFromTasks('email_1', tasks)).toBe(2);
    });

    it('should not subtract subtasks from dispatchers after the target', () => {
        const tasks = [
            {name: 'slack_1', type: 'slack/v1/sendMessage'},
            {name: 'email_1', type: 'email/v1/send'},
            {
                name: 'map_1',
                parameters: {iteratee: [{name: 'http_1'}]},
                type: 'map/v1/map',
            },
            {name: 'http_1', type: 'http/v1/get'},
        ] as WorkflowTask[];

        // nextTaskIndex of email_1 = 1
        // map_1 is after email_1, so its subtasks are not counted
        // result = 1
        expect(calculateNodeInsertIndexFromTasks('email_1', tasks)).toBe(1);
    });
});
