import { describe, expect, it } from 'vitest';
import {
  MAX_DESCRIPTION_LENGTH,
  MIN_DESCRIPTION_LENGTH,
  fitDescription,
  getDescriptionFillers,
  getDistinguishingSlugs,
  getSeoTitle,
} from '@/lib/seo';

describe('getSeoTitle', () => {
  it('names a component page with its kind', () => {
    expect(
      getSeoTitle({ slugs: ['reference', 'components', 'x_v1'], title: 'X', versionCount: 1 }),
    ).toBe('X Component');
  });

  it('tells a component from a flow control of the same name', () => {
    expect(
      getSeoTitle({ slugs: ['reference', 'flow-controls', 'map_v1'], title: 'Map', versionCount: 1 }),
    ).toBe('Map Flow Control');
  });

  it('adds the version when a component has several', () => {
    expect(
      getSeoTitle({
        slugs: ['reference', 'components', 'google-sheets_v2'],
        title: 'Google Sheets',
        versionCount: 2,
      }),
    ).toBe('Google Sheets Component v2');
  });

  it('adds the parent section to a duplicated guide title', () => {
    expect(
      getSeoTitle({
        distinguishingSlugs: ['generate-component'],
        slugs: ['developer-guide', 'generate-component', 'create-trigger'],
        title: 'Create Trigger',
        versionCount: 1,
      }),
    ).toBe('Create Trigger - Generate Component');
  });

  it('keeps a unique guide title', () => {
    expect(
      getSeoTitle({ slugs: ['platform', 'glossary'], title: 'Glossary', versionCount: 1 }),
    ).toBe('Glossary');
  });
});

describe('getSeoTitle length', () => {
  it('drops leading words of a long section label', () => {
    expect(
      getSeoTitle({
        distinguishingSlugs: ['embedded-configuration-integration-instance-workflow', 'backend'],
        slugs: ['openapi', 'backend', 'embedded-configuration-integration-instance-workflow', 'enableWorkflow'],
        title: 'Enable a workflow',
        versionCount: 1,
      }),
    ).toBe('Enable a workflow - Integration Instance Workflow, Backend');
  });

  it('keeps the label from the page slug whole', () => {
    expect(
      getSeoTitle({
        distinguishingSlugs: ['getAllFrontendConnections', 'frontend'],
        slugs: ['openapi', 'frontend', 'embedded-configuration-connection', 'getAllFrontendConnections'],
        title: "Get all connected user's connections",
        versionCount: 1,
      }),
    ).toContain('Get All Frontend Connections');
  });

  it('cuts a title that is too long on its own at a word boundary', () => {
    const result = getSeoTitle({
      slugs: ['openapi', 'backend', 'embedded-project', 'getProjects'],
      title: 'Get automation workflow projects available to a connected user identified by external user id',
      versionCount: 1,
    });

    expect(result.length).toBeLessThanOrEqual(70);
    expect(result.endsWith('…')).toBe(true);
  });
});

describe('getDistinguishingSlugs', () => {
  it('is empty for a unique title', () => {
    expect(getDistinguishingSlugs(['platform', 'glossary'], [])).toEqual([]);
  });

  it('skips a parent folder the duplicates share', () => {
    expect(
      getDistinguishingSlugs(
        ['openapi', 'frontend', 'embedded-workflow', 'deleteWorkflow'],
        [['openapi', 'backend', 'embedded-workflow', 'deleteWorkflow']],
      ),
    ).toEqual(['frontend']);
  });

  it('uses the parent folder when it differs', () => {
    expect(
      getDistinguishingSlugs(
        ['developer-guide', 'generate-component', 'create-trigger'],
        [['developer-guide', 'build-component', 'create-trigger']],
      ),
    ).toEqual(['generate-component']);
  });

  it('falls back to the page slug when a duplicate shares the whole path', () => {
    expect(
      getDistinguishingSlugs(
        ['openapi', 'backend', 'embedded-integration', 'getIntegrations'],
        [['openapi', 'backend', 'embedded-integration', 'getIntegration']],
      ),
    ).toEqual(['getIntegrations']);
  });

  it('combines folders until no duplicate shares them all', () => {
    expect(
      getDistinguishingSlugs(
        ['openapi', 'frontend', 'project-workflow', 'enableWorkflow'],
        [
          ['openapi', 'backend', 'project-workflow', 'enableWorkflow'],
          ['openapi', 'frontend', 'instance-workflow', 'enableWorkflow'],
          ['openapi', 'backend', 'instance-workflow', 'enableWorkflow'],
        ],
      ),
    ).toEqual(['project-workflow', 'frontend']);
  });
});

describe('fitDescription', () => {
  it('keeps a description already in range', () => {
    const description = 'a'.repeat(140);

    expect(fitDescription(description, [])).toBe(description);
  });

  it('builds a description for a page without one', () => {
    const result = fitDescription(undefined, getDescriptionFillers(['platform', 'glossary'], 'Glossary'));

    expect(result.length).toBeGreaterThanOrEqual(MIN_DESCRIPTION_LENGTH);
    expect(result.length).toBeLessThanOrEqual(MAX_DESCRIPTION_LENGTH);
  });

  it('extends a short component description', () => {
    const fillers = getDescriptionFillers(['reference', 'components', 'nasa_v1'], 'NASA');
    const result = fitDescription('Access NASA open data.', fillers);

    expect(result).toBe(`Access NASA open data. ${fillers[0]}`);
  });

  it('cuts a long description back to whole sentences', () => {
    const first =
      'Expressions in ByteChef provide a powerful way to dynamically access, transform, and manipulate data within your workflows.';
    const second =
      'They allow you to reference data from previous steps, perform calculations, implement conditional logic, and more.';

    expect(fitDescription(`${first} ${second}`, [])).toBe(first);
  });

  it('cuts at a word boundary when no sentence boundary fits', () => {
    const result = fitDescription(`${'word '.repeat(60)}end.`, []);

    expect(result.length).toBeLessThanOrEqual(MAX_DESCRIPTION_LENGTH);
    expect(result.endsWith('word…')).toBe(true);
  });

  it.each([
    [['platform', 'what-is-bytechef'], 'What is ByteChef?'],
    [['reference', 'components', 'google-sheets_v2'], 'Google Sheets'],
    [['reference', 'flow-controls', 'loop_v1'], 'Loop'],
    [['openapi', 'backend', 'embedded-action', 'executeAction'], 'Execute an action'],
  ])('lands every short description of %j inside the range', (slugs, title) => {
    const fillers = getDescriptionFillers(slugs, title);

    for (let length = 0; length < MIN_DESCRIPTION_LENGTH; length++) {
      const description = length === 0 ? '' : `${'w'.repeat(length - 1)}.`;
      const result = fitDescription(description, fillers);

      expect(result.length, `original length ${length}`).toBeGreaterThanOrEqual(MIN_DESCRIPTION_LENGTH);
      expect(result.length, `original length ${length}`).toBeLessThanOrEqual(MAX_DESCRIPTION_LENGTH);
    }
  });
});
