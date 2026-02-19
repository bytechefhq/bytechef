import {Button} from '@/components/ui/button';
import {Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle} from '@/components/ui/card';
import {EllipsisVerticalIcon} from 'lucide-react';
import * as React from 'react';

const workflows = [
    {
        name: 'First Name',
    },
    {
        name: 'Second Name',
    },
];

const IntegrationPortalConfigurationSettings = () => {
    return (
        <Card className="w-full border-0 shadow-none">
            <CardHeader className="px-0">
                <CardTitle>Settings</CardTitle>

                <CardDescription>
                    Provide settings for your users to configure the integration when linking an account. These values
                    can be used by any workflows connected to this integration.
                </CardDescription>
            </CardHeader>

            <CardContent className="px-0">
                <ul className="divide-y divide-gray-100" role="list">
                    {workflows.map((workflow) => (
                        <li className="relative flex justify-between py-4" key={workflow.name}>
                            <div className="min-w-0 flex-auto cursor-pointer">
                                <span className="text-sm font-semibold leading-6 text-gray-900">{workflow.name}</span>

                                <p className="mt-1 flex text-xs leading-5 text-gray-500">Text</p>
                            </div>

                            <div className="flex shrink-0 items-center gap-x-4">
                                <EllipsisVerticalIcon className="size-4 hover:cursor-pointer" />
                            </div>
                        </li>
                    ))}
                </ul>
            </CardContent>

            <CardFooter className="flex justify-between px-0">
                <Button className="w-full" size="sm" variant="secondary">
                    Add setting
                </Button>
            </CardFooter>
        </Card>
    );
};

export default IntegrationPortalConfigurationSettings;
