import {render, windowResizeObserver} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import IntegrationInstanceConfigurationDialogBasicStepIntegrationVersionsSelect from '../IntegrationInstanceConfigurationDialogBasicStepIntegrationVersionsSelect';

const hoisted = vi.hoisted(() => ({
    integrationVersions: [] as {status: string; version: number}[],
}));

vi.mock('@/ee/shared/queries/embedded/integrationVersions.queries', () => ({
    useGetIntegrationVersionsQuery: () => ({data: hoisted.integrationVersions}),
}));

describe('IntegrationInstanceConfigurationDialogBasicStepIntegrationVersionsSelect', () => {
    beforeEach(() => {
        windowResizeObserver();

        hoisted.integrationVersions = [
            {status: 'PUBLISHED', version: 1},
            {status: 'PUBLISHED', version: 3},
            {status: 'DRAFT', version: 4},
            {status: 'PUBLISHED', version: 2},
        ];
    });

    it('selects the latest published version when none is chosen', () => {
        const onChange = vi.fn();

        render(
            <IntegrationInstanceConfigurationDialogBasicStepIntegrationVersionsSelect
                integrationId={1}
                onChange={onChange}
            />
        );

        expect(onChange).toHaveBeenCalledWith(3);
    });

    it('selects the latest other published version when the current one is excluded', () => {
        const onChange = vi.fn();

        render(
            <IntegrationInstanceConfigurationDialogBasicStepIntegrationVersionsSelect
                excludedIntegrationVersion={3}
                integrationId={1}
                onChange={onChange}
            />
        );

        expect(onChange).toHaveBeenCalledWith(2);
    });

    it('keeps a version that is already chosen', () => {
        const onChange = vi.fn();

        render(
            <IntegrationInstanceConfigurationDialogBasicStepIntegrationVersionsSelect
                integrationId={1}
                integrationVersion={1}
                onChange={onChange}
            />
        );

        expect(onChange).not.toHaveBeenCalled();
    });

    it('selects the draft when no version is published', () => {
        const onChange = vi.fn();

        hoisted.integrationVersions = [{status: 'DRAFT', version: 1}];

        render(
            <IntegrationInstanceConfigurationDialogBasicStepIntegrationVersionsSelect
                integrationId={1}
                onChange={onChange}
            />
        );

        expect(onChange).toHaveBeenCalledWith(1);
    });

    it('does not offer the draft once a version is published', () => {
        const onChange = vi.fn();

        hoisted.integrationVersions = [
            {status: 'PUBLISHED', version: 1},
            {status: 'DRAFT', version: 2},
        ];

        render(
            <IntegrationInstanceConfigurationDialogBasicStepIntegrationVersionsSelect
                integrationId={1}
                onChange={onChange}
            />
        );

        expect(onChange).toHaveBeenCalledWith(1);
        expect(onChange).not.toHaveBeenCalledWith(2);
    });
});
