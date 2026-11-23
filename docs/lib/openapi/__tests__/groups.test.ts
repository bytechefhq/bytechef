import { describe, expect, it } from 'vitest';
import { GROUP_META, planGroups } from '../groups';

describe('OpenAPI reference groups', () => {
  it('has a GROUP_META entry for every group the published specs declare', async () => {
    const { groups } = await planGroups();

    const undocumented = groups.filter(({ metaKey }) => !GROUP_META[metaKey]).map(({ metaKey }) => metaKey);

    expect(undocumented).toEqual([]);
  });

  it('has no GROUP_META entry left over from a tag the specs no longer declare', async () => {
    const { groups } = await planGroups();

    const metaKeys = new Set(groups.map(({ metaKey }) => metaKey));

    expect(Object.keys(GROUP_META).filter((metaKey) => !metaKeys.has(metaKey))).toEqual([]);
  });
});
