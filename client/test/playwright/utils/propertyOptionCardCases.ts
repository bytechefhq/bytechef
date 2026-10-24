import {type AgUiEventType, toolCall} from './agUiStreamStub';

export interface PropertyOptionI {
    label: string;
    value: number | string;
}

export interface PropertyOptionCardCaseI {
    componentName: string;
    options: [PropertyOptionI, PropertyOptionI];
    propertyName: string;
    request: string;
    toolArguments: Record<string, unknown>;
    toolName: 'selectClusterElementPropertyOption' | 'selectTriggerPropertyOption';
}

export const PROPERTY_OPTION_CARD_CASES: PropertyOptionCardCaseI[] = [
    {
        componentName: 'knowledgeBase',
        options: [
            {label: 'Product docs', value: 12},
            {label: 'Support handbook', value: 13},
        ],
        propertyName: 'knowledgeBaseId',
        request: 'Answer questions from one of my knowledge bases',
        toolArguments: {
            clusterElementName: 'vectorStore',
            componentName: 'knowledgeBase',
            propertyName: 'knowledgeBaseId',
        },
        toolName: 'selectClusterElementPropertyOption',
    },
    {
        componentName: 'dataTable',
        options: [
            {label: 'signups', value: 'signups'},
            {label: 'orders', value: 'orders'},
        ],
        propertyName: 'table',
        request: 'Log every record added to one of my data tables',
        toolArguments: {componentName: 'dataTable', propertyName: 'table', triggerName: 'recordCreated'},
        toolName: 'selectTriggerPropertyOption',
    },
];

export const propertyOptionCardCall = ({
    componentName,
    options,
    propertyName,
    toolArguments,
    toolName,
}: PropertyOptionCardCaseI): AgUiEventType =>
    toolCall(
        toolName,
        toolArguments,
        JSON.stringify({
            awaitingAnswer: true,
            componentName,
            kind: 'select-property-option',
            options,
            propertyName,
            truncated: false,
        })
    );

export const pickedOptionMessage = ({label, value}: PropertyOptionI) => `User picked: ${label} (value: ${value})`;
