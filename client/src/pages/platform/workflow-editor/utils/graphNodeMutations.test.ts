import {GraphNodeType} from '@/shared/types';
import {describe, expect, it} from 'vitest';

import extractNextTargets from './extractNextTargets';
import {addGraphNode, deleteGraphNode, renameGraphNode, validateGraphNodeName} from './graphNodeMutations';

function makeNodes(): Array<GraphNodeType> {
    return [
        {name: 'node_0', next: "'node_1'", tasks: []},
        {name: 'node_1', next: undefined, tasks: [{name: 'task_1', type: 'task/v1'}]},
    ];
}

describe('validateGraphNodeName', () => {
    it('should reject an empty name', () => {
        expect(validateGraphNodeName(makeNodes(), 0, '')).toEqual({
            error: 'Node name cannot be empty.',
            valid: false,
        });
    });

    it('should reject a whitespace-only name', () => {
        expect(validateGraphNodeName(makeNodes(), 0, '   ').valid).toBe(false);
    });

    it('should reject a name already used by another node', () => {
        const result = validateGraphNodeName(makeNodes(), 0, 'node_1');

        expect(result.valid).toBe(false);
        expect(result.error).toContain('node_1');
    });

    it('should accept the node keeping its own unchanged name', () => {
        expect(validateGraphNodeName(makeNodes(), 0, 'node_0')).toEqual({valid: true});
    });

    it('should accept a fresh, unique name', () => {
        expect(validateGraphNodeName(makeNodes(), 0, 'entry')).toEqual({valid: true});
    });

    it('should reject the reserved "vars" name', () => {
        expect(validateGraphNodeName([], 0, 'vars')).toEqual({
            error: '"vars" is a reserved name.',
            valid: false,
        });
    });

    it('should accept a name that only starts with "vars"', () => {
        expect(validateGraphNodeName([], 0, 'varsCount')).toEqual({valid: true});
    });
});

describe('renameGraphNode', () => {
    it('should update only the targeted node name', () => {
        const renamed = renameGraphNode(makeNodes(), 0, 'entry');

        expect(renamed[0].name).toBe('entry');
        expect(renamed[1].name).toBe('node_1');
    });

    it('should trim the new name', () => {
        const renamed = renameGraphNode(makeNodes(), 0, '  entry  ');

        expect(renamed[0].name).toBe('entry');
    });

    it('should leave the array unchanged for an out-of-range index', () => {
        const original = makeNodes();

        expect(renameGraphNode(original, 5, 'entry')).toBe(original);
    });

    it("should NOT rewrite another node's next expression that referenced the old name — a rename produces a dangling target instead of a silent rewrite", () => {
        const nodes = makeNodes();

        // node_0.next === "'node_1'" — renaming node_1 must leave that string untouched.
        const renamed = renameGraphNode(nodes, 1, 'entry');

        expect(renamed[0].next).toBe("'node_1'");

        const declaredNodeNamesAfterRename = renamed.map((graphNode) => graphNode.name);

        expect(declaredNodeNamesAfterRename).toEqual(['node_0', 'entry']);

        // The stale literal no longer matches any declared node — it must surface as dangling,
        // not silently disappear or keep resolving.
        const result = extractNextTargets(renamed[0].next, declaredNodeNamesAfterRename);

        expect(result).toEqual({
            dangling: ['node_1'],
            dynamic: false,
            targets: [],
        });
    });
});

describe('addGraphNode', () => {
    it('should append an empty node with a fresh unique name', () => {
        const withAdded = addGraphNode(makeNodes());

        expect(withAdded).toHaveLength(3);
        expect(withAdded[2]).toEqual({name: expect.any(String), tasks: []});
        expect(withAdded[2].name).not.toBe('node_0');
        expect(withAdded[2].name).not.toBe('node_1');
    });

    it('should skip over already-used node_<n> names', () => {
        const withAdded = addGraphNode(makeNodes());

        // makeNodes already declares node_0 and node_1, so the generator must land on node_2.
        expect(withAdded[2].name).toBe('node_2');
    });

    it('should add an empty node to an empty graph', () => {
        const withAdded = addGraphNode([]);

        expect(withAdded).toEqual([{name: 'node_0', tasks: []}]);
    });

    it('should not mutate the input array', () => {
        const original = makeNodes();

        addGraphNode(original);

        expect(original).toHaveLength(2);
    });
});

describe('deleteGraphNode', () => {
    it('should remove the targeted node regardless of whether it holds tasks', () => {
        const nodes = makeNodes();

        expect(deleteGraphNode(nodes, 1).map((graphNode) => graphNode.name)).toEqual(['node_0']);
        expect(deleteGraphNode(nodes, 0).map((graphNode) => graphNode.name)).toEqual(['node_1']);
    });

    it('should allow shrinking a graph down to zero nodes', () => {
        expect(deleteGraphNode([{name: 'only', tasks: []}], 0)).toEqual([]);
    });

    it('should not mutate the input array', () => {
        const original = makeNodes();

        deleteGraphNode(original, 0);

        expect(original).toHaveLength(2);
    });
});
