import {Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle, SheetTrigger} from '@/components/ui/sheet';
import IntegrationPortalConfigurationWorkflowConfigurationFeatures from '@/ee/pages/embedded/integration/components/delete/IntegrationPortalConfigurationWorkflowConfigurationFeatures';
import IntegrationPortalConfigurationWorkflowConfigurationForm from '@/ee/pages/embedded/integration/components/delete/IntegrationPortalConfigurationWorkflowConfigurationForm';
import IntegrationPortalConfigurationWorkflowConfigurationPermissions from '@/ee/pages/embedded/integration/components/delete/IntegrationPortalConfigurationWorkflowConfigurationPermissions';
import IntegrationPortalConfigurationWorkflowConfigurationSettings from '@/ee/pages/embedded/integration/components/delete/IntegrationPortalConfigurationWorkflowConfigurationSettings';
import * as React from 'react';

const IntegrationPortalConfigurationWorkflowSheet = () => {
    return (
        <Sheet modal={false}>
            <SheetTrigger asChild>
                <div>Workflow 1</div>
            </SheetTrigger>

            <SheetContent className="flex flex-col sm:max-w-workflow-integration-portal-configuration-workflow-sheet-width">
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
