import { createMdxPlugin } from 'fumadocs-mdx/bun';
import { postInstall } from 'fumadocs-mdx/next';

const configPath = 'source.script.ts';
await postInstall({ configPath });
await Bun.plugin(createMdxPlugin({ configPath }));
