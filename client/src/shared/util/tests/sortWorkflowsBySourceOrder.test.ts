import {describe, expect, it} from 'vitest';

import sortWorkflowsBySourceOrder from '../sortWorkflowsBySourceOrder';

const source = `export default definition({
    workflows: [
        {name: "greet", label: "Greet on Request"},
        {name: "summarize", label: 'Summarize New Email'},
        {name: "fetchRecentEmails", label: "Fetch Recent Emails", tasks: [{name: "fetchEmails", label: "Fetch Emails"}]},
        {name: "notifySlackOnNewEmail", label: "Notify Slack on New Email"}
    ]
})`;

const getLabel = (workflow: {label: string}) => workflow.label;

describe('sortWorkflowsBySourceOrder', () => {
    it('orders workflows as their labels appear in the source, with either quote style', () => {
        const workflows = [
            {label: 'Notify Slack on New Email'},
            {label: 'Fetch Recent Emails'},
            {label: 'Summarize New Email'},
            {label: 'Greet on Request'},
        ];

        expect(sortWorkflowsBySourceOrder(workflows, source, getLabel).map(getLabel)).toEqual([
            'Greet on Request',
            'Summarize New Email',
            'Fetch Recent Emails',
            'Notify Slack on New Email',
        ]);
    });

    it('puts workflows missing from the source last, alphabetically', () => {
        const workflows = [{label: 'Zeta'}, {label: 'Fetch Recent Emails'}, {label: 'Alpha'}];

        expect(sortWorkflowsBySourceOrder(workflows, source, getLabel).map(getLabel)).toEqual([
            'Fetch Recent Emails',
            'Alpha',
            'Zeta',
        ]);
    });

    it('does not match a workflow label inside a longer task label', () => {
        const workflows = [{label: 'Fetch Recent Emails'}, {label: 'Fetch Emails'}];

        expect(sortWorkflowsBySourceOrder(workflows, source, getLabel).map(getLabel)).toEqual([
            'Fetch Recent Emails',
            'Fetch Emails',
        ]);
    });
});
