import {Button} from '@/components/ui/button';
import {Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import IntegrationPortalDialogContent from '@/pages/embedded/integration/components/delete/IntegrationPortalDialogContent';
import * as React from 'react';

const IntegrationConnectPortalDialog = () => {
    return (
        <Dialog modal={false} open={true}>
            <DialogContent className="max-w-[600px]">
                <DialogHeader>
                    <div className="flex justify-between">
                        <div>
                            <DialogTitle>Pipedrive</DialogTitle>

                            <DialogDescription>Sync records from Pipedrive.</DialogDescription>
                        </div>

                        <div>
                            <Button variant="secondary">Connect</Button>
                        </div>
                    </div>
                </DialogHeader>

                <IntegrationPortalDialogContent />
            </DialogContent>
        </Dialog>
    );
};

export default IntegrationConnectPortalDialog;
