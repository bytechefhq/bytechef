import LoadingIcon from '@/components/LoadingIcon';
import {Card, CardContent, CardDescription, CardHeader, CardTitle} from '@/components/ui/card';
import {Input} from '@/components/ui/input';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {PackageIcon} from 'lucide-react';
import InlineSVG from 'react-inlinesvg';

import useMcpComponentDialogComponentSelectionStep from './hooks/useMcpComponentDialogComponentSelectionStep';

interface ComponentSelectionStepProps {
    open: boolean;
    onComponentSelect: (component: ComponentDefinitionBasic) => void;
}

const McpComponentDialogComponentSelectionStep = ({onComponentSelect, open}: ComponentSelectionStepProps) => {
    const {filteredComponents, isLoadingComponents, searchTerm, setSearchTerm} =
        useMcpComponentDialogComponentSelectionStep(open);

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

            <div className="grid min-h-96 grid-cols-1 content-start gap-4 md:grid-cols-2 lg:grid-cols-3">
                {isLoadingComponents ? (
                    <div className="col-span-full flex min-h-96 items-center justify-center">
                        <LoadingIcon className="size-6" />
                    </div>
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
