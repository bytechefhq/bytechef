import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Empty} from '@/components/ui/empty';
import {Label} from '@/components/ui/label';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {LayersIcon, PlusIcon, RefreshCwIcon} from 'lucide-react';
import {ReactNode} from 'react';

export type ConfigurationDialogTabType = 'change-version' | 'new-configuration';

export interface ConfigurationDialogTabsItemI {
    label: string;
    value: string;
}

interface ConfigurationDialogTabsProps {
    activeTab: ConfigurationDialogTabType;
    changeVersionContent?: ReactNode;
    configurationItems: ConfigurationDialogTabsItemI[];
    emptyTitle: string;
    newConfigurationContent: ReactNode;
    onConfigurationSelect: (value: string) => void;
    onTabChange: (tab: ConfigurationDialogTabType) => void;
    selectedConfigurationValue?: string;
}

const ConfigurationDialogTabs = ({
    activeTab,
    changeVersionContent,
    configurationItems,
    emptyTitle,
    newConfigurationContent,
    onConfigurationSelect,
    onTabChange,
    selectedConfigurationValue,
}: ConfigurationDialogTabsProps) => (
    <Tabs onValueChange={(value) => onTabChange(value as ConfigurationDialogTabType)} value={activeTab}>
        <TabsList className="flex">
            <TabsTrigger className="w-full gap-2" value="new-configuration">
                <PlusIcon className="size-4" />

                <span>New</span>
            </TabsTrigger>

            <TabsTrigger className="w-full gap-2" value="change-version">
                <RefreshCwIcon className="size-4" />

                <span>Change Version</span>
            </TabsTrigger>
        </TabsList>

        <TabsContent className="m-0 flex flex-col gap-4 pt-4" value="new-configuration">
            {newConfigurationContent}
        </TabsContent>

        <TabsContent className="m-0 flex flex-col gap-4 pt-4" value="change-version">
            {configurationItems.length > 0 ? (
                <>
                    <fieldset className="flex flex-col gap-2 border-0 p-0">
                        <Label>Configuration</Label>

                        <Select onValueChange={onConfigurationSelect} value={selectedConfigurationValue}>
                            <SelectTrigger aria-label="Configuration" className="w-full">
                                <SelectValue placeholder="Select configuration" />
                            </SelectTrigger>

                            <SelectContent>
                                {configurationItems.map((configurationItem) => (
                                    <SelectItem key={configurationItem.value} value={configurationItem.value}>
                                        {configurationItem.label}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </fieldset>

                    {selectedConfigurationValue && changeVersionContent}
                </>
            ) : (
                <Empty>
                    <div className="flex items-center justify-center rounded-lg bg-surface-neutral-secondary p-2">
                        <LayersIcon />
                    </div>

                    <div className="flex flex-col items-center gap-2">
                        <h2 className="text-xl">{emptyTitle}</h2>

                        <p className="text-sm text-content-neutral-secondary">Head to the "New" tab to create one.</p>
                    </div>
                </Empty>
            )}
        </TabsContent>
    </Tabs>
);

export default ConfigurationDialogTabs;
