import {Card, CardContent, CardDescription, CardHeader, CardTitle} from '@/components/ui/card';
import {Input} from '@/components/ui/input';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {Package} from 'lucide-react';
import React, {useState} from 'react';

interface ComponentSelectionStepProps {
    open: boolean;
    onComponentSelect: (component: ComponentDefinitionBasic) => void;
}

const McpComponentDialogComponentSelectionStep = ({onComponentSelect, open}: ComponentSelectionStepProps) => {
    const [searchTerm, setSearchTerm] = useState('');

    const {data: components = [], isLoading: isLoadingComponents} = useGetComponentDefinitionsQuery(
        {
            actionDefinitions: true,
        },
        open
    );

    const filteredComponents = components.filter((component) => {
        const hasTools = component.clusterElementsCount?.TOOLS && component.clusterElementsCount.TOOLS > 0;
        if (!hasTools) return false;

        const searchLower = searchTerm.toLowerCase();
        const nameMatch = component.name.toLowerCase().includes(searchLower);
        const titleMatch = component.title?.toLowerCase().includes(searchLower);
        return nameMatch || titleMatch;
    });

    return (
        <div className="py-4">
            <div className="mb-4">
                <Input
                    className="w-full"
                    onChange={(e) => setSearchTerm(e.target.value)}
                    placeholder="Search components by name..."
                    value={searchTerm}
                />
            </div>

            <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
                {isLoadingComponents ? (
                    <div className="col-span-full py-8 text-center">Loading components...</div>
                ) : (
                    filteredComponents.map((component) => (
                        <Card
                            className="cursor-pointer transition-shadow hover:shadow-md"
                            key={`${component.name}-${component.version}`}
                            onClick={() => onComponentSelect(component)}
                        >
                            <CardHeader className="pb-2 text-center">
                                <div className="mx-auto mb-2">
                                    {component.icon ? (
                                        <img
                                            alt={component.title || component.name}
                                            className="size-12 object-contain"
                                            src={component.icon}
                                        />
                                    ) : (
                                        <Package className="size-12 text-gray-400" />
                                    )}
                                </div>

                                <CardTitle className="text-sm">{component.title || component.name}</CardTitle>
                            </CardHeader>

                            <CardContent className="pt-0">
                                <CardDescription className="line-clamp-3 text-center text-xs">
                                    {component.description || 'No description available'}
                                </CardDescription>

                                <div className="mt-2 text-center text-xs text-muted-foreground">
                                    {component.clusterElementsCount?.TOOL && (
                                        <span>{component.clusterElementsCount.TOOL} tools</span>
                                    )}
                                </div>
                            </CardContent>
                        </Card>
                    ))
                )}
            </div>
        </div>
    );
};

export default McpComponentDialogComponentSelectionStep;
