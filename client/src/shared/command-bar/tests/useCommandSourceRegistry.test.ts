import {type CommandContextI, type CommandI} from '@/shared/command-bar/types';
import {
    collectCommands,
    registerCommandSource,
    useCommandSourceRegistry,
} from '@/shared/command-bar/useCommandSourceRegistry';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const context: CommandContextI = {
    edition: 'EE',
    featureFlags: () => true,
    pathname: '/automation/projects',
};

const command = (id: string, overrides: Partial<CommandI> = {}): CommandI => ({
    actions: [{to: `/${id}`, type: 'navigate'}],
    id,
    title: id,
    ...overrides,
});

describe('useCommandSourceRegistry', () => {
    beforeEach(() => {
        useCommandSourceRegistry.getState().reset();
    });

    it('collects commands from every registered source', () => {
        registerCommandSource({getCommands: () => [command('a')], id: 'first'});
        registerCommandSource({getCommands: () => [command('b')], id: 'second'});

        const commands = collectCommands(useCommandSourceRegistry.getState().sources, context);

        expect(commands.map((collected) => collected.id)).toEqual(['a', 'b']);
    });

    it('unregisters through the returned callback', () => {
        registerCommandSource({getCommands: () => [command('a')], id: 'first'});
        const unregisterSecond = registerCommandSource({getCommands: () => [command('b')], id: 'second'});
        registerCommandSource({getCommands: () => [command('c')], id: 'third'});

        unregisterSecond();

        expect(useCommandSourceRegistry.getState().sources.map((registeredSource) => registeredSource.id)).toEqual([
            'first',
            'third',
        ]);
    });

    it('passes the context to each source', () => {
        const getCommands = vi.fn().mockReturnValue([]);

        registerCommandSource({getCommands, id: 'first'});

        collectCommands(useCommandSourceRegistry.getState().sources, context);

        expect(getCommands).toHaveBeenCalledWith(context);
    });

    it('drops commands whose when predicate is false', () => {
        registerCommandSource({
            getCommands: () => [command('a', {when: () => false}), command('b')],
            id: 'first',
        });

        const commands = collectCommands(useCommandSourceRegistry.getState().sources, context);

        expect(commands.map((collected) => collected.id)).toEqual(['b']);
    });

    it('keeps the last registration when two sources share an id', () => {
        registerCommandSource({getCommands: () => [command('a', {title: 'first'})], id: 'first'});
        registerCommandSource({getCommands: () => [command('a', {title: 'second'})], id: 'second'});

        const commands = collectCommands(useCommandSourceRegistry.getState().sources, context);

        expect(commands).toHaveLength(1);
        expect(commands[0].title).toBe('second');
    });

    it('rejects a command declaring both actions and children', () => {
        registerCommandSource({
            getCommands: () => [command('a', {children: {placeholder: 'Search...', resolve: async () => []}})],
            id: 'first',
        });

        expect(() => collectCommands(useCommandSourceRegistry.getState().sources, context)).toThrow(/exactly one of/);
    });

    it('rejects a command declaring neither actions nor children', () => {
        registerCommandSource({getCommands: () => [{id: 'a', title: 'a'}], id: 'first'});

        expect(() => collectCommands(useCommandSourceRegistry.getState().sources, context)).toThrow(/exactly one of/);
    });
});
