import {type CommandI} from '@/shared/command-bar/types';
import {useCommandBarStore} from '@/shared/command-bar/useCommandBarStore';
import {beforeEach, describe, expect, it} from 'vitest';

const openWorkflow: CommandI = {
    children: {placeholder: 'Search by workflow name...', resolve: async () => []},
    id: 'workflow.open',
    title: 'Open workflow',
};

describe('useCommandBarStore', () => {
    beforeEach(() => {
        useCommandBarStore.getState().close();
    });

    it('pushes a command onto the stack and clears the query', () => {
        useCommandBarStore.getState().setQuery('wor');
        useCommandBarStore.getState().pushCommand(openWorkflow);

        expect(useCommandBarStore.getState().stack).toEqual([openWorkflow]);
        expect(useCommandBarStore.getState().query).toBe('');
    });

    it('pops the stack and clears the query', () => {
        useCommandBarStore.getState().pushCommand(openWorkflow);
        useCommandBarStore.getState().setQuery('my');
        useCommandBarStore.getState().popCommand();

        expect(useCommandBarStore.getState().stack).toEqual([]);
        expect(useCommandBarStore.getState().query).toBe('');
    });

    it('resets the stack and the query when closed', () => {
        useCommandBarStore.getState().setOpen(true);
        useCommandBarStore.getState().pushCommand(openWorkflow);
        useCommandBarStore.getState().setQuery('my');
        useCommandBarStore.getState().close();

        expect(useCommandBarStore.getState().open).toBe(false);
        expect(useCommandBarStore.getState().stack).toEqual([]);
        expect(useCommandBarStore.getState().query).toBe('');
    });

    it('treats setOpen(false) as a close', () => {
        useCommandBarStore.getState().setOpen(true);
        useCommandBarStore.getState().pushCommand(openWorkflow);
        useCommandBarStore.getState().setQuery('my');
        useCommandBarStore.getState().setOpen(false);

        expect(useCommandBarStore.getState().stack).toEqual([]);
        expect(useCommandBarStore.getState().query).toBe('');
    });
});
