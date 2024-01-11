import {Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle, SheetTrigger} from '@/components/ui/sheet';
import IntegrationPortalConfigurationWorkflowConfigurationFeatures from '@/pages/embedded/integration/components/IntegrationPortalConfigurationWorkflowConfigurationFeatures';
import IntegrationPortalConfigurationWorkflowConfigurationForm from '@/pages/embedded/integration/components/IntegrationPortalConfigurationWorkflowConfigurationForm';
import IntegrationPortalConfigurationWorkflowConfigurationPermissions from '@/pages/embedded/integration/components/IntegrationPortalConfigurationWorkflowConfigurationPermissions';
import IntegrationPortalConfigurationWorkflowConfigurationSettings from '@/pages/embedded/integration/components/IntegrationPortalConfigurationWorkflowConfigurationSettings';
import * as React from 'react';

const IntegrationPortalConfigurationWorkflowSheet = () => {
    return (
        <Sheet modal={false}>
            <SheetTrigger asChild>
                <div>Workflow 1</div>
            </SheetTrigger>

            <SheetContent className="flex flex-col sm:max-w-[460px]">
                <SheetHeader>
                    <SheetTitle>Workflow Configuration</SheetTitle>

                    <SheetDescription>
                        Define name and description, and provide custom settings so users can add their own values for
                        this workflow.
                    </SheetDescription>
                </SheetHeader>

                <div className="flex-1 space-y-2 overflow-y-auto">
                    <IntegrationPortalConfigurationWorkflowConfigurationForm />

                    <IntegrationPortalConfigurationWorkflowConfigurationSettings />

                    <IntegrationPortalConfigurationWorkflowConfigurationFeatures />

                    <IntegrationPortalConfigurationWorkflowConfigurationPermissions />
                </div>
            </SheetContent>
        </Sheet>
    );
};

export default IntegrationPortalConfigurationWorkflowSheet;
