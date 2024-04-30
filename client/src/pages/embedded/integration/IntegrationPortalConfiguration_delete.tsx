import {Button} from '@/components/ui/button';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import LayoutContainer from '@/layouts/LayoutContainer';
import PageHeader from '@/layouts/PageHeader';
import IntegrationPortalConfigurationOverviewForm from '@/pages/embedded/integration/components/delete/IntegrationPortalConfigurationOverviewForm';
import IntegrationPortalConfigurationSettings from '@/pages/embedded/integration/components/delete/IntegrationPortalConfigurationSettings';
import IntegrationPortalConfigurationWorkflowList from '@/pages/embedded/integration/components/delete/IntegrationPortalConfigurationWorkflowList';
import IntegrationPortalDialogContent from '@/pages/embedded/integration/components/delete/IntegrationPortalDialogContent';
import * as React from 'react';

const IntegrationPortalConfiguration_delete = () => {
    return (
        <LayoutContainer
            className="bg-muted"
            header={<PageHeader position="main" title="Integration Portal" />}
            leftSidebarOpen={false}
            rightSidebarBody={
                <div className="space-y-4 p-4">
                    <p className="text-sm text-muted-foreground">
                        Define the visibility and the settings your users can personalize when they are integrating
                        their account.
                    </p>

                    <Tabs className="w-full" defaultValue="overview">
                        <TabsList className="grid w-full grid-cols-3">
                            <TabsTrigger value="overview">Overview</TabsTrigger>

                            <TabsTrigger value="configuration">Configuration</TabsTrigger>

                            <TabsTrigger value="appearance">Appearance</TabsTrigger>
                        </TabsList>

                        <TabsContent value="overview">
                            <IntegrationPortalConfigurationOverviewForm />
                        </TabsContent>

                        <TabsContent className="space-y-4" value="configuration">
                            <IntegrationPortalConfigurationSettings />

                            <IntegrationPortalConfigurationWorkflowList />
                        </TabsContent>

                        <TabsContent value="appearance"></TabsContent>
                    </Tabs>
                </div>
            }
            rightSidebarHeader={<PageHeader position="main" title="Custommize" />}
            rightSidebarOpen={true}
            rightSidebarWidth="460"
        >
            <div className="flex w-full items-center justify-center">
                <div className="flex w-[600px] flex-col gap-4 rounded-lg border bg-background p-6 shadow-lg">
                    <div className="flex flex-col space-y-1.5 text-center sm:text-left">
                        <div className="flex justify-between">
                            <div>
                                <h2 className="text-lg font-semibold leading-none tracking-tight">Pipedrive</h2>

                                <p className="text-sm text-muted-foreground">Sync records from Pipedrive.</p>
                            </div>

                            <div>
                                <Button variant="secondary">Connect</Button>
                            </div>
                        </div>
                    </div>

                    <div>
                        <IntegrationPortalDialogContent />
                    </div>
                </div>
            </div>
        </LayoutContainer>
    );
};

export default IntegrationPortalConfiguration_delete;
