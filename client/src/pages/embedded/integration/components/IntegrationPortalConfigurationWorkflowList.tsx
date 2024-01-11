import {Card, CardContent, CardDescription, CardHeader, CardTitle} from '@/components/ui/card';
import IntegrationPortalConfigurationWorkflowSheet from '@/pages/embedded/integration/components/IntegrationPortalConfigurationWorkflowSheet';
import {DotsVerticalIcon} from '@radix-ui/react-icons';
import * as React from 'react';

const workflows = [
    {
        name: 'Workflow 1',
        sheet: <IntegrationPortalConfigurationWorkflowSheet />,
    },
    {
        name: 'Workflow 2',
    },
    {
        name: 'Workflow 3',
    },
    {
        name: 'Workflow 4',
    },
    {
        name: 'Workflow 5',
    },
    {
        name: 'Workflow 6',
    },
];

const IntegrationPortalConfigurationWorkflowList = () => {
    return (
        <Card className="w-full border-0 shadow-none">
            <CardHeader className="px-0">
                <CardTitle>Workflows</CardTitle>

                <CardDescription>
                    Choose the workflows that are visible and adjustable to your users. You can add settings for each
                    workflow to enable more customization.
                </CardDescription>
            </CardHeader>

            <CardContent className="px-0">
                <ul className="divide-y divide-gray-100" role="list">
                    {workflows.map((workflow) => (
                        <li className="relative flex justify-between py-4" key={workflow.name}>
                            <div className="min-w-0 flex-auto cursor-pointer">
                                <span className="text-sm font-semibold leading-6 text-gray-900">
                                    {workflow.sheet || workflow.name}
                                </span>
                            </div>

                            <div className="flex shrink-0 items-center gap-x-4">
                                <DotsVerticalIcon aria-hidden="true" className="size-4 flex-none text-gray-500" />
                            </div>
                        </li>
                    ))}
                </ul>
            </CardContent>
        </Card>
    );
};

export default IntegrationPortalConfigurationWorkflowList;
