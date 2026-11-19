import VersionHistoryTimeline, {VersionHistoryItemI} from '@/shared/components/version-history/VersionHistoryTimeline';
import {render, screen} from '@/shared/util/test-utils';
import {expect, it} from 'vitest';

const draftItem: VersionHistoryItemI = {published: false, version: 5};

const publishedItem: VersionHistoryItemI = {
    description: 'First release',
    published: true,
    publishedDate: new Date(2026, 9, 7, 16, 25),
    version: 4,
};

const renderTimeline = (items: VersionHistoryItemI[], unpublishedChanges?: boolean) =>
    render(
        <VersionHistoryTimeline
            ariaLabel="Project versions"
            items={items}
            renderChanges={() => null}
            unpublishedChanges={unpublishedChanges}
        />
    );

it('shows the draft above the published versions when the draft has unpublished changes', () => {
    renderTimeline([draftItem, publishedItem], true);

    expect(screen.getByText('V5')).toBeInTheDocument();
    expect(screen.getByText('Draft')).toBeInTheDocument();
    expect(screen.getByText('V4')).toBeInTheDocument();
});

it('hides the draft when it matches the last published version', () => {
    renderTimeline([draftItem, publishedItem], false);

    expect(screen.queryByText('V5')).not.toBeInTheDocument();
    expect(screen.queryByText('Draft')).not.toBeInTheDocument();
    expect(screen.getByText('V4')).toBeInTheDocument();
    expect(screen.getByText('Published')).toBeInTheDocument();
});

it('keeps the draft when nothing was published yet', () => {
    renderTimeline([draftItem], false);

    expect(screen.getByText('V5')).toBeInTheDocument();
    expect(screen.getByText('Draft')).toBeInTheDocument();
});

it('keeps the draft when it is not known whether the draft has unpublished changes', () => {
    renderTimeline([draftItem, publishedItem]);

    expect(screen.getByText('V5')).toBeInTheDocument();
});
