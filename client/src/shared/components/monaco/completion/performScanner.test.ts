import {describe, expect, it} from 'vitest';

import {findEnclosingFunctionName, findPerformScope} from './performScanner';

describe('findPerformScope', () => {
    it('maps three JavaScript parameters positionally', () => {
        const source = 'function perform(input, connection, context) {\n  input.\n}';

        expect(findPerformScope(source, source.indexOf('input.') + 6, 'javascript')).toMatchObject({
            connectionName: 'connection',
            contextName: 'context',
            inputName: 'input',
        });
    });

    it('maps the older custom component parameter names', () => {
        const source = 'perform: function (inputParameters, connectionParameters, context) {\n  inputParameters.';

        expect(findPerformScope(source, source.length, 'javascript')).toMatchObject({
            connectionName: 'connectionParameters',
            contextName: 'context',
            inputName: 'inputParameters',
        });
    });

    it('maps a two-parameter script as input and context', () => {
        const source = 'function perform(input, context) {\n  context.';

        expect(findPerformScope(source, source.length, 'javascript')).toMatchObject({
            contextName: 'context',
            inputName: 'input',
        });
        expect(findPerformScope(source, source.length, 'javascript')?.connectionName).toBeUndefined();
    });

    it('maps an arrow function with no parentheses', () => {
        const source = 'perform: context => {\n  context.';

        expect(findPerformScope(source, source.length, 'javascript')).toMatchObject({contextName: 'context'});
    });

    it('maps a method shorthand', () => {
        const source = '{ perform(context) {\n  context.';

        expect(findPerformScope(source, source.length, 'javascript')).toMatchObject({contextName: 'context'});
    });

    it('returns undefined outside any function', () => {
        expect(findPerformScope('const x = 1;\nx.', 15, 'javascript')).toBeUndefined();
    });

    it('does not leak an outer scope into a sibling function that already closed', () => {
        const source = 'function perform(input, context) {\n  return 1;\n}\nconst other = 2;\nother.';

        expect(findPerformScope(source, source.length, 'javascript')).toBeUndefined();
    });

    it('maps the outermost function when no root is given', () => {
        const source = 'function perform(input, context) {\n  const f = (row) => {\n    ro';

        expect(findPerformScope(source, source.length, 'javascript')).toMatchObject({
            contextName: 'context',
            inputName: 'input',
        });
    });

    it('returns undefined for a root bound by an inner callback', () => {
        const source = 'function perform(input, context) {\n  const f = (row) => {\n    row.';

        expect(findPerformScope(source, source.length, 'javascript', 'row')).toBeUndefined();
    });

    it('maps the perform for a root it binds from inside a callback', () => {
        const source = 'function perform(input, context) {\n  input.rows.map((row) => {\n    context.';

        expect(findPerformScope(source, source.length, 'javascript', 'context')).toMatchObject({
            contextName: 'context',
            inputName: 'input',
        });
    });

    it('returns undefined for a root no enclosing function binds', () => {
        const source = 'function perform(input, context) {\n  other.';

        expect(findPerformScope(source, source.length, 'javascript', 'other')).toBeUndefined();
    });

    it('maps a Python def by indentation', () => {
        const source = 'def greet(context):\n    name = context.';

        expect(findPerformScope(source, source.length, 'python')).toMatchObject({contextName: 'context'});
        expect(findEnclosingFunctionName(source, source.length, 'python')).toBe('greet');
    });

    it('ends a Python def at a dedent', () => {
        const source = 'def greet(context):\n    return 1\n\nproject = {\n  "x": ctx.';

        expect(findPerformScope(source, source.length, 'python')).toBeUndefined();
    });

    it('maps a Python lambda on its own line', () => {
        const source = '"perform": lambda context: context.';

        expect(findPerformScope(source, source.length, 'python')).toMatchObject({contextName: 'context'});
    });

    it('returns a nameless scope for lambda *args', () => {
        const source = '"perform": lambda *args: args[0].';

        expect(findPerformScope(source, source.length, 'python')).toEqual({performStart: source.indexOf('lambda')});
    });

    it('maps a Ruby lambda block', () => {
        const source = '"perform" => lambda { |context| context.';

        expect(findPerformScope(source, source.length, 'ruby')).toMatchObject({contextName: 'context'});
    });

    it('maps a Ruby stabby lambda and def', () => {
        expect(findPerformScope('->(input, connection, context) { input.', 39, 'ruby')).toMatchObject({
            inputName: 'input',
        });
        expect(findPerformScope('def perform(input, connection, context)\n  connection.', 52, 'ruby')).toMatchObject({
            connectionName: 'connection',
        });
    });

    it('names the enclosing function and returns undefined for anonymous heads', () => {
        const named = 'function perform(input, context) {\n  context.';
        const anonymous = 'const x = (row) => {\n  row.';

        expect(findEnclosingFunctionName(named, named.length, 'javascript')).toBe('perform');
        expect(findEnclosingFunctionName('foo((row) => {\n  row.', 19, 'javascript')).toBeUndefined();
        expect(findEnclosingFunctionName(anonymous, anonymous.length, 'javascript')).toBe('x');
    });

    it('ends a Ruby def at its end keyword', () => {
        const source = 'def perform(input, context)\n  if input\n    1\n  end\nend\nother.';

        expect(findPerformScope(source, source.length, 'ruby')).toBeUndefined();
    });

    it('ignores braces inside JavaScript comments', () => {
        const source =
            'function perform(input, context) {\n  // }\n  /* } */\n  const f = (row) => row.id; // }\n  context.';

        expect(findPerformScope(source, source.length, 'javascript', 'context')).toMatchObject({
            contextName: 'context',
        });
    });

    it('ignores closing parentheses inside a comment in an expression-bodied arrow', () => {
        const source = 'function perform(input, context) {\n  const f = (row) => row.id /* ) */ + row.';

        expect(findEnclosingFunctionName(source, source.length, 'javascript')).toBe('f');
    });

    it('ignores end keywords inside Ruby comments and strings', () => {
        const source = 'def perform(input, context)\n  # end\n  label = "end }"\n  context.';

        expect(findPerformScope(source, source.length, 'ruby', 'context')).toMatchObject({contextName: 'context'});
    });

    it('ignores dedented Python comment lines', () => {
        const source = 'def perform(input, context):\n# note\n    context.';

        expect(findPerformScope(source, source.length, 'python', 'context')).toMatchObject({contextName: 'context'});
    });
});
