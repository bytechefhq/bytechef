import {EvaluatorFunctionDefinition} from '@/shared/middleware/graphql';
import {twMerge} from 'tailwind-merge';

import {formatFunctionSignatureParts} from './functionSuggestionUtils';

interface FunctionSignatureTooltipProps {
    activeArgIndex: number;
    definition: EvaluatorFunctionDefinition;
}

const FunctionSignatureTooltip = ({activeArgIndex, definition}: FunctionSignatureTooltipProps) => {
    const {params, returnType} = formatFunctionSignatureParts(definition);

    return (
        <div className="max-w-sm rounded-md border bg-background p-2 font-mono text-xs shadow-md">
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

            {definition.parameters[activeArgIndex]?.description && (
                <div className="mt-1 font-sans whitespace-normal text-muted-foreground">
                    {definition.parameters[activeArgIndex].description}
                </div>
            )}
        </div>
    );
};

export default FunctionSignatureTooltip;
