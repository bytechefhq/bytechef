import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {ReactNode} from 'react';

export type IntegrationListItemTabType = 'configurations' | 'workflows';

interface IntegrationListItemTabsProps {
    actions?: ReactNode;
    activeTab: IntegrationListItemTabType;
    configurationsContent: ReactNode;
    onTabChange: (tab: IntegrationListItemTabType) => void;
    workflowsContent: ReactNode;
}

const IntegrationListItemTabs = ({
    actions,
    activeTab,
    configurationsContent,
    onTabChange,
    workflowsContent,
}: IntegrationListItemTabsProps) => (
    <Tabs onValueChange={(value) => onTabChange(value as IntegrationListItemTabType)} value={activeTab}>
        <div className="flex items-center justify-between">
            <TabsList>
                <TabsTrigger value="workflows">Workflows</TabsTrigger>

                <TabsTrigger value="configurations">Configurations</TabsTrigger>
            </TabsList>

            {actions}
        </div>

        <TabsContent className="pt-2" value="workflows">
            {workflowsContent}
        </TabsContent>

        <TabsContent className="pt-2" value="configurations">
            {configurationsContent}
        </TabsContent>
    </Tabs>
);

export default IntegrationListItemTabs;
