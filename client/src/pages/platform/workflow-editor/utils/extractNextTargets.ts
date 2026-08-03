/**
 * Extracts the set of possible transition targets out of a graph node's `next` expression.
 *
 * `next` is a FORMULA_MODE expression: it may be a bare single-quoted literal (`'nodeB'`), a
 * conditional mixing literals with runtime logic (`steps.decision.value == 'yes' ? 'approve' :
 * 'reject'`), a pure runtime expression with no literal at all (`steps.decision.nextNode`), or
 * blank (terminal — the graph run ends at this node).
 *
 * This is a standalone, side-effect-free util deliberately: phase 3 (rendered transition edges)
 * derives its edges from the same literal/dynamic split this function produces, so its contract
 * is pinned independently of any rendering code.
 *
 * Extraction is POSITIONAL, not purely syntactic: a single-quoted literal counts as a candidate
 * target only when it sits in a RESULT position AND spans the ENTIRE result branch — either the
 * whole trimmed expression is a bare literal, or the literal is directly preceded (ignoring
 * whitespace) by `?` or `:` AND directly followed (ignoring whitespace, and any run of closing
 * parens each themselves followed only by whitespace) by `:` or the end of the expression. That
 * combined rule covers ternary true/false branches (including nested ternaries, since the nested
 * `?`/`:` are themselves directly adjacent to their own branch literals, and a paren-wrapped
 * nested ternary's closing paren still counts as reaching the branch boundary) and the Elvis `?:`
 * operator's result branch (its literal is directly preceded by the `:` half of `?:`) without any
 * special casing. A literal that is a comparison operand (`== 'yes'`), a leading operand of a
 * compound result branch (`'a' + suffix`), or any other non-result or partial-result position is
 * never a candidate — see the phase-3 spec decision
 * (docs/superpowers/specs/2026-08-02-graph-task-dispatcher-design.md, "Implementation-forced
 * decisions (Phase 2, 2026-08-03)"): phase 2 shipped this extraction reading comparison operands
 * as dangling targets, pinned that as a known limitation, and phase 3 tightens extraction to
 * ternary-result positions only, flipping that limitation test deliberately. A leading literal in
 * an otherwise-compound result branch (`cond ? 'a' + suffix : 'b'`) contributes no target at all
 * (not even dangling) and the expression's `dynamic` flag still covers it, since the branch as a
 * whole can only be known at runtime.
 *
 * - `targets`: result-position literals that match a currently declared node name.
 * - `dangling`: result-position literals that match NO declared node name (a rename elsewhere, or
 *   a typo, left this expression pointing at nothing — phase 2 does not auto-repair these, see
 *   the graph node rename decision).
 * - `dynamic`: true whenever the expression is anything other than a single bare literal (a
 *   ternary, a variable reference, a function call, ...) — i.e. the actual target can only be
 *   known at runtime, even if some of its literal branches are visible statically.
 */

const SINGLE_QUOTED_LITERAL_PATTERN = /'((?:[^'\\]|\\.)*)'/g;
const PURE_SINGLE_LITERAL_PATTERN = /^'(?:[^'\\]|\\.)*'$/;

export interface ExtractNextTargetsResultI {
    dangling: string[];
    dynamic: boolean;
    targets: string[];
}

/**
 * Whether the literal ending at `literalEndIndex` reaches a branch boundary rather than
 * continuing into a compound expression (`'a' + suffix`): skipping whitespace and any run of
 * closing parens (each of which may itself be followed only by whitespace before the next check),
 * the next meaningful character must be `:` or the end of the expression. A run of closing parens
 * is consumed rather than just one, so a doubly-parenthesized branch still resolves correctly.
 */
function reachesResultBranchBoundary(expression: string, literalEndIndex: number): boolean {
    let index = literalEndIndex;

    while (true) {
        while (index < expression.length && /\s/.test(expression[index])) {
            index += 1;
        }

        if (index >= expression.length || expression[index] === ':') {
            return true;
        }

        if (expression[index] !== ')') {
            return false;
        }

        index += 1;
    }
}

/**
 * A single-quoted literal is in a RESULT position when it either is the entire expression, or it
 * spans the WHOLE result branch: the nearest non-whitespace character before it is `?` or `:`,
 * AND (per `reachesResultBranchBoundary`) the nearest meaningful content after it is `:` or the
 * end of the expression. This covers ternary branches, nested ternary branches (including
 * paren-wrapped ones), and the Elvis `?:` operator's result branch in one local check, with no
 * need to track ternary nesting depth explicitly — while excluding a literal that is merely the
 * LEADING operand of a compound result branch (`cond ? 'a' + suffix : 'b'`), which the preceding
 * `?` check alone would otherwise wrongly admit.
 */
function isResultPositionLiteral(expression: string, literalStartIndex: number, literalEndIndex: number): boolean {
    let precedingIndex = literalStartIndex - 1;

    while (precedingIndex >= 0 && /\s/.test(expression[precedingIndex])) {
        precedingIndex -= 1;
    }

    if (precedingIndex < 0) {
        return false;
    }

    const precedingCharacter = expression[precedingIndex];

    if (precedingCharacter !== '?' && precedingCharacter !== ':') {
        return false;
    }

    return reachesResultBranchBoundary(expression, literalEndIndex);
}

export default function extractNextTargets(
    nextExpression: string | null | undefined,
    declaredNodeNames: Array<string>
): ExtractNextTargetsResultI {
    const trimmedExpression = (nextExpression ?? '').trim();

    if (!trimmedExpression) {
        return {dangling: [], dynamic: false, targets: []};
    }

    const declaredNodeNameSet = new Set(declaredNodeNames);
    const isWholeExpressionLiteral = PURE_SINGLE_LITERAL_PATTERN.test(trimmedExpression);
    const resultPositionLiterals: string[] = [];

    SINGLE_QUOTED_LITERAL_PATTERN.lastIndex = 0;

    let match = SINGLE_QUOTED_LITERAL_PATTERN.exec(trimmedExpression);

    while (match !== null) {
        const literalEndIndex = match.index + match[0].length;

        if (isWholeExpressionLiteral || isResultPositionLiteral(trimmedExpression, match.index, literalEndIndex)) {
            resultPositionLiterals.push(match[1]);
        }

        match = SINGLE_QUOTED_LITERAL_PATTERN.exec(trimmedExpression);
    }

    const uniqueResultPositionLiterals = Array.from(new Set(resultPositionLiterals));

    return {
        dangling: uniqueResultPositionLiterals.filter((literal) => !declaredNodeNameSet.has(literal)),
        dynamic: !isWholeExpressionLiteral,
        targets: uniqueResultPositionLiterals.filter((literal) => declaredNodeNameSet.has(literal)),
    };
}
