import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';

import ComponentVisibilityTab from './components/ComponentVisibilityTab';

/**
 * Policies tab content for the Components page. Keeps its own sub-tab bar so additional policy types (beyond
 * component visibility) can slot in without another page restructure.
 */
const ComponentPoliciesTab = () => {
    return (
        <Tabs className="size-full px-6 pb-4" defaultValue="component-visibility">
            <TabsList variant="line">
                <TabsTrigger value="component-visibility">Component Visibility</TabsTrigger>
            </TabsList>

            <TabsContent value="component-visibility">
                <ComponentVisibilityTab />
            </TabsContent>
        </Tabs>
    );
};

export default ComponentPoliciesTab;
