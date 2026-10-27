import {EvaluatorFunctionDefinition} from '@/shared/middleware/graphql';
import {useMemo} from 'react';
import {twMerge} from 'tailwind-merge';

import {formatFunctionSignatureParts} from './functionSuggestionUtils';

interface FunctionSignatureTooltipProps {
    activeArgIndex: number;
    definitions: EvaluatorFunctionDefinition[];
}

const FunctionSignatureTooltip = ({activeArgIndex, definitions}: FunctionSignatureTooltipProps) => {
    const sharedDescription = useMemo(() => {
        const descriptions = new Set(
            definitions.map((definition) => definition.parameters[activeArgIndex]?.description ?? '')
        );

        return descriptions.size === 1 ? [...descriptions][0] : undefined;
    }, [activeArgIndex, definitions]);

    return (
        <div className="max-w-sm space-y-1 rounded-md border bg-background p-2 font-mono text-xs shadow-md">
            {definitions.map((definition, definitionIndex) => {
                const {params, returnType} = formatFunctionSignatureParts(definition);
                const activeParameterDescription = definition.parameters[activeArgIndex]?.description;

                return (
                    <div key={definitionIndex}>
                        <span className="text-primary">{definition.name}</span>

                        <span className="text-muted-foreground">(</span>

                        {params.map((param, index) => (
                            <span key={param}>
                                <span
                                    className={twMerge(
                                        'text-muted-foreground',
                                        index === activeArgIndex && 'font-bold text-foreground'
                                    )}
                                >
                                    {param}
                                </span>

                                <span className="text-muted-foreground">{index < params.length - 1 ? ', ' : ''}</span>
                            </span>
                        ))}

                        <span className="text-muted-foreground">): {returnType}</span>

                        {sharedDescription === undefined && activeParameterDescription && (
                            <div className="font-sans whitespace-normal text-muted-foreground">
                                {activeParameterDescription}
                            </div>
                        )}
                    </div>
                );
            })}

            {sharedDescription && (
                <div className="font-sans whitespace-normal text-muted-foreground">{sharedDescription}</div>
            )}
        </div>
    );
};

export default FunctionSignatureTooltip;
