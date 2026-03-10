import {describe, expect, it} from 'vitest';

import extractDefinitionPositions from './extractDefinitionPositions';

function makeDefinition(overrides: Record<string, unknown> = {}): string {
    return JSON.stringify({
        tasks: [],
        triggers: [],
        ...overrides,
    });
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function makeTask(name: string, nodePosition?: {x: number; y: number}, parameters?: Record<string, any>) {
    return {
        metadata: nodePosition ? {ui: {nodePosition}} : undefined,
        name,
        parameters,
        type: `test/${name}`,
    };
}

describe('extractDefinitionPositions', () => {
    it('should return empty map for definition with no positions', () => {
        const definition = makeDefinition({
            tasks: [makeTask('task_1')],
        });

        const result = extractDefinitionPositions(definition);

        expect(result.size).toBe(0);
    });

    it('should extract trigger position', () => {
        const definition = makeDefinition({
            triggers: [{metadata: {ui: {nodePosition: {x: 100, y: 200}}}, name: 'trigger_1', type: 'test/trigger'}],
        });

        const result = extractDefinitionPositions(definition);

        expect(result.get('trigger_1')).toEqual({x: 100, y: 200});
    });

    it('should extract top-level task positions', () => {
        const definition = makeDefinition({
            tasks: [makeTask('task_1', {x: 100, y: 200}), makeTask('task_2', {x: 300, y: 400})],
        });

        const result = extractDefinitionPositions(definition);

        expect(result.get('task_1')).toEqual({x: 100, y: 200});
        expect(result.get('task_2')).toEqual({x: 300, y: 400});
    });

    it('should extract positions from condition caseTrue and caseFalse', () => {
        const definition = makeDefinition({
            tasks: [
                makeTask(
                    'condition_1',
                    {x: 50, y: 50},
                    {
                        caseFalse: [makeTask('false_child', {x: 300, y: 400})],
                        caseTrue: [makeTask('true_child', {x: 100, y: 200})],
                    }
                ),
            ],
        });

        const result = extractDefinitionPositions(definition);

        expect(result.get('condition_1')).toEqual({x: 50, y: 50});
        expect(result.get('true_child')).toEqual({x: 100, y: 200});
        expect(result.get('false_child')).toEqual({x: 300, y: 400});
    });

    it('should extract positions from loop iteratee', () => {
        const definition = makeDefinition({
            tasks: [
                makeTask(
                    'loop_1',
                    {x: 10, y: 20},
                    {
                        iteratee: [makeTask('child_1', {x: 100, y: 200})],
                    }
                ),
            ],
        });

        const result = extractDefinitionPositions(definition);

        expect(result.get('loop_1')).toEqual({x: 10, y: 20});
        expect(result.get('child_1')).toEqual({x: 100, y: 200});
    });

    it('should extract positions from branch cases and default', () => {
        const definition = makeDefinition({
            tasks: [
                makeTask(
                    'branch_1',
                    {x: 10, y: 10},
                    {
                        cases: [{key: 'caseA', tasks: [makeTask('case_child', {x: 100, y: 100})], value: 'A'}],
                        default: [makeTask('default_child', {x: 200, y: 200})],
                    }
                ),
            ],
        });

        const result = extractDefinitionPositions(definition);

        expect(result.get('branch_1')).toEqual({x: 10, y: 10});
        expect(result.get('case_child')).toEqual({x: 100, y: 100});
        expect(result.get('default_child')).toEqual({x: 200, y: 200});
    });

    it('should extract positions from parallel tasks', () => {
        const definition = makeDefinition({
            tasks: [
                makeTask(
                    'parallel_1',
                    {x: 10, y: 10},
                    {
                        tasks: [makeTask('par_child', {x: 100, y: 100})],
                    }
                ),
            ],
        });

        const result = extractDefinitionPositions(definition);

        expect(result.get('parallel_1')).toEqual({x: 10, y: 10});
        expect(result.get('par_child')).toEqual({x: 100, y: 100});
    });

    it('should extract positions from fork-join branches', () => {
        const definition = makeDefinition({
            tasks: [
                makeTask(
                    'forkJoin_1',
                    {x: 10, y: 10},
                    {
                        branches: [[makeTask('branch_a', {x: 100, y: 100})], [makeTask('branch_b', {x: 200, y: 200})]],
                    }
                ),
            ],
        });

        const result = extractDefinitionPositions(definition);

        expect(result.get('forkJoin_1')).toEqual({x: 10, y: 10});
        expect(result.get('branch_a')).toEqual({x: 100, y: 100});
        expect(result.get('branch_b')).toEqual({x: 200, y: 200});
    });

    it('should extract positions from deeply nested dispatchers', () => {
        const definition = makeDefinition({
            tasks: [
                makeTask(
                    'condition_1',
                    {x: 50, y: 50},
                    {
                        caseTrue: [
                            makeTask(
                                'each_1',
                                {x: 100, y: 100},
                                {
                                    iteratee: [makeTask('grandchild', {x: 200, y: 200})],
                                }
                            ),
                        ],
                    }
                ),
            ],
        });

        const result = extractDefinitionPositions(definition);

        expect(result.get('condition_1')).toEqual({x: 50, y: 50});
        expect(result.get('each_1')).toEqual({x: 100, y: 100});
        expect(result.get('grandchild')).toEqual({x: 200, y: 200});
    });

    it('should skip tasks without positions', () => {
        const definition = makeDefinition({
            tasks: [
                makeTask('task_1', {x: 100, y: 200}),
                makeTask('task_2'), // no position
            ],
        });

        const result = extractDefinitionPositions(definition);

        expect(result.get('task_1')).toEqual({x: 100, y: 200});
        expect(result.has('task_2')).toBe(false);
    });

    it('should return empty map for invalid JSON', () => {
        const result = extractDefinitionPositions('not valid json');

        expect(result.size).toBe(0);
    });

    it('should return empty map for empty definition', () => {
        const result = extractDefinitionPositions('{}');

        expect(result.size).toBe(0);
    });

    it('should handle trigger without position', () => {
        const definition = makeDefinition({
            triggers: [{name: 'trigger_1', type: 'test/trigger'}],
        });

        const result = extractDefinitionPositions(definition);

        expect(result.has('trigger_1')).toBe(false);
    });

    it('should extract both trigger and task positions', () => {
        const definition = makeDefinition({
            tasks: [makeTask('task_1', {x: 300, y: 400})],
            triggers: [{metadata: {ui: {nodePosition: {x: 100, y: 200}}}, name: 'trigger_1', type: 'test/trigger'}],
        });

        const result = extractDefinitionPositions(definition);

        expect(result.get('trigger_1')).toEqual({x: 100, y: 200});
        expect(result.get('task_1')).toEqual({x: 300, y: 400});
    });
});
