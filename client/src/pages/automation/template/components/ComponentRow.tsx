import Button from '@/components/Button/Button';
import LazyLoadSVG from '@/components/LazyLoadSVG/LazyLoadSVG';
import {ComponentDefinition, ConnectionDefinition} from '@/shared/middleware/graphql';
import {Dispatch, SetStateAction} from 'react';

interface TemplateImportComponentProps {
    componentDefinition: Omit<ComponentDefinition, 'connection' | 'description' | 'id'> & {
        connection?: Omit<ConnectionDefinition, 'authorizationRequired'> | null;
    };
    connectedComponents: string[];
    setConnectedComponents: Dispatch<SetStateAction<string[]>>;
}

const ComponentRow = ({
    componentDefinition,
    connectedComponents,
    setConnectedComponents,
}: TemplateImportComponentProps) => {
    const isConnected = (component: string) => connectedComponents.includes(component);

    return (
        <div className="flex items-center justify-between rounded-lg border border-muted p-2">
            <div className="flex items-center gap-3">
                <div className="flex size-8 items-center justify-center rounded">
                    <LazyLoadSVG className="size-5 flex-none" src={componentDefinition?.icon ?? ''} />
                </div>

                <span className="font-medium text-gray-900">{componentDefinition.title}</span>
            </div>

            {
                /* eslint-disable no-constant-binary-expression */ componentDefinition.connection && false && (
                    <Button
                        disabled={isConnected(componentDefinition.name)}
                        onClick={() => setConnectedComponents((prev) => [...prev, componentDefinition.name])}
                    >
                        {isConnected(componentDefinition.name) ? 'Connected' : 'Connect'}
                    </Button>
                )
            }
        </div>
    );
};

export default ComponentRow;
