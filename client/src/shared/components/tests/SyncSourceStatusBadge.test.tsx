import SyncSourceStatusBadge from '@/shared/components/SyncSourceStatusBadge';
import {render, screen} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

describe('SyncSourceStatusBadge', () => {
    it('renders BUILDING_PREVIEW with yellow style and label "Building Preview"', () => {
        render(<SyncSourceStatusBadge status="BUILDING_PREVIEW" />);

        const badge = screen.getByText('Building Preview');

        expect(badge).toBeInTheDocument();
        expect(badge.className).toContain('bg-yellow-100');
        expect(badge.className).toContain('text-yellow-800');
    });

    it('renders DISABLED with gray style and label "Disabled"', () => {
        render(<SyncSourceStatusBadge status="DISABLED" />);

        const badge = screen.getByText('Disabled');

        expect(badge).toBeInTheDocument();
        expect(badge.className).toContain('bg-surface-neutral-secondary');
        expect(badge.className).toContain('text-content-neutral-primary');
    });

    it('renders FAILED with red style and label "Failed"', () => {
        render(<SyncSourceStatusBadge status="FAILED" />);

        const badge = screen.getByText('Failed');

        expect(badge).toBeInTheDocument();
        expect(badge.className).toContain('bg-surface-destructive-secondary');
        expect(badge.className).toContain('text-content-destructive');
    });

    it('renders PREVIEW with blue style and label "Preview"', () => {
        render(<SyncSourceStatusBadge status="PREVIEW" />);

        const badge = screen.getByText('Preview');

        expect(badge).toBeInTheDocument();
        expect(badge.className).toContain('bg-surface-brand-secondary');
        expect(badge.className).toContain('text-content-brand-primary');
    });

    it('renders READY with green style and label "Ready"', () => {
        render(<SyncSourceStatusBadge status="READY" />);

        const badge = screen.getByText('Ready');

        expect(badge).toBeInTheDocument();
        expect(badge.className).toContain('bg-surface-success-secondary');
        expect(badge.className).toContain('text-content-success-primary');
    });

    it('renders unknown status with gray fallback and the status string as label', () => {
        render(<SyncSourceStatusBadge status="UNKNOWN_STATE" />);

        const badge = screen.getByText('UNKNOWN_STATE');

        expect(badge).toBeInTheDocument();
        expect(badge.className).toContain('bg-surface-neutral-secondary');
    });
});
