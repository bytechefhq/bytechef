import {act, renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {parse as yamlParse} from 'yaml';

import {useApiConnectorWizardStore} from '../../../stores/useApiConnectorWizardStore';
import useApiConnectorAiPage from '../useApiConnectorAiPage';
import {API_CONNECTORS_PATH} from '../useImportApiConnector';

interface JobStatusQueryStateI {
    state: {data?: {generationJobStatus?: {status?: string}}};
}

interface JobStatusQueryOptionsI {
    enabled: boolean;
    refetchInterval: (query: JobStatusQueryStateI) => number | false;
}

interface JobStatusResultI {
    data?: {
        generationJobStatus: {
            errorMessage?: string | null;
            specification?: string | null;
            status: string;
        };
    };
    isError: boolean;
}

const hoisted = vi.hoisted(() => ({
    cancelMutateAsync: vi.fn(),
    importMutate: vi.fn(),
    importOptions: {} as {onSuccess?: () => void},
    importPending: false,
    invalidateQueries: vi.fn(),
    jobStatusOptions: undefined as unknown,
    jobStatusResult: {isError: false} as unknown,
    jobStatusVariables: undefined as unknown,
    navigate: vi.fn(),
    startMutate: vi.fn(),
    startOptions: {} as {
        onError?: (error: Error) => void;
        onSuccess?: (result: {startGenerateFromDocumentationPreview: {jobId?: string | null}}) => void;
    },
    startPending: false,
    toastError: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useCancelGenerationJobMutation: () => ({mutateAsync: hoisted.cancelMutateAsync}),
    useGenerationJobStatusQuery: (variables: unknown, options: unknown) => {
        hoisted.jobStatusVariables = variables;
        hoisted.jobStatusOptions = options;

        return hoisted.jobStatusResult;
    },
    useImportOpenApiSpecificationMutation: (options: {onSuccess?: () => void}) => {
        hoisted.importOptions = options;

        return {isPending: hoisted.importPending, mutate: hoisted.importMutate};
    },
    useStartGenerateFromDocumentationPreviewMutation: (options: typeof hoisted.startOptions) => {
        hoisted.startOptions = options;

        return {isPending: hoisted.startPending, mutate: hoisted.startMutate};
    },
}));

vi.mock('@tanstack/react-query', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@tanstack/react-query')>()),
    useQueryClient: () => ({invalidateQueries: hoisted.invalidateQueries}),
}));

vi.mock('react-router-dom', async (importOriginal) => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    useNavigate: () => hoisted.navigate,
}));

vi.mock('sonner', () => ({
    toast: {error: hoisted.toastError},
}));

const SPECIFICATION = `openapi: 3.0.0
info:
  title: Pets
  version: 1.0.0
paths:
  /pets:
    get:
      summary: List pets
      tags:
        - animals
    post:
      operationId: createPet
  /owners/{ownerId}:
    parameters:
      - name: ownerId
        in: path
        required: true
        schema:
          type: string
    get:
      summary: Get owner
    trace:
      summary: Unsupported method
  /:
    delete:
      summary: Root delete
`;

const getJobStatusOptions = () => hoisted.jobStatusOptions as JobStatusQueryOptionsI;

const setJobStatusResult = (jobStatusResult: JobStatusResultI) => {
    hoisted.jobStatusResult = jobStatusResult;
};

const setStoreState = (state: Partial<ReturnType<typeof useApiConnectorWizardStore.getState>>) => {
    act(() => {
        useApiConnectorWizardStore.setState(state);
    });
};

describe('useApiConnectorAiPage', () => {
    beforeEach(() => {
        useApiConnectorWizardStore.getState().reset();

        hoisted.cancelMutateAsync.mockReset();
        hoisted.importMutate.mockReset();
        hoisted.importPending = false;
        hoisted.invalidateQueries.mockReset();
        hoisted.jobStatusResult = {isError: false};
        hoisted.navigate.mockReset();
        hoisted.startMutate.mockReset();
        hoisted.startPending = false;
        hoisted.toastError.mockReset();
    });

    afterEach(() => {
        vi.useRealTimers();
        vi.restoreAllMocks();
    });

    it('resets the wizard store on mount', () => {
        useApiConnectorWizardStore.setState({currentStep: 2, name: 'Stale'});

        renderHook(() => useApiConnectorAiPage());

        expect(useApiConnectorWizardStore.getState().name).toBe('');
        expect(useApiConnectorWizardStore.getState().currentStep).toBe(0);
    });

    describe('canProceed', () => {
        it('requires a name and a documentation URL on the first step', () => {
            const {result} = renderHook(() => useApiConnectorAiPage());

            expect(result.current.canProceed).toBe(false);

            setStoreState({name: 'Pets'});

            expect(result.current.canProceed).toBe(false);

            setStoreState({documentationUrl: 'https://docs.example.com'});

            expect(result.current.canProceed).toBe(true);
        });

        it('requires at least one selected endpoint on the endpoint step', () => {
            const {result} = renderHook(() => useApiConnectorAiPage());

            setStoreState({currentStep: 1});

            expect(result.current.canProceed).toBe(false);

            setStoreState({selectedEndpointIds: ['/pets-get']});

            expect(result.current.canProceed).toBe(true);
        });

        it('always allows proceeding on the review step', () => {
            const {result} = renderHook(() => useApiConnectorAiPage());

            setStoreState({currentStep: 2});

            expect(result.current.canProceed).toBe(true);
        });
    });

    describe('handleNext', () => {
        it('does nothing on the first step when the name or documentation URL is missing', () => {
            const {result} = renderHook(() => useApiConnectorAiPage());

            setStoreState({name: 'Pets'});

            act(() => result.current.handleNext());

            expect(hoisted.startMutate).not.toHaveBeenCalled();
            expect(hoisted.toastError).not.toHaveBeenCalled();
            expect(useApiConnectorWizardStore.getState().isProcessing).toBe(false);
        });

        it.each(['not a url', 'ftp://docs.example.com'])('rejects the invalid documentation URL %s', (url) => {
            const {result} = renderHook(() => useApiConnectorAiPage());

            setStoreState({documentationUrl: url, name: 'Pets'});

            act(() => result.current.handleNext());

            expect(hoisted.toastError).toHaveBeenCalledWith('Invalid URL', expect.any(Object));
            expect(hoisted.startMutate).not.toHaveBeenCalled();
            expect(useApiConnectorWizardStore.getState().isProcessing).toBe(false);
        });

        it('starts the generation with blank optional fields omitted', () => {
            const {result} = renderHook(() => useApiConnectorAiPage());

            setStoreState({documentationUrl: 'http://docs.example.com', error: 'Old error', icon: '', name: 'Pets'});

            act(() => result.current.handleNext());

            expect(hoisted.startMutate).toHaveBeenCalledWith({
                input: {
                    documentationUrl: 'http://docs.example.com',
                    icon: undefined,
                    name: 'Pets',
                    userPrompt: undefined,
                },
            });
            expect(useApiConnectorWizardStore.getState().isProcessing).toBe(true);
            expect(useApiConnectorWizardStore.getState().error).toBeUndefined();
        });

        it('passes the icon and user prompt when they are set', () => {
            const {result} = renderHook(() => useApiConnectorAiPage());

            setStoreState({
                documentationUrl: 'https://docs.example.com',
                icon: '<svg />',
                name: 'Pets',
                userPrompt: 'Only the pets API',
            });

            act(() => result.current.handleNext());

            expect(hoisted.startMutate).toHaveBeenCalledWith({
                input: {
                    documentationUrl: 'https://docs.example.com',
                    icon: '<svg />',
                    name: 'Pets',
                    userPrompt: 'Only the pets API',
                },
            });
        });

        it('advances to the next step after the first step', () => {
            const {result} = renderHook(() => useApiConnectorAiPage());

            setStoreState({currentStep: 1});

            act(() => result.current.handleNext());

            expect(useApiConnectorWizardStore.getState().currentStep).toBe(2);
            expect(hoisted.startMutate).not.toHaveBeenCalled();
        });
    });

    describe('generation mutation callbacks', () => {
        it('stores the job id returned by the generation start', () => {
            renderHook(() => useApiConnectorAiPage());

            act(() => hoisted.startOptions.onSuccess?.({startGenerateFromDocumentationPreview: {jobId: 'job-1'}}));

            expect(useApiConnectorWizardStore.getState().jobId).toBe('job-1');
        });

        it('keeps the job id empty when the generation start returns none', () => {
            renderHook(() => useApiConnectorAiPage());

            act(() => hoisted.startOptions.onSuccess?.({startGenerateFromDocumentationPreview: {jobId: null}}));

            expect(useApiConnectorWizardStore.getState().jobId).toBeNull();
        });

        it('records the error and stops processing when the generation start fails', () => {
            renderHook(() => useApiConnectorAiPage());

            setStoreState({isProcessing: true});

            act(() => hoisted.startOptions.onError?.(new Error('Boom')));

            expect(useApiConnectorWizardStore.getState().error).toBe('Boom');
            expect(useApiConnectorWizardStore.getState().isProcessing).toBe(false);
        });
    });

    describe('job status polling', () => {
        it('polls only while a job id is set', () => {
            renderHook(() => useApiConnectorAiPage());

            expect(hoisted.jobStatusVariables).toEqual({jobId: ''});
            expect(getJobStatusOptions().enabled).toBe(false);

            setStoreState({jobId: 'job-1'});

            expect(hoisted.jobStatusVariables).toEqual({jobId: 'job-1'});
            expect(getJobStatusOptions().enabled).toBe(true);
        });

        it('stops polling once the job reaches a terminal status', () => {
            renderHook(() => useApiConnectorAiPage());

            const {refetchInterval} = getJobStatusOptions();

            for (const status of ['COMPLETED', 'FAILED', 'CANCELLED']) {
                expect(refetchInterval({state: {data: {generationJobStatus: {status}}}})).toBe(false);
            }
        });

        it('polls every second until five minutes have elapsed', () => {
            const dateNowSpy = vi.spyOn(Date, 'now').mockReturnValue(1_000_000);

            renderHook(() => useApiConnectorAiPage());

            const {refetchInterval} = getJobStatusOptions();
            const runningQuery = {state: {data: {generationJobStatus: {status: 'RUNNING'}}}};

            expect(refetchInterval(runningQuery)).toBe(1000);

            dateNowSpy.mockReturnValue(1_000_000 + 299_999);

            expect(refetchInterval(runningQuery)).toBe(1000);

            dateNowSpy.mockReturnValue(1_000_000 + 300_000);

            expect(refetchInterval(runningQuery)).toBe(false);

            dateNowSpy.mockReturnValue(2_000_000);

            expect(refetchInterval({state: {}})).toBe(1000);
        });

        it('applies a completed job: stores the specification, discovers and selects endpoints, advances', () => {
            const {rerender} = renderHook(() => useApiConnectorAiPage());

            setStoreState({isProcessing: true, jobId: 'job-1'});

            setJobStatusResult({
                data: {generationJobStatus: {specification: SPECIFICATION, status: 'COMPLETED'}},
                isError: false,
            });

            rerender();

            const state = useApiConnectorWizardStore.getState();

            expect(state.specification).toBe(SPECIFICATION);
            expect(state.discoveredEndpoints).toEqual([
                {id: '/pets-get', method: 'GET', path: '/pets', resource: 'Animals', summary: 'List pets'},
                {id: '/pets-post', method: 'POST', path: '/pets', resource: 'Pets', summary: undefined},
                {
                    id: '/owners/{ownerId}-get',
                    method: 'GET',
                    path: '/owners/{ownerId}',
                    resource: 'Owners',
                    summary: 'Get owner',
                },
                {id: '/-delete', method: 'DELETE', path: '/', resource: 'Other', summary: 'Root delete'},
            ]);
            expect(state.selectedEndpointIds).toEqual(['/pets-get', '/pets-post', '/owners/{ownerId}-get', '/-delete']);
            expect(state.isProcessing).toBe(false);
            expect(state.jobId).toBeNull();
            expect(state.currentStep).toBe(1);
        });

        it('reports an unparsable generated specification and discovers no endpoints', () => {
            vi.spyOn(console, 'error').mockImplementation(() => undefined);

            const {rerender} = renderHook(() => useApiConnectorAiPage());

            setStoreState({isProcessing: true, jobId: 'job-1'});

            setJobStatusResult({
                data: {generationJobStatus: {specification: 'paths: [unclosed', status: 'COMPLETED'}},
                isError: false,
            });

            rerender();

            expect(hoisted.toastError).toHaveBeenCalledWith('Failed to parse specification', expect.any(Object));
            expect(useApiConnectorWizardStore.getState().discoveredEndpoints).toEqual([]);
            expect(useApiConnectorWizardStore.getState().currentStep).toBe(1);
        });

        it('ignores a completed job without a specification', () => {
            const {rerender} = renderHook(() => useApiConnectorAiPage());

            setStoreState({isProcessing: true, jobId: 'job-1'});

            setJobStatusResult({
                data: {generationJobStatus: {specification: null, status: 'COMPLETED'}},
                isError: false,
            });

            rerender();

            expect(useApiConnectorWizardStore.getState().isProcessing).toBe(true);
            expect(useApiConnectorWizardStore.getState().currentStep).toBe(0);
        });

        it('records the error message of a failed job', () => {
            const {rerender} = renderHook(() => useApiConnectorAiPage());

            setStoreState({isProcessing: true, jobId: 'job-1'});

            setJobStatusResult({
                data: {generationJobStatus: {errorMessage: 'Docs unreachable', status: 'FAILED'}},
                isError: false,
            });

            rerender();

            expect(useApiConnectorWizardStore.getState().error).toBe('Docs unreachable');
            expect(useApiConnectorWizardStore.getState().isProcessing).toBe(false);
            expect(useApiConnectorWizardStore.getState().jobId).toBeNull();
        });

        it('falls back to a generic error for a failed job without a message', () => {
            const {rerender} = renderHook(() => useApiConnectorAiPage());

            setStoreState({isProcessing: true, jobId: 'job-1'});

            setJobStatusResult({data: {generationJobStatus: {errorMessage: null, status: 'FAILED'}}, isError: false});

            rerender();

            expect(useApiConnectorWizardStore.getState().error).toBe('Generation failed');
        });

        it('stops processing without an error for a cancelled job', () => {
            const {rerender} = renderHook(() => useApiConnectorAiPage());

            setStoreState({isProcessing: true, jobId: 'job-1'});

            setJobStatusResult({data: {generationJobStatus: {status: 'CANCELLED'}}, isError: false});

            rerender();

            expect(useApiConnectorWizardStore.getState().error).toBeUndefined();
            expect(useApiConnectorWizardStore.getState().isProcessing).toBe(false);
            expect(useApiConnectorWizardStore.getState().jobId).toBeNull();
        });

        it('reports a failing status query while processing', () => {
            const {rerender} = renderHook(() => useApiConnectorAiPage());

            setStoreState({isProcessing: true, jobId: 'job-1'});

            setJobStatusResult({isError: true});

            rerender();

            expect(useApiConnectorWizardStore.getState().error).toBe(
                'Failed to check generation status. Please try again.'
            );
            expect(useApiConnectorWizardStore.getState().isProcessing).toBe(false);
            expect(useApiConnectorWizardStore.getState().jobId).toBeNull();
        });

        it('ignores a failing status query when not processing', () => {
            const {rerender} = renderHook(() => useApiConnectorAiPage());

            setJobStatusResult({isError: true});

            rerender();

            expect(useApiConnectorWizardStore.getState().error).toBeUndefined();
        });

        it('times the generation out after five minutes of processing', () => {
            vi.useFakeTimers();

            renderHook(() => useApiConnectorAiPage());

            setStoreState({isProcessing: true, jobId: 'job-1'});

            act(() => {
                vi.advanceTimersByTime(299_999);
            });

            expect(useApiConnectorWizardStore.getState().isProcessing).toBe(true);

            act(() => {
                vi.advanceTimersByTime(1);
            });

            expect(useApiConnectorWizardStore.getState().error).toBe(
                'Generation timed out after 5 minutes. Please try again.'
            );
            expect(useApiConnectorWizardStore.getState().isProcessing).toBe(false);
            expect(useApiConnectorWizardStore.getState().jobId).toBeNull();
        });

        it('clears the timeout when processing stops before it fires', () => {
            vi.useFakeTimers();

            renderHook(() => useApiConnectorAiPage());

            setStoreState({isProcessing: true, jobId: 'job-1'});
            setStoreState({isProcessing: false});

            act(() => {
                vi.advanceTimersByTime(300_000);
            });

            expect(useApiConnectorWizardStore.getState().error).toBeUndefined();
            expect(useApiConnectorWizardStore.getState().jobId).toBe('job-1');
        });
    });

    describe('handleSave', () => {
        const discoveredEndpoints = [
            {id: '/pets-get', method: 'GET', path: '/pets', resource: 'Animals'},
            {id: '/pets-post', method: 'POST', path: '/pets', resource: 'Pets'},
            {id: '/owners/{ownerId}-get', method: 'GET', path: '/owners/{ownerId}', resource: 'Owners'},
            {id: '/-delete', method: 'DELETE', path: '/', resource: 'Other'},
        ];

        it('does nothing without a name or a specification', () => {
            const {result} = renderHook(() => useApiConnectorAiPage());

            setStoreState({name: 'Pets'});

            act(() => result.current.handleSave());

            expect(hoisted.importMutate).not.toHaveBeenCalled();
            expect(hoisted.toastError).not.toHaveBeenCalled();
        });

        it('refuses to save without a selected endpoint', () => {
            const {result} = renderHook(() => useApiConnectorAiPage());

            setStoreState({name: 'Pets', selectedEndpointIds: [], specification: SPECIFICATION});

            act(() => result.current.handleSave());

            expect(hoisted.toastError).toHaveBeenCalledWith('No endpoints selected', expect.any(Object));
            expect(hoisted.importMutate).not.toHaveBeenCalled();
        });

        it('imports the unmodified specification when every endpoint is selected', () => {
            const {result} = renderHook(() => useApiConnectorAiPage());

            setStoreState({
                discoveredEndpoints,
                icon: '',
                name: 'Pets',
                selectedEndpointIds: discoveredEndpoints.map((endpoint) => endpoint.id),
                specification: SPECIFICATION,
            });

            act(() => result.current.handleSave());

            expect(hoisted.importMutate).toHaveBeenCalledWith({
                input: {icon: undefined, name: 'Pets', specification: SPECIFICATION},
            });
        });

        it('imports only the selected operations when some endpoints are deselected', () => {
            const {result} = renderHook(() => useApiConnectorAiPage());

            setStoreState({
                discoveredEndpoints,
                icon: '<svg />',
                name: 'Pets',
                selectedEndpointIds: ['/pets-post'],
                specification: SPECIFICATION,
            });

            act(() => result.current.handleSave());

            const {input} = hoisted.importMutate.mock.calls[0][0];

            expect(input.icon).toBe('<svg />');
            expect(input.name).toBe('Pets');

            const filteredSpecification = yamlParse(input.specification);

            expect(filteredSpecification.info).toEqual({title: 'Pets', version: '1.0.0'});
            expect(filteredSpecification.paths).toEqual({'/pets': {post: {operationId: 'createPet'}}});
        });

        it('keeps path-level fields such as shared parameters for the selected operations', () => {
            const {result} = renderHook(() => useApiConnectorAiPage());

            setStoreState({
                discoveredEndpoints,
                name: 'Pets',
                selectedEndpointIds: ['/owners/{ownerId}-get'],
                specification: SPECIFICATION,
            });

            act(() => result.current.handleSave());

            const filteredSpecification = yamlParse(hoisted.importMutate.mock.calls[0][0].input.specification);

            expect(filteredSpecification.paths).toEqual({
                '/owners/{ownerId}': {
                    get: {summary: 'Get owner'},
                    parameters: [{in: 'path', name: 'ownerId', required: true, schema: {type: 'string'}}],
                },
            });
        });

        it('reports a specification that cannot be filtered', () => {
            vi.spyOn(console, 'error').mockImplementation(() => undefined);

            const {result} = renderHook(() => useApiConnectorAiPage());

            setStoreState({
                discoveredEndpoints,
                name: 'Pets',
                selectedEndpointIds: ['/pets-get'],
                specification: 'paths: [unclosed',
            });

            act(() => result.current.handleSave());

            expect(hoisted.toastError).toHaveBeenCalledWith('Error', expect.any(Object));
            expect(hoisted.importMutate).not.toHaveBeenCalled();
        });

        it('refreshes the connector list, resets the wizard and navigates back after the import', () => {
            renderHook(() => useApiConnectorAiPage());

            setStoreState({currentStep: 2, name: 'Pets'});

            act(() => hoisted.importOptions.onSuccess?.());

            expect(hoisted.invalidateQueries).toHaveBeenCalledWith({queryKey: ['apiConnectors']});
            expect(useApiConnectorWizardStore.getState().name).toBe('');
            expect(useApiConnectorWizardStore.getState().currentStep).toBe(0);
            expect(hoisted.navigate).toHaveBeenCalledWith(API_CONNECTORS_PATH);
        });
    });

    describe('handleCancel', () => {
        it('cancels a running generation job before leaving', async () => {
            hoisted.cancelMutateAsync.mockResolvedValue({cancelGenerationJob: true});

            const {result} = renderHook(() => useApiConnectorAiPage());

            setStoreState({isProcessing: true, jobId: 'job-1', name: 'Pets'});

            await act(async () => {
                await result.current.handleCancel();
            });

            expect(hoisted.cancelMutateAsync).toHaveBeenCalledWith({jobId: 'job-1'});
            expect(hoisted.toastError).not.toHaveBeenCalled();
            expect(useApiConnectorWizardStore.getState().name).toBe('');
            expect(hoisted.navigate).toHaveBeenCalledWith(API_CONNECTORS_PATH);
        });

        it('warns when the job cannot be cancelled and still leaves', async () => {
            vi.spyOn(console, 'error').mockImplementation(() => undefined);

            hoisted.cancelMutateAsync.mockRejectedValue(new Error('Network down'));

            const {result} = renderHook(() => useApiConnectorAiPage());

            setStoreState({isProcessing: true, jobId: 'job-1'});

            await act(async () => {
                await result.current.handleCancel();
            });

            expect(hoisted.toastError).toHaveBeenCalledWith('Failed to cancel generation', expect.any(Object));
            expect(useApiConnectorWizardStore.getState().jobId).toBeNull();
            expect(hoisted.navigate).toHaveBeenCalledWith(API_CONNECTORS_PATH);
        });

        it('leaves without cancelling when no job is processing', async () => {
            const {result} = renderHook(() => useApiConnectorAiPage());

            setStoreState({isProcessing: false, jobId: 'job-1'});

            await act(async () => {
                await result.current.handleCancel();
            });

            expect(hoisted.cancelMutateAsync).not.toHaveBeenCalled();
            expect(hoisted.navigate).toHaveBeenCalledWith(API_CONNECTORS_PATH);
        });
    });

    it('is pending while either the import or the generation start is pending', () => {
        const {rerender, result} = renderHook(() => useApiConnectorAiPage());

        expect(result.current.isPending).toBe(false);

        hoisted.startPending = true;

        rerender();

        expect(result.current.isPending).toBe(true);

        hoisted.startPending = false;
        hoisted.importPending = true;

        rerender();

        expect(result.current.isPending).toBe(true);
    });

    it('exposes the store step navigation', () => {
        const {result} = renderHook(() => useApiConnectorAiPage());

        setStoreState({currentStep: 2});

        act(() => result.current.previousStep());

        expect(result.current.currentStep).toBe(1);
    });
});
