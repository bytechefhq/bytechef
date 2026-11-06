import {Input} from '@/components/Input/Input';
import {Card, CardContent, CardDescription, CardHeader, CardTitle} from '@/components/ui/card';
import {Skeleton} from '@/components/ui/skeleton';
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
    id?: string;
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

        <div
            aria-busy={isLoading}
            className="grid min-h-96 grid-cols-1 content-start gap-4 md:grid-cols-2 lg:grid-cols-3"
        >
            {isLoading ? (
                Array.from({length: 6}, (_, index) => (
                    <Card aria-hidden="true" data-testid="mcp-component-card-skeleton" key={index}>
                        <CardHeader className="justify-items-center pb-2">
                            <Skeleton className="mb-2 size-12 rounded-md" />

                            <Skeleton className="h-4 w-24" />
                        </CardHeader>

                        <CardContent className="flex flex-col items-center gap-1.5 pt-0">
                            <Skeleton className="h-3 w-full" />

                            <Skeleton className="h-3 w-4/5" />

                            <Skeleton className="h-3 w-3/5" />

                            <Skeleton className="mt-2 h-3 w-12" />
                        </CardContent>
                    </Card>
                ))
            ) : components.length === 0 ? (
                <div className="col-span-full flex min-h-96 flex-col items-center justify-center gap-2 text-center">
                    <PackageIcon className="size-10 text-muted-foreground" />

                    <p className="text-sm font-medium">No components found</p>

                    <p className="text-sm text-muted-foreground">
                        {searchTerm
                            ? `No components match "${searchTerm}". Try a different search term.`
                            : 'There are no components with tools available.'}
                    </p>
                </div>
            ) : (
                components.map((component) => (
                    <Card
                        className="cursor-pointer transition-shadow hover:shadow-md"
                        key={component.id ?? `${component.name}-${component.version}`}
                        onClick={() => onComponentSelect(component)}
                    >
                        <CardHeader className="pb-2 text-center">
                            <div className="mx-auto mb-2">
                                {component.icon ? (
                                    <InlineSVG className="size-12" src={component.icon} />
                                ) : (
                                    <PackageIcon className="size-12 text-content-neutral-tertiary" />
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
