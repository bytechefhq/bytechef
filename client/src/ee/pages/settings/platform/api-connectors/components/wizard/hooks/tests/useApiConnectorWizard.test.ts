import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

import {useApiConnectorWizardStore} from '../../../../stores/useApiConnectorWizardStore';
import useApiConnectorWizard from '../useApiConnectorWizard';

describe('useApiConnectorWizard', () => {
    beforeEach(() => {
        useApiConnectorWizardStore.getState().reset();
    });

    it('initializes the form from the wizard store', () => {
        useApiConnectorWizardStore.setState({
            baseUrl: 'https://api.example.com',
            documentationUrl: 'https://docs.example.com',
            icon: '<svg />',
            name: 'Pets',
            specification: 'openapi: 3.0.0',
            userPrompt: 'Only pets',
        });

        const {result} = renderHook(() => useApiConnectorWizard('basic'));

        expect(result.current.form.getValues()).toEqual({
            baseUrl: 'https://api.example.com',
            documentationUrl: 'https://docs.example.com',
            icon: '<svg />',
            name: 'Pets',
            specification: 'openapi: 3.0.0',
            userPrompt: 'Only pets',
        });
        expect(result.current.control).toBe(result.current.form.control);
    });

    it('initializes unset store values as empty strings', () => {
        const {result} = renderHook(() => useApiConnectorWizard('basic'));

        expect(result.current.form.getValues()).toEqual({
            baseUrl: '',
            documentationUrl: '',
            icon: '',
            name: '',
            specification: '',
            userPrompt: '',
        });
    });

    it('syncs the name, icon and base URL of the basic form', () => {
        const {result} = renderHook(() => useApiConnectorWizard('basic'));

        act(() => {
            result.current.form.setValue('name', 'Pets');
            result.current.form.setValue('icon', '<svg />');
            result.current.form.setValue('baseUrl', 'https://api.example.com');
            result.current.form.setValue('specification', 'openapi: 3.0.0');
            result.current.form.setValue('documentationUrl', 'https://docs.example.com');
            result.current.form.setValue('userPrompt', 'Only pets');
        });

        const state = useApiConnectorWizardStore.getState();

        expect(state.name).toBe('Pets');
        expect(state.icon).toBe('<svg />');
        expect(state.baseUrl).toBe('https://api.example.com');
        expect(state.specification).toBeUndefined();
        expect(state.documentationUrl).toBeUndefined();
        expect(state.userPrompt).toBeUndefined();
    });

    it('syncs the specification of the import form', () => {
        const {result} = renderHook(() => useApiConnectorWizard('import'));

        act(() => {
            result.current.form.setValue('name', 'Pets');
            result.current.form.setValue('specification', 'openapi: 3.0.0');
            result.current.form.setValue('baseUrl', 'https://api.example.com');
            result.current.form.setValue('documentationUrl', 'https://docs.example.com');
        });

        const state = useApiConnectorWizardStore.getState();

        expect(state.name).toBe('Pets');
        expect(state.specification).toBe('openapi: 3.0.0');
        expect(state.baseUrl).toBeUndefined();
        expect(state.documentationUrl).toBeUndefined();
    });

    it('syncs the documentation URL and prompt of the documentation form', () => {
        const {result} = renderHook(() => useApiConnectorWizard('docUrl'));

        act(() => {
            result.current.form.setValue('icon', '<svg />');
            result.current.form.setValue('documentationUrl', 'https://docs.example.com');
            result.current.form.setValue('userPrompt', 'Only pets');
            result.current.form.setValue('baseUrl', 'https://api.example.com');
            result.current.form.setValue('specification', 'openapi: 3.0.0');
        });

        const state = useApiConnectorWizardStore.getState();

        expect(state.icon).toBe('<svg />');
        expect(state.documentationUrl).toBe('https://docs.example.com');
        expect(state.userPrompt).toBe('Only pets');
        expect(state.baseUrl).toBeUndefined();
        expect(state.specification).toBeUndefined();
    });

    it('stops syncing after unmount', () => {
        const {result, unmount} = renderHook(() => useApiConnectorWizard('basic'));

        const {form} = result.current;

        unmount();

        form.setValue('name', 'Pets');

        expect(useApiConnectorWizardStore.getState().name).toBe('');
    });
});
