import {WorkflowDefinitionType} from '../types';

export const SAMPLE_WORKFLOW_DEFINITION: WorkflowDefinitionType = {
    label: 'Sample: lead routing',
    tasks: [
        {
            name: 'fetchLead',
            parameters: {
                uri: 'https://example.com/leads/latest',
            },
            type: 'httpClient/v1/get',
        },
        {
            name: 'checkScore',
            parameters: {
                caseFalse: [
                    {
                        name: 'notifySales',
                        parameters: {},
                        type: 'slack/v2/sendMessage',
                    },
                ],
                caseTrue: [
                    {
                        name: 'enrichEach',
                        parameters: {
                            iteratee: [
                                {
                                    name: 'enrichContact',
                                    parameters: {},
                                    type: 'hubspot/v1/updateContact',
                                },
                            ],
                        },
                        type: 'loop/v1',
                    },
                ],
                expression: '${fetchLead.score} > 50',
            },
            type: 'condition/v1',
        },
        {
            name: 'routeRegion',
            parameters: {
                cases: [
                    {
                        key: 'emea',
                        tasks: [
                            {
                                name: 'mailEmea',
                                parameters: {},
                                type: 'googleMail/v1/sendEmail',
                            },
                        ],
                    },
                    {
                        key: 'amer',
                        tasks: [
                            {
                                name: 'mailAmer',
                                parameters: {},
                                type: 'googleMail/v1/sendEmail',
                            },
                        ],
                    },
                ],
                default: [],
                expression: '${fetchLead.region}',
            },
            type: 'branch/v1',
        },
    ],
    triggers: [
        {
            name: 'trigger_1',
            parameters: {},
            type: 'manual/v1/manual',
        },
    ],
};

export const SAMPLE_WORKFLOW_DEFINITION_UPDATED: WorkflowDefinitionType = {
    ...SAMPLE_WORKFLOW_DEFINITION,
    tasks: [
        ...(SAMPLE_WORKFLOW_DEFINITION.tasks as Array<unknown>),
        {
            name: 'archiveLead',
            parameters: {},
            type: 'airtable/v1/createRecord',
        },
    ],
};
