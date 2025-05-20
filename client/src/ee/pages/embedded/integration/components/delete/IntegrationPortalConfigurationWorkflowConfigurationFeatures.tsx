import {Card, CardContent} from '@/components/ui/card';
import {Switch} from '@/components/ui/switch';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import * as React from 'react';

const IntegrationPortalConfigurationWorkflowConfigurationFeatures = () => {
    return (
        <Card className="w-full border-0 shadow-none">
            <CardContent className="space-y-4 px-0">
                <div className="flex justify-between">
                    <div className="text-sm">
                        <Tooltip>
                            <TooltipTrigger>Default to enabled</TooltipTrigger>

                            <TooltipContent>
                                When on, this workflow will automatically be enabled when the user connects their
                                account.
                            </TooltipContent>
                        </Tooltip>
                    </div>

                    <div>
                        <Switch />
                    </div>
                </div>

                <div className="flex justify-between">
                    <div className="text-sm">
                        <Tooltip>
                            <TooltipTrigger>Hide workflow from Portal for all users</TooltipTrigger>

                            <TooltipContent>
                                When on, this workflow will not be displayed in the Portal. The workflow can still be
                                enabled using the API, the Connected Users Dashboard, or by default.
                            </TooltipContent>
                        </Tooltip>
                    </div>

                    <div>
                        <Switch />
                    </div>
                </div>
            </CardContent>
        </Card>
    );
};

export default IntegrationPortalConfigurationWorkflowConfigurationFeatures;
