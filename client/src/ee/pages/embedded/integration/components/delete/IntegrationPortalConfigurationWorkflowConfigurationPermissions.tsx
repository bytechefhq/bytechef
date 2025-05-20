import {Card, CardContent, CardDescription, CardHeader, CardTitle} from '@/components/ui/card';
import * as React from 'react';

const IntegrationPortalConfigurationWorkflowConfigurationPermissions = () => {
    return (
        <Card className="w-full border-0 shadow-none">
            <CardHeader className="px-0">
                <CardTitle>Permissions</CardTitle>

                <CardDescription>
                    Decide which users have the visibility and the capability to activate this workflow.
                </CardDescription>
            </CardHeader>

            <CardContent className="px-0"></CardContent>
        </Card>
    );
};

export default IntegrationPortalConfigurationWorkflowConfigurationPermissions;
