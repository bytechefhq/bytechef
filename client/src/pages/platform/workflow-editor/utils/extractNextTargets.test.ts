import {describe, expect, it} from 'vitest';

import extractNextTargets from './extractNextTargets';

describe('extractNextTargets', () => {
    const declaredNodeNames = ['nodeA', 'nodeB', 'approve', 'reject', 'fallback'];

    it('should treat an undefined expression as terminal', () => {
        expect(extractNextTargets(undefined, declaredNodeNames)).toEqual({
            dangling: [],
            dynamic: false,
            targets: [],
        });
    });

    it('should treat a null expression as terminal', () => {
        expect(extractNextTargets(null, declaredNodeNames)).toEqual({
            dangling: [],
            dynamic: false,
            targets: [],
        });
    });

    it('should treat an empty string expression as terminal', () => {
        expect(extractNextTargets('', declaredNodeNames)).toEqual({
            dangling: [],
            dynamic: false,
            targets: [],
        });
    });

    it('should treat a whitespace-only expression as terminal', () => {
        expect(extractNextTargets('   ', declaredNodeNames)).toEqual({
            dangling: [],
            dynamic: false,
            targets: [],
        });
    });

    it('should resolve a bare single-quoted literal matching a declared node as a static target', () => {
        expect(extractNextTargets("'nodeB'", declaredNodeNames)).toEqual({
            dangling: [],
            dynamic: false,
            targets: ['nodeB'],
        });
    });

    it('should tolerate surrounding whitespace around a bare literal', () => {
        expect(extractNextTargets("  'nodeB'  ", declaredNodeNames)).toEqual({
            dangling: [],
            dynamic: false,
            targets: ['nodeB'],
        });
    });

    it('should mark a bare literal matching no declared node as dangling, not dynamic', () => {
        expect(extractNextTargets("'missingNode'", declaredNodeNames)).toEqual({
            dangling: ['missingNode'],
            dynamic: false,
            targets: [],
        });
    });

    // Phase-3 spec decision (docs/superpowers/specs/2026-08-02-graph-task-dispatcher-design.md,
    // "Implementation-forced decisions (Phase 2, 2026-08-03)"): phase 2 shipped extraction that
    // read EVERY single-quoted literal in the expression, so a comparison operand like 'yes'
    // here was mis-classified as a dangling target even though it isn't a node reference at all.
    // That was pinned as a known, deliberate limitation in the phase-2 test suite. Phase 3
    // tightens extraction to ternary-RESULT positions only (directly after `?` or `:`), so the
    // comparison operand no longer surfaces as a target OR as dangling — this test flips to pin
    // the corrected behavior.
    it('should split a ternary between literal targets and ignore a comparison operand entirely', () => {
        const result = extractNextTargets("steps.decision.value == 'yes' ? 'approve' : 'reject'", declaredNodeNames);

        expect(result.dynamic).toBe(true);
        expect(result.targets.sort()).toEqual(['approve', 'reject']);
        expect(result.dangling).toEqual([]);
    });

    it('should surface a dangling literal inside a ternary result position alongside a valid target', () => {
        const result = extractNextTargets(
            "steps.decision.value == 'yes' ? 'approve' : 'missingNode'",
            declaredNodeNames
        );

        expect(result.dynamic).toBe(true);
        expect(result.targets).toEqual(['approve']);
        expect(result.dangling).toEqual(['missingNode']);
    });

    it('should mark a pure runtime expression with no literal at all as dynamic with no targets', () => {
        expect(extractNextTargets('steps.decision.nextNode', declaredNodeNames)).toEqual({
            dangling: [],
            dynamic: true,
            targets: [],
        });
    });

    it('should not treat a double-quoted string as a node-name literal', () => {
        const result = extractNextTargets('"nodeA"', declaredNodeNames);

        expect(result.targets).toEqual([]);
        expect(result.dangling).toEqual([]);
        expect(result.dynamic).toBe(true);
    });

    it('should de-duplicate a literal referenced more than once', () => {
        const result = extractNextTargets("condition ? 'nodeA' : 'nodeA'", declaredNodeNames);

        expect(result.targets).toEqual(['nodeA']);
        expect(result.dynamic).toBe(true);
    });

    it('should not crash on an escaped quote inside a literal', () => {
        expect(() => extractNextTargets("'it\\'s complicated'", declaredNodeNames)).not.toThrow();
    });

    it('should ignore literal operands of a concatenation, since neither sits in a result position', () => {
        // Not the whole expression, and neither literal is directly preceded by `?` or `:` — under
        // the tightened, position-based extraction neither counts as a target, even though both
        // are declared node names and the phase-2 syntactic reading would have surfaced them.
        const result = extractNextTargets("'node' + 'A'", ['nodeA', 'node', 'A']);

        expect(result.dynamic).toBe(true);
        expect(result.targets).toEqual([]);
        expect(result.dangling).toEqual([]);
    });

    it('should resolve every branch of a nested ternary in the else position recursively', () => {
        const result = extractNextTargets(
            "steps.a.value == 'x' ? 'approve' : steps.b.value == 'y' ? 'reject' : 'fallback'",
            declaredNodeNames
        );

        expect(result.dynamic).toBe(true);
        expect(result.targets.sort()).toEqual(['approve', 'fallback', 'reject']);
        expect(result.dangling).toEqual([]);
    });

    it('should resolve every branch of a parenthesized nested ternary in the true position', () => {
        const result = extractNextTargets(
            "steps.a.value == 'x' ? (steps.b.value == 'y' ? 'approve' : 'reject') : 'fallback'",
            declaredNodeNames
        );

        expect(result.dynamic).toBe(true);
        expect(result.targets.sort()).toEqual(['approve', 'fallback', 'reject']);
        expect(result.dangling).toEqual([]);
    });

    it('should resolve the result branch of an Elvis expression but not its left-hand operand', () => {
        const result = extractNextTargets("steps.decision.nextNode ?: 'fallback'", declaredNodeNames);

        expect(result.dynamic).toBe(true);
        expect(result.targets).toEqual(['fallback']);
        expect(result.dangling).toEqual([]);
    });

    it('should not treat a bare literal to the left of an Elvis operator as a target', () => {
        const result = extractNextTargets("'approve' ?: 'fallback'", declaredNodeNames);

        expect(result.dynamic).toBe(true);
        expect(result.targets).toEqual(['fallback']);
        expect(result.dangling).toEqual([]);
    });

    // A leading literal that only starts a compound result branch (concatenation, here) must not
    // leak through as a target just because it is directly preceded by `?` — the branch as a
    // whole is `'approve' + suffix`, not the bare literal `'approve'`, so it can only resolve at
    // runtime. Excluded means excluded from BOTH targets and dangling, not just targets.
    it('should ignore a leading literal that only starts a compound true-branch, contributing neither a target nor a dangling entry', () => {
        const result = extractNextTargets("condition ? 'approve' + suffix : 'reject'", declaredNodeNames);

        expect(result.dynamic).toBe(true);
        expect(result.targets).toEqual(['reject']);
        expect(result.dangling).toEqual([]);
    });

    it('should ignore a leading literal that only starts a compound false-branch', () => {
        const result = extractNextTargets("condition ? 'approve' : 'reject' + suffix", declaredNodeNames);

        expect(result.dynamic).toBe(true);
        expect(result.targets).toEqual(['approve']);
        expect(result.dangling).toEqual([]);
    });

    it('should not surface a compound-branch leading literal as dangling even when it matches no declared node', () => {
        const result = extractNextTargets("condition ? 'missingNode' + suffix : 'reject'", declaredNodeNames);

        expect(result.dynamic).toBe(true);
        expect(result.targets).toEqual(['reject']);
        expect(result.dangling).toEqual([]);
    });
});
