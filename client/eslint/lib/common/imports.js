function getLeadingComments(node, context) {
    const code = context.getSourceCode();

    const comments = code.getCommentsBefore(node);

    const leadingComments = [];

    let successor = node;

    for (let i = comments.length - 1; i >= 0; i--) {
        const comment = comments[i];

        if (isHeaderComment(comment)) {
            break;
        }

        // To be considered preceding, must be the last (closest)
        // comment, or immediately adjacent to the comments that follow
        // (no blank lines in between).
        //
        //      // I'm not considered a leading comment.
        //
        //      // But I am.
        //
        //      something();

        const precedes =
            i === comments.length - 1 ||
            comment.loc.end.line === successor.loc.start.line - 1 ||
            comment.loc.end.line === successor.loc.start.line;

        if (!precedes) {
            break;
        }

        // In order to be considered "leading", comments must not be "trailing"
        // anything else; eg.
        //
        //      something(); // I'm a trailing comment.

        const tokenBefore = context.getSourceCode().getTokenBefore(comment, {
            includeComments: true,
        });

        const trailing = tokenBefore && tokenBefore.loc.end.line === comment.loc.start.line;

        if (!trailing) {
            leadingComments.unshift(comment);

            successor = comment;
        } else {
            break;
        }
    }

    return leadingComments;
}

/**
 * See also `isRequireStatement()` in this module.
 */
function getRequireStatement(node) {
    if (node.callee.type === 'Identifier' && node.callee.name === 'require') {
        const argument = node.arguments && node.arguments[0];

        if (argument && argument.type === 'Literal' && typeof argument.value === 'string') {
            if (
                node.parent.type === 'CallExpression' &&
                node.parent.parent.type === 'VariableDeclarator' &&
                node.parent.parent.parent.type === 'VariableDeclaration'
            ) {
                return node.parent.parent.parent;
            } else if (node.parent.type === 'ExpressionStatement') {
                return node.parent;
            } else if (
                node.parent.type === 'MemberExpression' &&
                node.parent.parent.type === 'VariableDeclarator' &&
                node.parent.parent.parent.type === 'VariableDeclaration'
            ) {
                return node.parent.parent.parent;
            } else if (node.parent.type === 'VariableDeclarator' && node.parent.parent.type === 'VariableDeclaration') {
                return node.parent.parent;
            }
        }
    }
}

function getSource(node) {
    if (node.type === 'ImportDeclaration') {
        return node.source.value;
    } else if (node.type === 'VariableDeclaration') {
        const init = node.declarations[0].init;

        if (init.type === 'CallExpression') {
            if (init.callee.type === 'CallExpression') {
                // ie. `const ... = require('...')(...);

                return init.callee.arguments[0].value;
            } else {
                // ie. `const ... = require('...');`

                return init.arguments[0].value;
            }
        } else if (init.type === 'MemberExpression') {
            // ie. `const ... = require('...').thing;

            return init.object.arguments[0].value;
        }
    } else if (node.type === 'ExpressionStatement') {
        // ie. `require('...');`

        return node.expression.arguments[0].value;
    }
}

function getTrailingComments(node, context) {
    return context
        .getSourceCode()
        .getCommentsAfter(node)
        .filter((comment) => comment.loc.start.line === node.loc.end.line);
}

/**
 * Returns true if an import is made exclusively for its side effects;
 * eg:
 *
 *      import 'foo';
 *      require('bar');
 *
 * Such nodes form boundaries across which we must not re-order any
 * imports.
 */
function hasSideEffects(node) {
    if (node.type === 'ImportDeclaration') {
        return !node.specifiers.length;
    } else {
        // ie. a `require()` call.

        return node.type === 'ExpressionStatement';
    }
}

/**
 * Returns true if `source` is an absolute path (ie. starts with "/").
 *
 * Technically, we shouldn't have any of imports that use absolute paths
 * in our codebase, but a separate lint can handle that.
 */
function isAbsolute(source) {
    return /^\//.test(source);
}

function isHeaderComment(comment) {
    return (
        /\bcopyright\b|\(c\)|©/i.test(comment.value) && comment.loc.start.line < 3 /* At top or just after shebang. */
    );
}

/**
 * Returns true if `source` is a "local" path (ie. not a NodeJS built-in or
 * dependency declared in a "package.json" file).
 */
function isLocal(source) {
    return isAbsolute(source) || isRelative(source);
}

/**
 * Returns true if `source` is a relative path (ie. starts with "./" or "../").
 */
function isRelative(source) {
    return source === '.' || /^\.\.?\//.test(source);
}

/**
 * See also `getRequireStatement()` in this module.
 */
function isRequireStatement(node) {
    if (!node || node.type !== 'VariableDeclaration') {
        return false;
    }

    const {init} = node.declarations[0];

    // Check for `const a = require('a')`

    if (init.type === 'CallExpression' && init.callee.name === 'require') {
        return true;
    }

    // Check for `const a = require('a').item`

    if (
        init.type === 'MemberExpression' &&
        init.object.type === 'CallExpression' &&
        init.object.callee.name === 'require'
    ) {
        return true;
    }

    // Check for `const a = require('a')()`

    return (
        init.type === 'CallExpression' && init.callee.type === 'CallExpression' && init.callee.callee.name === 'require'
    );
}

/**
 * Returns true if `node` corresponds to a type-only import (ie. `import type
 * {x} from 'source'`).
 */
function isTypeImport(node) {
    return node.type === 'ImportDeclaration' && node.importKind === 'type';
}

function withScope() {
    const scope = [];

    const enterScope = (node) => scope.push(node);
    const exitScope = () => scope.pop();

    return {
        scope,

        visitors: {
            ArrowFunctionExpression: enterScope,
            'ArrowFunctionExpression:exit': exitScope,

            BlockStatement: enterScope,
            'BlockStatement:exit': exitScope,

            FunctionDeclaration: enterScope,
            'FunctionDeclaration:exit': exitScope,

            FunctionExpression: enterScope,
            'FunctionExpression:exit': exitScope,

            ObjectExpression: enterScope,
            'ObjectExpression:exit': exitScope,
        },
    };
}

module.exports = {
    getLeadingComments,
    getRequireStatement,
    getSource,
    getTrailingComments,
    hasSideEffects,
    isAbsolute,
    isLocal,
    isRelative,
    isRequireStatement,
    isTypeImport,
    withScope,
};
