import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {ReactNode} from 'react';

export type AutomationProjectListItemTabType = 'configurations' | 'workflows';

interface AutomationProjectListItemTabsProps {
    actions?: ReactNode;
    activeTab: AutomationProjectListItemTabType;
    configurationsContent: ReactNode;
    onTabChange: (tab: AutomationProjectListItemTabType) => void;
    workflowsContent: ReactNode;
}

const AutomationProjectListItemTabs = ({
    actions,
    activeTab,
    configurationsContent,
    onTabChange,
    workflowsContent,
}: AutomationProjectListItemTabsProps) => (
    <Tabs onValueChange={(value) => onTabChange(value as AutomationProjectListItemTabType)} value={activeTab}>
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

export default AutomationProjectListItemTabs;
