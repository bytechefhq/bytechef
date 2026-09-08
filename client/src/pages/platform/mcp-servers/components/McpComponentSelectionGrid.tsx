import {Input} from '@/components/Input/Input';
import LoadingIcon from '@/components/LoadingIcon';
import {Card, CardContent, CardDescription, CardHeader, CardTitle} from '@/components/ui/card';
import {PackageIcon} from 'lucide-react';
import InlineSVG from 'react-inlinesvg';

/**
 * The fields a component card renders. Kept structural rather than tied to one definition type: the automation
 * dialog receives REST ComponentDefinitionBasic values and the embedded one GraphQL ComponentDefinition values.
 */
export interface McpSelectableComponentI {
    clusterElementsCount?: {[key: string]: number} | null;
    description?: string | null;
    icon?: string | null;
    name: string;
    title?: string | null;
    version: number | null;
}

interface McpComponentSelectionGridProps<T extends McpSelectableComponentI> {
    components: T[];
    isLoading: boolean;
    onComponentSelect: (component: T) => void;
    onSearchTermChange: (searchTerm: string) => void;
    searchTerm: string;
}

const McpComponentSelectionGrid = <T extends McpSelectableComponentI>({
    components,
    isLoading,
    onComponentSelect,
    onSearchTermChange,
    searchTerm,
}: McpComponentSelectionGridProps<T>) => (
    <div className="py-4">
        <div className="mb-4">
            <Input
                className="w-full"
                onChange={(event) => onSearchTermChange(event.target.value)}
                placeholder="Search components by name..."
                value={searchTerm}
            />
        </div>

        <div className="grid min-h-96 grid-cols-1 content-start gap-4 md:grid-cols-2 lg:grid-cols-3">
            {isLoading ? (
                <div className="col-span-full flex min-h-96 items-center justify-center">
                    <LoadingIcon className="size-6" />
                </div>
            ) : (
                components.map((component) => (
                    <Card
                        className="cursor-pointer transition-shadow hover:shadow-md"
                        key={`${component.name}-${component.version}`}
                        onClick={() => onComponentSelect(component)}
                    >
                        <CardHeader className="pb-2 text-center">
                            <div className="mx-auto mb-2">
                                {component.icon ? (
                                    <InlineSVG className="size-12" src={component.icon} />
                                ) : (
                                    <PackageIcon className="size-12 text-gray-400" />
                                )}
                            </div>

                            <CardTitle className="text-sm">{component.title || component.name}</CardTitle>
                        </CardHeader>

                        <CardContent className="pt-0">
                            <CardDescription className="line-clamp-3 text-center text-xs">
                                {component.description || 'No description available'}
                            </CardDescription>

                            <div className="mt-2 text-center text-xs text-muted-foreground">
                                {component.clusterElementsCount?.TOOLS && (
                                    <span>{component.clusterElementsCount.TOOLS} tools</span>
                                )}
                            </div>
                        </CardContent>
                    </Card>
                ))
            )}
        </div>
    </div>
);

export default McpComponentSelectionGrid;
