import {describe, expect, it} from 'vitest';

import resolveNodeOpenTab from './resolveNodeOpenTab';

const teamsDefinition = {connectionRequired: true, name: 'microsoftTeams'};

describe('resolveNodeOpenTab', () => {
    it('waits while the component definition of the opened node has not loaded', () => {
        expect(
            resolveNodeOpenTab({
                componentDefinition: {connectionRequired: true, name: 'microsoftExcel'},
                connectionsFetched: false,
                nodeComponentName: 'microsoftTeams',
                testConfigurationConnectionsCount: 0,
            })
        ).toBe('WAIT');
    });

    it('waits for the connections of a node that requires a connection before choosing a tab', () => {
        expect(
            resolveNodeOpenTab({
                componentDefinition: teamsDefinition,
                connectionsFetched: false,
                nodeComponentName: 'microsoftTeams',
                testConfigurationConnectionsCount: 0,
            })
        ).toBe('WAIT');
    });

    it('keeps the properties tab when the required connection is set', () => {
        expect(
            resolveNodeOpenTab({
                componentDefinition: teamsDefinition,
                connectionsFetched: true,
                nodeComponentName: 'microsoftTeams',
                testConfigurationConnectionsCount: 1,
            })
        ).toBe('KEEP');
    });

    it('opens the connection tab when the required connection is missing', () => {
        expect(
            resolveNodeOpenTab({
                componentDefinition: teamsDefinition,
                connectionsFetched: true,
                nodeComponentName: 'microsoftTeams',
                testConfigurationConnectionsCount: 0,
            })
        ).toBe('CONNECTION');
    });

    it('keeps the properties tab when the component does not require a connection', () => {
        expect(
            resolveNodeOpenTab({
                componentDefinition: {connectionRequired: false, name: 'httpClient'},
                connectionsFetched: false,
                nodeComponentName: 'httpClient',
                testConfigurationConnectionsCount: 0,
            })
        ).toBe('KEEP');
    });
});
