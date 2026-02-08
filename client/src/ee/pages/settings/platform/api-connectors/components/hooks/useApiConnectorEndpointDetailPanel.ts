import {toast} from '@/hooks/use-toast';
import {ApiConnectorEndpoint} from '@/shared/middleware/graphql';
import {useCallback, useMemo} from 'react';
import {parse as yamlParse, stringify as yamlStringify} from 'yaml';

import {useEndpointDetailPanelStore} from '../../stores/useEndpointDetailPanelStore';

interface UseApiConnectorEndpointDetailPanelI {
    apiConnectorName: string | undefined;
    calculateEditorHeight: (content: string | null) => string;
    closePanel: () => void;
    editorOptions: {
        automaticLayout: boolean;
        folding: boolean;
        lineNumbers: 'on';
        minimap: {enabled: boolean};
        readOnly: boolean;
        scrollBeyondLastLine: boolean;
        wordWrap: 'on';
    };
    endpointYaml: string | null;
    isOpen: boolean;
    selectedEndpoint: ApiConnectorEndpoint | undefined;
    specification: string | undefined;
}

export default function useApiConnectorEndpointDetailPanel(): UseApiConnectorEndpointDetailPanelI {
    const {apiConnectorName, closePanel, isOpen, selectedEndpoint, specification} = useEndpointDetailPanelStore();

    const extractEndpointSpec = useCallback(() => {
        if (!specification || !selectedEndpoint) {
            return null;
        }

        const path = selectedEndpoint.path;
        const method = selectedEndpoint.httpMethod?.toLowerCase();

        if (!path || !method) {
            return specification;
        }

        try {
            const parsedSpec = yamlParse(specification);

            if (!parsedSpec.paths?.[path]?.[method]) {
                return null;
            }

            const endpointSpec = {
                info: parsedSpec.info,
                openapi: parsedSpec.openapi,
                paths: {
                    [path]: {
                        [method]: parsedSpec.paths[path][method],
                    },
                },
                servers: parsedSpec.servers,
            };

            return yamlStringify(endpointSpec);
        } catch (error) {
            console.error('Failed to parse YAML specification when extracting endpoint spec:', error);

            toast({
                description: 'Could not extract endpoint details. Showing full specification instead.',
                title: 'Failed to parse specification',
                variant: 'destructive',
            });

            return specification;
        }
    }, [selectedEndpoint, specification]);

    const endpointYaml = useMemo(() => extractEndpointSpec(), [extractEndpointSpec]);

    const editorOptions = useMemo(
        () => ({
            automaticLayout: true,
            folding: true,
            lineNumbers: 'on' as const,
            minimap: {enabled: false},
            readOnly: true,
            scrollBeyondLastLine: false,
            wordWrap: 'on' as const,
        }),
        []
    );

    const calculateEditorHeight = useCallback((content: string | null) => {
        if (!content) {
            return '200px';
        }

        const lineCount = content.split('\n').length;
        const lineHeight = 19;
        const minHeight = 200;
        const maxHeight = 500;
        const calculatedHeight = Math.min(Math.max(lineCount * lineHeight, minHeight), maxHeight);

        return `${calculatedHeight}px`;
    }, []);

    return {
        apiConnectorName,
        calculateEditorHeight,
        closePanel,
        editorOptions,
        endpointYaml,
        isOpen,
        selectedEndpoint,
        specification,
    };
}
