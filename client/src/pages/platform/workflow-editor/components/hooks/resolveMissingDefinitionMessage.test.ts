import {describe, expect, it} from 'vitest';

import resolveMissingDefinitionMessage from './resolveMissingDefinitionMessage';

describe('resolveMissingDefinitionMessage', () => {
    it('reports a component that is not available', () => {
        expect(
            resolveMissingDefinitionMessage({
                componentDefinitionLookupFailed: true,
                componentName: 'viber',
                componentVersion: 1,
                operationName: 'sendTextMessage',
            })
        ).toBe("Component 'viber' (version 1) is not available");
    });

    it('reports an action that does not exist in the component', () => {
        expect(
            resolveMissingDefinitionMessage({
                componentDefinition: {actions: [{name: 'sendSms'}], name: 'infobip', version: 1},
                componentDefinitionLookupFailed: false,
                componentName: 'infobip',
                componentVersion: 1,
                operationName: 'sendViberBotTextMessage',
            })
        ).toBe("Action 'sendViberBotTextMessage' does not exist in component 'infobip' (version 1)");
    });

    it('reports a trigger that does not exist in the component', () => {
        expect(
            resolveMissingDefinitionMessage({
                componentDefinition: {name: 'infobip', triggers: [{name: 'newSmsMessage'}], version: 1},
                componentDefinitionLookupFailed: false,
                componentName: 'infobip',
                componentVersion: 1,
                operationName: 'newViberBusinessMessage',
                trigger: true,
            })
        ).toBe("Trigger 'newViberBusinessMessage' does not exist in component 'infobip' (version 1)");
    });

    it('treats a component without the operation list as missing the operation', () => {
        expect(
            resolveMissingDefinitionMessage({
                componentDefinition: {name: 'infobip', version: 1},
                componentDefinitionLookupFailed: false,
                componentName: 'infobip',
                componentVersion: 1,
                operationName: 'newViberBusinessMessage',
                trigger: true,
            })
        ).toBe("Trigger 'newViberBusinessMessage' does not exist in component 'infobip' (version 1)");
    });

    it('returns nothing when the operation exists', () => {
        expect(
            resolveMissingDefinitionMessage({
                componentDefinition: {actions: [{name: 'set'}], name: 'var', version: 1},
                componentDefinitionLookupFailed: false,
                componentName: 'var',
                componentVersion: 1,
                operationName: 'set',
            })
        ).toBeUndefined();
    });

    it('returns nothing while the component definition is still loading', () => {
        expect(
            resolveMissingDefinitionMessage({
                componentDefinitionLookupFailed: false,
                componentName: 'infobip',
                componentVersion: 1,
                operationName: 'sendViberBotTextMessage',
            })
        ).toBeUndefined();
    });

    it('returns nothing for task dispatchers and cluster elements', () => {
        expect(
            resolveMissingDefinitionMessage({
                componentDefinitionLookupFailed: true,
                componentName: 'loop',
                componentVersion: 1,
                taskDispatcher: true,
            })
        ).toBeUndefined();

        expect(
            resolveMissingDefinitionMessage({
                clusterElementType: 'model',
                componentDefinition: {name: 'openAi', version: 1},
                componentDefinitionLookupFailed: false,
                componentName: 'openAi',
                componentVersion: 1,
                operationName: 'model',
            })
        ).toBeUndefined();
    });

    it('returns nothing for a node without an operation', () => {
        expect(
            resolveMissingDefinitionMessage({
                componentDefinition: {actions: [], name: 'manual', version: 1},
                componentDefinitionLookupFailed: false,
                componentName: 'manual',
                componentVersion: 1,
            })
        ).toBeUndefined();
    });
});
