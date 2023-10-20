import {Button} from '@/components/ui/button';
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import IntegrationPortalContent from '@/ee/pages/embedded/integration/components/IntegrationPortalContent';
import * as React from 'react';

const IntegrationPortal = () => {
    return (
        <Dialog open={true} modal={false}>
            <DialogContent className="max-w-[600px]">
                <DialogHeader>
                    <div className="flex justify-between">
                        <div>
                            <DialogTitle>Pipedrive</DialogTitle>

                            <DialogDescription>
                                Sync records from Pipedrive.
                            </DialogDescription>
                        </div>

                        <div>
                            <Button variant="secondary">Connect</Button>
                        </div>
                    </div>
                </DialogHeader>

                <IntegrationPortalContent />
            </DialogContent>
        </Dialog>
    );
};

export default IntegrationPortal;
