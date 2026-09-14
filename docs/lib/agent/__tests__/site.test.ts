import { describe, expect, it } from 'vitest';
import {
  absoluteUrl,
  buildNotFoundMarkdown,
  normalizePathname,
  RECOVERY_LINKS,
} from '../site';

describe('buildNotFoundMarkdown', () => {
  it('names the missing path and links every recovery target as an absolute URL', () => {
    const markdown = buildNotFoundMarkdown('/platform/nope/');

    expect(markdown.startsWith('# 404: page not found')).toBe(true);
    expect(markdown).toContain('`/platform/nope`');

    for (const { href } of RECOVERY_LINKS) {
      expect(markdown).toContain(`](${absoluteUrl(href)})`);
    }

    expect(markdown).toContain('https://docs.bytechef.io/llms.txt');
    expect(markdown).toContain('https://docs.bytechef.io/openapi.json');
  });
});

describe('paths', () => {
  it('normalizes duplicate and trailing slashes', () => {
    expect(normalizePathname('platform//x/')).toBe('/platform/x');
    expect(normalizePathname('/')).toBe('/');
  });

  it('leaves external links alone when making URLs absolute', () => {
    expect(absoluteUrl('/reference')).toBe('https://docs.bytechef.io/reference');
    expect(absoluteUrl('https://www.bytechef.io')).toBe('https://www.bytechef.io');
  });
});
