import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';

import PlanCard from './components/PlanCard';

const MOCK_SUBSCRIPTION = {
    cancelAtPeriodEnd: false,
    planName: 'Trial',
    taskLimit: 5000,
    tasksUsed: 0,
    trialDaysRemaining: 30,
};

const Billing = () => (
    <LayoutContainer
        header={
            <Header centerTitle description="Manage your subscription and usage." position="main" title="Billing" />
        }
        leftSidebarOpen={false}
    >
        <div className="w-full space-y-4 px-4 3xl:mx-auto 3xl:w-4/5">
            <Tabs defaultValue="overview">
                <TabsList>
                    <TabsTrigger value="overview">Overview</TabsTrigger>

                    <TabsTrigger value="invoices">Invoices</TabsTrigger>
                </TabsList>

                <TabsContent className="mt-4 space-y-4" value="overview">
                    <PlanCard {...MOCK_SUBSCRIPTION} onCancelPlan={() => {}} onChangePlan={() => {}} />
                </TabsContent>

                <TabsContent className="mt-4" value="invoices">
                    <p className="text-sm text-muted-foreground">No invoices yet.</p>
                </TabsContent>
            </Tabs>
        </div>
    </LayoutContainer>
);

export default Billing;
