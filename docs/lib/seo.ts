// Search engines truncate titles past ~70 characters and prefer descriptions of 120-170.
export const MAX_TITLE_LENGTH = 70;
export const MAX_DESCRIPTION_LENGTH = 170;
export const MIN_DESCRIPTION_LENGTH = 120;

const MIN_TITLE_ROOM = 20;

const VERSION_SUFFIX = /_v(\d+)$/;

export type ReferenceKindType = 'Component' | 'Flow Control';

/** Generated component and flow-control reference pages live at /reference/{components,flow-controls}/<name>_vN. */
export function getReferenceKind(slugs: string[]): ReferenceKindType | undefined {
  if (slugs.length !== 3 || slugs[0] !== 'reference') return undefined;

  if (slugs[1] === 'components') return 'Component';

  if (slugs[1] === 'flow-controls') return 'Flow Control';

  return undefined;
}

/** `google-sheets_v2` -> `google-sheets`, so the versions of one component can be grouped. */
export function getReferenceBaseName(slug: string): string {
  return slug.replace(VERSION_SUFFIX, '');
}

export function humanizeSlug(slug: string): string {
  return slug
    .replace(/[-_]+/g, ' ')
    .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}

/**
 * The title used in search results. Generated reference pages are named after the component alone ("X", "Map"),
 * which is too short and collides between a component and a flow control, or between versions of one component,
 * so they gain their kind and, when several versions exist, the version. Any other page whose title is shared
 * with another page gains the name of the nearest section that tells them apart.
 */
export function getSeoTitle({
  distinguishingSlugs = [],
  slugs,
  title,
  versionCount,
}: {
  distinguishingSlugs?: string[];
  slugs: string[];
  title: string;
  versionCount: number;
}): string {
  const kind = getReferenceKind(slugs);

  if (kind) {
    const version = slugs[2].match(VERSION_SUFFIX)?.[1];

    return versionCount > 1 && version ? `${title} ${kind} v${version}` : `${title} ${kind}`;
  }

  // A label from the page's own slug is what separates operations of one folder, so it is never shortened.
  const ownSlugLabel = distinguishingSlugs[0] === slugs.at(-1);

  return fitTitle(title, distinguishingSlugs.map(humanizeSlug), ownSlugLabel ? 1 : 0);
}

function composeTitle(title: string, labels: string[]): string {
  return labels.length > 0 ? `${title} - ${labels.join(', ')}` : title;
}

/**
 * Keeps a title within the length limit. Section labels lose leading words first, since their tail is the specific
 * part ("Embedded Configuration Integration Instance Workflow" -> "Integration Instance Workflow"); a title that is
 * still too long is cut at a word boundary, keeping the labels that make it unique.
 */
function fitTitle(title: string, labels: string[], protectedCount: number): string {
  const fittedLabels = [...labels];

  while (composeTitle(title, fittedLabels).length > MAX_TITLE_LENGTH) {
    const longestIndex = fittedLabels.reduce(
      (best, label, index) =>
        index >= protectedCount &&
        label.includes(' ') &&
        (best < 0 || label.length > fittedLabels[best].length)
          ? index
          : best,
      -1,
    );

    if (longestIndex < 0) break;

    fittedLabels[longestIndex] = fittedLabels[longestIndex].split(' ').slice(1).join(' ');
  }

  const fitted = composeTitle(title, fittedLabels);

  if (fitted.length <= MAX_TITLE_LENGTH) return fitted;

  const suffix = fittedLabels.length > 0 ? ` - ${fittedLabels.join(', ')}` : '';

  if (MAX_TITLE_LENGTH - suffix.length < MIN_TITLE_ROOM) {
    return truncateAtWord(fittedLabels.join(', '), MAX_TITLE_LENGTH);
  }

  return `${truncateAtWord(title, MAX_TITLE_LENGTH - suffix.length)}${suffix}`;
}

/**
 * The ancestor folders that tell a page apart from every other page with the same title, nearest first, e.g.
 * `frontend` for an OpenAPI operation that also exists under `backend`. Folders are added one level at a time
 * until no other same-titled page shares all of them, falling back to the page's own slug when one shares the
 * whole path; empty for a unique title.
 */
export function getDistinguishingSlugs(slugs: string[], sameTitleSlugs: string[][]): string[] {
  const distinguishing: string[] = [];
  let remaining = sameTitleSlugs;

  for (let depth = 2; depth <= slugs.length && remaining.length > 0; depth++) {
    const segment = slugs[slugs.length - depth];
    const sharing = remaining.filter((other) => other[other.length - depth] === segment);

    if (sharing.length < remaining.length) distinguishing.push(segment);

    remaining = sharing;
  }

  // Pages in the same folder can only be told apart by their own slug, e.g. two OpenAPI operations of one tag.
  if (remaining.length > 0) distinguishing.unshift(slugs[slugs.length - 1]);

  return distinguishing;
}

function truncateAtWord(text: string, maxLength: number): string {
  if (text.length <= maxLength) return text;

  const cut = text.slice(0, maxLength - 1).replace(/[\uD800-\uDBFF]$/, '');
  const lastSpace = cut.lastIndexOf(' ');

  const end = lastSpace > cut.length / 2 ? lastSpace : cut.length;

  return `${cut.slice(0, end).replace(/[\s,;:.-]+$/, '')}…`;
}

function shortenToSentences(text: string): string {
  // Splits only where punctuation is followed by whitespace, so names like `cities.csv` stay whole.
  const sentences = text.split(/(?<=[.!?])\s+/);

  let result = '';

  for (const sentence of sentences) {
    const candidate = result ? `${result} ${sentence}` : sentence;

    if (candidate.length > MAX_DESCRIPTION_LENGTH) break;

    result = candidate;
  }

  return result.length >= MIN_DESCRIPTION_LENGTH
    ? result
    : truncateAtWord(text, MAX_DESCRIPTION_LENGTH);
}

/**
 * Fits a page description into the meta description length range. A long description is cut back to whole
 * sentences, or to a whole word when no sentence boundary fits; a short or missing one is extended with the
 * first filler sentence that lands it inside the range, falling back to the longest one that stays under it.
 */
export function fitDescription(description: string | undefined, fillers: string[]): string {
  const normalized = (description ?? '').replace(/\s+/g, ' ').trim();

  if (normalized.length > MAX_DESCRIPTION_LENGTH) return shortenToSentences(normalized);

  if (normalized.length >= MIN_DESCRIPTION_LENGTH) return normalized;

  const sentence = normalized && !/[.!?]$/.test(normalized) ? `${normalized}.` : normalized;

  const candidates = fillers.map((filler) => `${sentence} ${filler}`.trim());

  const fitting = candidates.find(
    (candidate) =>
      candidate.length >= MIN_DESCRIPTION_LENGTH && candidate.length <= MAX_DESCRIPTION_LENGTH,
  );

  if (fitting) return fitting;

  const underMaximum = candidates.filter((candidate) => candidate.length <= MAX_DESCRIPTION_LENGTH);

  if (underMaximum.length > 0) {
    return underMaximum.reduce((current, candidate) =>
      candidate.length > current.length ? candidate : current,
    );
  }

  return truncateAtWord(candidates[0] ?? normalized, MAX_DESCRIPTION_LENGTH);
}

/**
 * Filler sentences per page kind for descriptions that are missing or too short, longest first.
 */
export function getDescriptionFillers(slugs: string[], title: string): string[] {
  const kind = getReferenceKind(slugs);

  if (kind === 'Component') {
    return [
      `Reference for the ${title} component in ByteChef: configuration, available operations and outputs for use in workflows and AI agents.`,
      `Reference for the ${title} component in ByteChef: configuration, operations and outputs for workflows.`,
      `Reference for the ${title} component in ByteChef workflows and AI agents.`,
      `Reference for the ${title} component in ByteChef.`,
      `${title} component reference.`,
    ];
  }

  if (kind === 'Flow Control') {
    return [
      `Reference for the ${title} flow control in ByteChef: how it directs workflow execution, its properties and how to configure it.`,
      `Reference for the ${title} flow control in ByteChef: how it directs execution and how to configure it.`,
      `Reference for the ${title} flow control in ByteChef workflows.`,
      `${title} flow control reference.`,
    ];
  }

  if (slugs[0] === 'openapi') {
    // An operation's description usually restates its title, so the filler names the API instead.
    const api = slugs.length > 1 ? `ByteChef ${humanizeSlug(slugs[1])} API` : 'ByteChef API';

    return [
      `Part of the ${api} reference: endpoint, request parameters, request body, responses and code samples in several languages.`,
      `Part of the ${api} reference: endpoint, parameters, responses and code samples.`,
      `Part of the ${api} reference, with code samples.`,
      `${api} reference.`,
    ];
  }

  return [
    `Learn about ${title} in the ByteChef documentation, the open-source platform for workflow automation, integrations and AI agents.`,
    `Learn about ${title} in the ByteChef documentation for workflow automation and AI agents.`,
    `Learn about ${title} in the ByteChef documentation.`,
    'From the ByteChef documentation.',
  ];
}
