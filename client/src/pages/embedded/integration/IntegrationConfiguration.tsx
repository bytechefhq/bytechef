import {Button} from '@/components/ui/button';
import LayoutContainer from '@/layouts/LayoutContainer';
import PageHeader from '@/layouts/PageHeader';
import IntegrationLeftSidebarNav from '@/pages/embedded/integration/components/IntegrationLeftSidebarNav';
import IntegrationPortalContent from '@/pages/embedded/integration/components/IntegrationPortalContent';
import * as React from 'react';

const IntegrationConfiguration = () => {
    return (
        <LayoutContainer
            className="bg-muted"
            header={<PageHeader position="main" title="Configuration" />}
            leftSidebarHeader={
                <PageHeader position="sidebar" title="Pipedrive Integration" />
            }
            leftSidebarBody={<IntegrationLeftSidebarNav />}
            rightSidebarOpen={true}
            rightSidebarBody={
                <PageHeader position="main" title="Integration Portal" />
            }
            rightSidebarWidth="460"
        >
            <div className="flex w-full items-center justify-center">
                <div className="flex w-[600px] flex-col gap-4 rounded-lg border bg-background p-6 shadow-lg">
                    <div className="flex flex-col space-y-1.5 text-center sm:text-left">
                        <div className="flex justify-between">
                            <div>
                                <h2 className="text-lg font-semibold leading-none tracking-tight">
                                    Pipedrive
                                </h2>

                                <p className="text-sm text-muted-foreground">
                                    Sync records from Pipedrive.
                                </p>
                            </div>

                            <div>
                                <Button variant="secondary">Connect</Button>
                            </div>
                        </div>
                    </div>

                    <div>
                        <IntegrationPortalContent />
                    </div>
                </div>
            </div>
        </LayoutContainer>
    );
};

export default IntegrationConfiguration;
