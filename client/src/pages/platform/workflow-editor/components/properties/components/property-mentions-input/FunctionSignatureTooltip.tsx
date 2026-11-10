import {EvaluatorFunctionDefinition} from '@/shared/middleware/graphql';
import {useLayoutEffect, useMemo, useRef, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import {formatFunctionSignature, formatFunctionSignatureParts} from './functionSuggestionUtils';

interface FunctionSignatureTooltipProps {
    activeArgIndex: number;
    definitions: EvaluatorFunctionDefinition[];
}

const FunctionSignatureTooltip = ({activeArgIndex, definitions}: FunctionSignatureTooltipProps) => {
    const [wrappedSignatureKey, setWrappedSignatureKey] = useState<string>();

    const containerRef = useRef<HTMLDivElement>(null);

    const signatureKey = useMemo(() => definitions.map(formatFunctionSignature).join('|'), [definitions]);

    const isWrapped = wrappedSignatureKey === signatureKey;

    const sharedDescription = useMemo(() => {
        const descriptions = new Set(
            definitions.map((definition) => definition.parameters[activeArgIndex]?.description ?? '')
        );

        return descriptions.size === 1 ? [...descriptions][0] : undefined;
    }, [activeArgIndex, definitions]);

    useLayoutEffect(() => {
        if (isWrapped || !containerRef.current) {
            return;
        }

        const signatureElements = containerRef.current.querySelectorAll<HTMLElement>('[data-signature]');

        const overflows = [...signatureElements].some((element) => element.scrollWidth > element.clientWidth);

        if (overflows) {
            setWrappedSignatureKey(signatureKey);
        }
    }, [isWrapped, signatureKey]);

    return (
        <div
            className="max-w-sm space-y-2 rounded-md border bg-background p-2 font-mono text-xs shadow-md"
            ref={containerRef}
        >
            {definitions.map((definition, definitionIndex) => {
                const {params, returnType} = formatFunctionSignatureParts(definition);
                const activeParameterDescription = definition.parameters[activeArgIndex]?.description;

                return (
                    <div key={definitionIndex}>
                        <div className={twMerge(!isWrapped && 'overflow-hidden whitespace-nowrap')} data-signature>
                            <span className="text-primary">{definition.name}</span>

                            <span className="text-muted-foreground">(</span>

                            <span className={twMerge(isWrapped && 'block pl-4')} data-testid="signature-parameters">
                                {params.map((param, index) => (
                                    <span key={param}>
                                        <span
                                            className={twMerge(
                                                'whitespace-nowrap text-muted-foreground',
                                                index === activeArgIndex && 'font-bold text-foreground'
                                            )}
                                        >
                                            {param}
                                        </span>

                                        <span className="text-muted-foreground">
                                            {index < params.length - 1 ? ', ' : ''}
                                        </span>
                                    </span>
                                ))}
                            </span>

                            <span className="whitespace-nowrap text-muted-foreground">): {returnType}</span>
                        </div>

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
