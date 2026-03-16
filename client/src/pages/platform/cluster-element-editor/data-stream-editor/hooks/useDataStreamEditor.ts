import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {useCallback, useEffect, useMemo, useState} from 'react';
import {useShallow} from 'zustand/shallow';

export const TOTAL_STEPS = 4;

interface UseDataStreamEditorResultI {
    configuredSteps: Set<number>;
    currentStep: number;
    handleGoToStep: (step: number) => void;
    handleNext: () => void;
    handlePrevious: () => void;
}

export default function useDataStreamEditor(): UseDataStreamEditorResultI {
    const [currentStep, setCurrentStep] = useState(0);

    const {rootClusterElementNodeData, setMainClusterRootComponentDefinition} = useWorkflowEditorStore(
        useShallow((state) => ({
            rootClusterElementNodeData: state.rootClusterElementNodeData,
            setMainClusterRootComponentDefinition: state.setMainClusterRootComponentDefinition,
        }))
    );

    const mainClusterRootQueryParameters = useMemo(() => {
        if (!rootClusterElementNodeData?.type || !rootClusterElementNodeData?.componentName) {
            return {componentName: '', componentVersion: 1};
        }

        return {
            componentName: rootClusterElementNodeData.componentName,
            componentVersion: Number(rootClusterElementNodeData.type?.split('/')[1]?.replace(/^v/, '')) || 1,
        };
    }, [rootClusterElementNodeData?.type, rootClusterElementNodeData?.componentName]);

    const {data: rootClusterElementDefinition} = useGetComponentDefinitionQuery(
        mainClusterRootQueryParameters,
        !!rootClusterElementNodeData?.workflowNodeName
    );

    useEffect(() => {
        if (rootClusterElementDefinition && rootClusterElementNodeData?.workflowNodeName) {
            setMainClusterRootComponentDefinition(rootClusterElementDefinition);
        }
    }, [
        rootClusterElementDefinition,
        rootClusterElementNodeData?.workflowNodeName,
        setMainClusterRootComponentDefinition,
    ]);

    const configuredSteps = useMemo(() => {
        const clusterElements = rootClusterElementNodeData?.clusterElements;

        if (!clusterElements || Array.isArray(clusterElements)) {
            return new Set<number>();
        }

        const steps = new Set<number>();

        if (clusterElements['source']) {
            steps.add(0);
        }

        if (clusterElements['destination']) {
            steps.add(1);
        }

        if (clusterElements['processor']) {
            steps.add(2);
        }

        return steps;
    }, [rootClusterElementNodeData?.clusterElements]);

    const handleNext = useCallback(() => {
        setCurrentStep((previousStep) => Math.min(previousStep + 1, TOTAL_STEPS - 1));
    }, []);

    const handlePrevious = useCallback(() => {
        setCurrentStep((previousStep) => Math.max(previousStep - 1, 0));
    }, []);

    const handleGoToStep = useCallback((step: number) => {
        if (step >= 0 && step < TOTAL_STEPS) {
            setCurrentStep(step);
        }
    }, []);

    return {
        configuredSteps,
        currentStep,
        handleGoToStep,
        handleNext,
        handlePrevious,
    };
}
