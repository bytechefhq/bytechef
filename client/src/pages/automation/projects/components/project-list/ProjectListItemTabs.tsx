import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {ReactNode} from 'react';

export type ProjectListItemTabType = 'agents' | 'deployments' | 'workflows';

interface ProjectListItemTabsProps {
    actions?: ReactNode;
    activeTab: ProjectListItemTabType;
    agentCount: number;
    agentsContent: ReactNode;
    deploymentsContent: ReactNode;
    onTabChange: (tab: ProjectListItemTabType) => void;
    workflowCount: number;
    workflowsContent: ReactNode;
}

const ProjectListItemTabs = ({
    actions,
    activeTab,
    agentCount,
    agentsContent,
    deploymentsContent,
    onTabChange,
    workflowCount,
    workflowsContent,
}: ProjectListItemTabsProps) => (
    <Tabs onValueChange={(value) => onTabChange(value as ProjectListItemTabType)} value={activeTab}>
        <div className="flex items-center justify-between">
            <TabsList>
                <TabsTrigger value="workflows">Workflows ({workflowCount})</TabsTrigger>

                <TabsTrigger value="agents">Agents ({agentCount})</TabsTrigger>

                <TabsTrigger value="deployments">Deployments</TabsTrigger>
            </TabsList>

            {actions}
        </div>

        <TabsContent className="pt-2" value="workflows">
            {workflowsContent}
        </TabsContent>

        <TabsContent className="pt-2" value="agents">
            {agentsContent}
        </TabsContent>

        <TabsContent className="pt-2" value="deployments">
            {deploymentsContent}
        </TabsContent>
    </Tabs>
);

export default ProjectListItemTabs;
