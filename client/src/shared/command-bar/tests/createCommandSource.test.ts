import {createCommandSource} from '@/shared/command-bar/sources/createCommandSource';
import {type CommandContextI} from '@/shared/command-bar/types';
import {describe, expect, it} from 'vitest';

const context: CommandContextI = {edition: 'EE', featureFlags: () => true, pathname: '/automation/datatables'};

const EXPECTED_CREATE_COMMANDS = [
    {id: 'create.project', intentKey: 'project.create', to: '/automation/projects'},
    {id: 'create.connection', intentKey: 'connection.create', to: '/automation/connections'},
    {id: 'create.dataTable', intentKey: 'dataTable.create', to: '/automation/datatables'},
    {id: 'create.knowledgeBase', intentKey: 'knowledgeBase.create', to: '/automation/knowledge-bases'},
];

describe('createCommandSource', () => {
    it.each(EXPECTED_CREATE_COMMANDS)(
        'navigates $id to $to then publishes the $intentKey intent',
        ({id, intentKey, to}) => {
            const command = createCommandSource.getCommands(context).find((candidate) => candidate.id === id)!;

            expect(command.actions).toEqual([
                {to, type: 'navigate'},
                {key: intentKey, type: 'intent'},
            ]);
        }
    );

    it('exposes a create command for each supported resource', () => {
        expect(createCommandSource.getCommands(context).map((command) => command.id)).toEqual(
            EXPECTED_CREATE_COMMANDS.map((expectedCommand) => expectedCommand.id)
        );
    });
});
