import Button from '@/components/Button/Button';
import {Card, CardHeader} from '@/components/ui/card';
import {ExternalLinkIcon} from 'lucide-react';

interface ManageBillingCardPropsI {
    customerPortalUrl: string;
}

const ManageBillingCard = ({customerPortalUrl}: ManageBillingCardPropsI) => (
    <Card className="w-full max-w-3xl">
        <CardHeader className="flex flex-row items-start justify-between space-y-0">
            <div className="flex flex-col gap-1">
                <span className="font-bold">Billing &amp; invoices</span>

                <span className="text-sm text-muted-foreground">
                    Update your payment method and billing details, and view or download past invoices.
                </span>
            </div>

            <Button onClick={() => window.open(customerPortalUrl, '_blank', 'noopener,noreferrer')} variant="default">
                Manage billing &amp; invoices <ExternalLinkIcon />
            </Button>
        </CardHeader>
    </Card>
);

export default ManageBillingCard;
