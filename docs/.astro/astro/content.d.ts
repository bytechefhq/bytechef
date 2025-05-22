declare module 'astro:content' {
	interface Render {
		'.mdx': Promise<{
			Content: import('astro').MarkdownInstance<{}>['Content'];
			headings: import('astro').MarkdownHeading[];
			remarkPluginFrontmatter: Record<string, any>;
		}>;
	}
}

declare module 'astro:content' {
	interface RenderResult {
		Content: import('astro/runtime/server/index.js').AstroComponentFactory;
		headings: import('astro').MarkdownHeading[];
		remarkPluginFrontmatter: Record<string, any>;
	}
	interface Render {
		'.md': Promise<RenderResult>;
	}

	export interface RenderedContent {
		html: string;
		metadata?: {
			imagePaths: Array<string>;
			[key: string]: unknown;
		};
	}
}

declare module 'astro:content' {
	type Flatten<T> = T extends { [K: string]: infer U } ? U : never;

	export type CollectionKey = keyof AnyEntryMap;
	export type CollectionEntry<C extends CollectionKey> = Flatten<AnyEntryMap[C]>;

	export type ContentCollectionKey = keyof ContentEntryMap;
	export type DataCollectionKey = keyof DataEntryMap;

	type AllValuesOf<T> = T extends any ? T[keyof T] : never;
	type ValidContentEntrySlug<C extends keyof ContentEntryMap> = AllValuesOf<
		ContentEntryMap[C]
	>['slug'];

	/** @deprecated Use `getEntry` instead. */
	export function getEntryBySlug<
		C extends keyof ContentEntryMap,
		E extends ValidContentEntrySlug<C> | (string & {}),
	>(
		collection: C,
		// Note that this has to accept a regular string too, for SSR
		entrySlug: E,
	): E extends ValidContentEntrySlug<C>
		? Promise<CollectionEntry<C>>
		: Promise<CollectionEntry<C> | undefined>;

	/** @deprecated Use `getEntry` instead. */
	export function getDataEntryById<C extends keyof DataEntryMap, E extends keyof DataEntryMap[C]>(
		collection: C,
		entryId: E,
	): Promise<CollectionEntry<C>>;

	export function getCollection<C extends keyof AnyEntryMap, E extends CollectionEntry<C>>(
		collection: C,
		filter?: (entry: CollectionEntry<C>) => entry is E,
	): Promise<E[]>;
	export function getCollection<C extends keyof AnyEntryMap>(
		collection: C,
		filter?: (entry: CollectionEntry<C>) => unknown,
	): Promise<CollectionEntry<C>[]>;

	export function getEntry<
		C extends keyof ContentEntryMap,
		E extends ValidContentEntrySlug<C> | (string & {}),
	>(entry: {
		collection: C;
		slug: E;
	}): E extends ValidContentEntrySlug<C>
		? Promise<CollectionEntry<C>>
		: Promise<CollectionEntry<C> | undefined>;
	export function getEntry<
		C extends keyof DataEntryMap,
		E extends keyof DataEntryMap[C] | (string & {}),
	>(entry: {
		collection: C;
		id: E;
	}): E extends keyof DataEntryMap[C]
		? Promise<DataEntryMap[C][E]>
		: Promise<CollectionEntry<C> | undefined>;
	export function getEntry<
		C extends keyof ContentEntryMap,
		E extends ValidContentEntrySlug<C> | (string & {}),
	>(
		collection: C,
		slug: E,
	): E extends ValidContentEntrySlug<C>
		? Promise<CollectionEntry<C>>
		: Promise<CollectionEntry<C> | undefined>;
	export function getEntry<
		C extends keyof DataEntryMap,
		E extends keyof DataEntryMap[C] | (string & {}),
	>(
		collection: C,
		id: E,
	): E extends keyof DataEntryMap[C]
		? Promise<DataEntryMap[C][E]>
		: Promise<CollectionEntry<C> | undefined>;

	/** Resolve an array of entry references from the same collection */
	export function getEntries<C extends keyof ContentEntryMap>(
		entries: {
			collection: C;
			slug: ValidContentEntrySlug<C>;
		}[],
	): Promise<CollectionEntry<C>[]>;
	export function getEntries<C extends keyof DataEntryMap>(
		entries: {
			collection: C;
			id: keyof DataEntryMap[C];
		}[],
	): Promise<CollectionEntry<C>[]>;

	export function render<C extends keyof AnyEntryMap>(
		entry: AnyEntryMap[C][string],
	): Promise<RenderResult>;

	export function reference<C extends keyof AnyEntryMap>(
		collection: C,
	): import('astro/zod').ZodEffects<
		import('astro/zod').ZodString,
		C extends keyof ContentEntryMap
			? {
					collection: C;
					slug: ValidContentEntrySlug<C>;
				}
			: {
					collection: C;
					id: keyof DataEntryMap[C];
				}
	>;
	// Allow generic `string` to avoid excessive type errors in the config
	// if `dev` is not running to update as you edit.
	// Invalid collection names will be caught at build time.
	export function reference<C extends string>(
		collection: C,
	): import('astro/zod').ZodEffects<import('astro/zod').ZodString, never>;

	type ReturnTypeOrOriginal<T> = T extends (...args: any[]) => infer R ? R : T;
	type InferEntrySchema<C extends keyof AnyEntryMap> = import('astro/zod').infer<
		ReturnTypeOrOriginal<Required<ContentConfig['collections'][C]>['schema']>
	>;

	type ContentEntryMap = {
		"docs": {
"automation/getting-started/glossary.md": {
	id: "automation/getting-started/glossary.md";
  slug: "automation/getting-started/glossary";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"automation/getting-started/introduction.md": {
	id: "automation/getting-started/introduction.md";
  slug: "automation/getting-started/introduction";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"automation/getting-started/quick-start.md": {
	id: "automation/getting-started/quick-start.md";
  slug: "automation/getting-started/quick-start";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"developer_guide/build_component/add_connection.md": {
	id: "developer_guide/build_component/add_connection.md";
  slug: "developer_guide/build_component/add_connection";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"developer_guide/build_component/create_action.md": {
	id: "developer_guide/build_component/create_action.md";
  slug: "developer_guide/build_component/create_action";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"developer_guide/build_component/create_component_definition.md": {
	id: "developer_guide/build_component/create_component_definition.md";
  slug: "developer_guide/build_component/create_component_definition";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"developer_guide/build_component/create_trigger.md": {
	id: "developer_guide/build_component/create_trigger.md";
  slug: "developer_guide/build_component/create_trigger";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"developer_guide/build_component/initial_setup.md": {
	id: "developer_guide/build_component/initial_setup.md";
  slug: "developer_guide/build_component/initial_setup";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"developer_guide/build_component/overview.md": {
	id: "developer_guide/build_component/overview.md";
  slug: "developer_guide/build_component/overview";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"developer_guide/component_specification/action.md": {
	id: "developer_guide/component_specification/action.md";
  slug: "developer_guide/component_specification/action";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"developer_guide/component_specification/component.md": {
	id: "developer_guide/component_specification/component.md";
  slug: "developer_guide/component_specification/component";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"developer_guide/component_specification/connection.md": {
	id: "developer_guide/component_specification/connection.md";
  slug: "developer_guide/component_specification/connection";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"developer_guide/component_specification/property.md": {
	id: "developer_guide/component_specification/property.md";
  slug: "developer_guide/component_specification/property";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"developer_guide/component_specification/trigger.md": {
	id: "developer_guide/component_specification/trigger.md";
  slug: "developer_guide/component_specification/trigger";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"developer_guide/components/connectors_manual.md": {
	id: "developer_guide/components/connectors_manual.md";
  slug: "developer_guide/components/connectors_manual";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"developer_guide/generate_component/create_trigger.md": {
	id: "developer_guide/generate_component/create_trigger.md";
  slug: "developer_guide/generate_component/create_trigger";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"developer_guide/generate_component/customize_component.md": {
	id: "developer_guide/generate_component/customize_component.md";
  slug: "developer_guide/generate_component/customize_component";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"developer_guide/generate_component/initial_setup.md": {
	id: "developer_guide/generate_component/initial_setup.md";
  slug: "developer_guide/generate_component/initial_setup";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"developer_guide/generate_component/open_api_specification.md": {
	id: "developer_guide/generate_component/open_api_specification.md";
  slug: "developer_guide/generate_component/open_api_specification";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"developer_guide/generate_component/overview.md": {
	id: "developer_guide/generate_component/overview.md";
  slug: "developer_guide/generate_component/overview";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"developer_guide/observability/dev_setup.md": {
	id: "developer_guide/observability/dev_setup.md";
  slug: "developer_guide/observability/dev_setup";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"developer_guide/testing_triggers/triggers.md": {
	id: "developer_guide/testing_triggers/triggers.md";
  slug: "developer_guide/testing_triggers/triggers";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"embedded/getting-started/introduction.md": {
	id: "embedded/getting-started/introduction.md";
  slug: "embedded/getting-started/introduction";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"embedded/getting-started/quick-start.md": {
	id: "embedded/getting-started/quick-start.md";
  slug: "embedded/getting-started/quick-start";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"index.mdx": {
	id: "index.mdx";
  slug: "index";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".mdx"] };
"reference/components/accelo.md": {
	id: "reference/components/accelo.md";
  slug: "reference/components/accelo";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/active-campaign.md": {
	id: "reference/components/active-campaign.md";
  slug: "reference/components/active-campaign";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/affinity.md": {
	id: "reference/components/affinity.md";
  slug: "reference/components/affinity";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/airtable.md": {
	id: "reference/components/airtable.md";
  slug: "reference/components/airtable";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/aitable.md": {
	id: "reference/components/aitable.md";
  slug: "reference/components/aitable";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/amazon-bedrock.md": {
	id: "reference/components/amazon-bedrock.md";
  slug: "reference/components/amazon-bedrock";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/anthropic.md": {
	id: "reference/components/anthropic.md";
  slug: "reference/components/anthropic";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/app-event.md": {
	id: "reference/components/app-event.md";
  slug: "reference/components/app-event";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/asana.md": {
	id: "reference/components/asana.md";
  slug: "reference/components/asana";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/aws-s3.md": {
	id: "reference/components/aws-s3.md";
  slug: "reference/components/aws-s3";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/azure-openai.md": {
	id: "reference/components/azure-openai.md";
  slug: "reference/components/azure-openai";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/baserow.md": {
	id: "reference/components/baserow.md";
  slug: "reference/components/baserow";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/bash.md": {
	id: "reference/components/bash.md";
  slug: "reference/components/bash";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/box.md": {
	id: "reference/components/box.md";
  slug: "reference/components/box";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/chat.md": {
	id: "reference/components/chat.md";
  slug: "reference/components/chat";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/clickup.md": {
	id: "reference/components/clickup.md";
  slug: "reference/components/clickup";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/copper.md": {
	id: "reference/components/copper.md";
  slug: "reference/components/copper";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/csv-file.md": {
	id: "reference/components/csv-file.md";
  slug: "reference/components/csv-file";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/data-mapper.md": {
	id: "reference/components/data-mapper.md";
  slug: "reference/components/data-mapper";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/data-storage.md": {
	id: "reference/components/data-storage.md";
  slug: "reference/components/data-storage";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/date-helper.md": {
	id: "reference/components/date-helper.md";
  slug: "reference/components/date-helper";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/delay.md": {
	id: "reference/components/delay.md";
  slug: "reference/components/delay";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/discord.md": {
	id: "reference/components/discord.md";
  slug: "reference/components/discord";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/dropbox.md": {
	id: "reference/components/dropbox.md";
  slug: "reference/components/dropbox";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/email.md": {
	id: "reference/components/email.md";
  slug: "reference/components/email";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/encharge.md": {
	id: "reference/components/encharge.md";
  slug: "reference/components/encharge";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/example.md": {
	id: "reference/components/example.md";
  slug: "reference/components/example";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/figma.md": {
	id: "reference/components/figma.md";
  slug: "reference/components/figma";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/file-storage.md": {
	id: "reference/components/file-storage.md";
  slug: "reference/components/file-storage";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/filesystem.md": {
	id: "reference/components/filesystem.md";
  slug: "reference/components/filesystem";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/freshdesk.md": {
	id: "reference/components/freshdesk.md";
  slug: "reference/components/freshdesk";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/freshsales.md": {
	id: "reference/components/freshsales.md";
  slug: "reference/components/freshsales";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/github.md": {
	id: "reference/components/github.md";
  slug: "reference/components/github";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/gitlab.md": {
	id: "reference/components/gitlab.md";
  slug: "reference/components/gitlab";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/google-calendar.md": {
	id: "reference/components/google-calendar.md";
  slug: "reference/components/google-calendar";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/google-contacts.md": {
	id: "reference/components/google-contacts.md";
  slug: "reference/components/google-contacts";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/google-docs.md": {
	id: "reference/components/google-docs.md";
  slug: "reference/components/google-docs";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/google-drive.md": {
	id: "reference/components/google-drive.md";
  slug: "reference/components/google-drive";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/google-forms.md": {
	id: "reference/components/google-forms.md";
  slug: "reference/components/google-forms";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/google-mail.md": {
	id: "reference/components/google-mail.md";
  slug: "reference/components/google-mail";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/google-sheets.md": {
	id: "reference/components/google-sheets.md";
  slug: "reference/components/google-sheets";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/google-slides.md": {
	id: "reference/components/google-slides.md";
  slug: "reference/components/google-slides";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/groq.md": {
	id: "reference/components/groq.md";
  slug: "reference/components/groq";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/http-client.md": {
	id: "reference/components/http-client.md";
  slug: "reference/components/http-client";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/hubspot.md": {
	id: "reference/components/hubspot.md";
  slug: "reference/components/hubspot";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/hugging-face.md": {
	id: "reference/components/hugging-face.md";
  slug: "reference/components/hugging-face";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/image-helper.md": {
	id: "reference/components/image-helper.md";
  slug: "reference/components/image-helper";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/infobip.md": {
	id: "reference/components/infobip.md";
  slug: "reference/components/infobip";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/insightly.md": {
	id: "reference/components/insightly.md";
  slug: "reference/components/insightly";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/intercom.md": {
	id: "reference/components/intercom.md";
  slug: "reference/components/intercom";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/jira.md": {
	id: "reference/components/jira.md";
  slug: "reference/components/jira";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/jotform.md": {
	id: "reference/components/jotform.md";
  slug: "reference/components/jotform";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/json-file.md": {
	id: "reference/components/json-file.md";
  slug: "reference/components/json-file";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/json-helper.md": {
	id: "reference/components/json-helper.md";
  slug: "reference/components/json-helper";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/keap.md": {
	id: "reference/components/keap.md";
  slug: "reference/components/keap";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/logger.md": {
	id: "reference/components/logger.md";
  slug: "reference/components/logger";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/mailchimp.md": {
	id: "reference/components/mailchimp.md";
  slug: "reference/components/mailchimp";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/map.md": {
	id: "reference/components/map.md";
  slug: "reference/components/map";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/math-helper.md": {
	id: "reference/components/math-helper.md";
  slug: "reference/components/math-helper";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/microsoft-excel.md": {
	id: "reference/components/microsoft-excel.md";
  slug: "reference/components/microsoft-excel";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/microsoft-one-drive.md": {
	id: "reference/components/microsoft-one-drive.md";
  slug: "reference/components/microsoft-one-drive";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/microsoft-outlook-365.md": {
	id: "reference/components/microsoft-outlook-365.md";
  slug: "reference/components/microsoft-outlook-365";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/microsoft-share-point.md": {
	id: "reference/components/microsoft-share-point.md";
  slug: "reference/components/microsoft-share-point";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/microsoft-teams.md": {
	id: "reference/components/microsoft-teams.md";
  slug: "reference/components/microsoft-teams";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/mistral.md": {
	id: "reference/components/mistral.md";
  slug: "reference/components/mistral";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/myob.md": {
	id: "reference/components/myob.md";
  slug: "reference/components/myob";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/mysql.md": {
	id: "reference/components/mysql.md";
  slug: "reference/components/mysql";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/nifty.md": {
	id: "reference/components/nifty.md";
  slug: "reference/components/nifty";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/nvidia.md": {
	id: "reference/components/nvidia.md";
  slug: "reference/components/nvidia";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/object-helper.md": {
	id: "reference/components/object-helper.md";
  slug: "reference/components/object-helper";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/ods-file.md": {
	id: "reference/components/ods-file.md";
  slug: "reference/components/ods-file";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/ollama.md": {
	id: "reference/components/ollama.md";
  slug: "reference/components/ollama";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/one-simple-api.md": {
	id: "reference/components/one-simple-api.md";
  slug: "reference/components/one-simple-api";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/openai.md": {
	id: "reference/components/openai.md";
  slug: "reference/components/openai";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/pdf-helper.md": {
	id: "reference/components/pdf-helper.md";
  slug: "reference/components/pdf-helper";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/petstore.md": {
	id: "reference/components/petstore.md";
  slug: "reference/components/petstore";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/pinecone.md": {
	id: "reference/components/pinecone.md";
  slug: "reference/components/pinecone";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/pipedrive.md": {
	id: "reference/components/pipedrive.md";
  slug: "reference/components/pipedrive";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/pipeliner.md": {
	id: "reference/components/pipeliner.md";
  slug: "reference/components/pipeliner";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/postgresql.md": {
	id: "reference/components/postgresql.md";
  slug: "reference/components/postgresql";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/quickbooks.md": {
	id: "reference/components/quickbooks.md";
  slug: "reference/components/quickbooks";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/rabbitmq.md": {
	id: "reference/components/rabbitmq.md";
  slug: "reference/components/rabbitmq";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/random-helper.md": {
	id: "reference/components/random-helper.md";
  slug: "reference/components/random-helper";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/reckon.md": {
	id: "reference/components/reckon.md";
  slug: "reference/components/reckon";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/request.md": {
	id: "reference/components/request.md";
  slug: "reference/components/request";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/resend.md": {
	id: "reference/components/resend.md";
  slug: "reference/components/resend";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/salesflare.md": {
	id: "reference/components/salesflare.md";
  slug: "reference/components/salesflare";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/schedule.md": {
	id: "reference/components/schedule.md";
  slug: "reference/components/schedule";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/script.md": {
	id: "reference/components/script.md";
  slug: "reference/components/script";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/sendgrid.md": {
	id: "reference/components/sendgrid.md";
  slug: "reference/components/sendgrid";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/shopify.md": {
	id: "reference/components/shopify.md";
  slug: "reference/components/shopify";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/slack.md": {
	id: "reference/components/slack.md";
  slug: "reference/components/slack";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/spotify.md": {
	id: "reference/components/spotify.md";
  slug: "reference/components/spotify";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/stability.md": {
	id: "reference/components/stability.md";
  slug: "reference/components/stability";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/stripe.md": {
	id: "reference/components/stripe.md";
  slug: "reference/components/stripe";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/teamwork.md": {
	id: "reference/components/teamwork.md";
  slug: "reference/components/teamwork";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/text-helper.md": {
	id: "reference/components/text-helper.md";
  slug: "reference/components/text-helper";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/todoist.md": {
	id: "reference/components/todoist.md";
  slug: "reference/components/todoist";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/trello.md": {
	id: "reference/components/trello.md";
  slug: "reference/components/trello";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/twilio.md": {
	id: "reference/components/twilio.md";
  slug: "reference/components/twilio";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/typeform.md": {
	id: "reference/components/typeform.md";
  slug: "reference/components/typeform";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/var.md": {
	id: "reference/components/var.md";
  slug: "reference/components/var";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/vtiger.md": {
	id: "reference/components/vtiger.md";
  slug: "reference/components/vtiger";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/watsonx.md": {
	id: "reference/components/watsonx.md";
  slug: "reference/components/watsonx";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/weaviate.md": {
	id: "reference/components/weaviate.md";
  slug: "reference/components/weaviate";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/webflow.md": {
	id: "reference/components/webflow.md";
  slug: "reference/components/webflow";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/webhook.md": {
	id: "reference/components/webhook.md";
  slug: "reference/components/webhook";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/whatsapp.md": {
	id: "reference/components/whatsapp.md";
  slug: "reference/components/whatsapp";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/xero.md": {
	id: "reference/components/xero.md";
  slug: "reference/components/xero";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/xlsx-file.md": {
	id: "reference/components/xlsx-file.md";
  slug: "reference/components/xlsx-file";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/xml-file.md": {
	id: "reference/components/xml-file.md";
  slug: "reference/components/xml-file";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/xml-helper.md": {
	id: "reference/components/xml-helper.md";
  slug: "reference/components/xml-helper";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/zendesk-sell.md": {
	id: "reference/components/zendesk-sell.md";
  slug: "reference/components/zendesk-sell";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/components/zeplin.md": {
	id: "reference/components/zeplin.md";
  slug: "reference/components/zeplin";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/overview.md": {
	id: "reference/overview.md";
  slug: "reference/overview";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/task-dispatchers/branch.md": {
	id: "reference/task-dispatchers/branch.md";
  slug: "reference/task-dispatchers/branch";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/task-dispatchers/condition.md": {
	id: "reference/task-dispatchers/condition.md";
  slug: "reference/task-dispatchers/condition";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/task-dispatchers/each.md": {
	id: "reference/task-dispatchers/each.md";
  slug: "reference/task-dispatchers/each";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/task-dispatchers/fork-join.md": {
	id: "reference/task-dispatchers/fork-join.md";
  slug: "reference/task-dispatchers/fork-join";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/task-dispatchers/loop.md": {
	id: "reference/task-dispatchers/loop.md";
  slug: "reference/task-dispatchers/loop";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/task-dispatchers/map.md": {
	id: "reference/task-dispatchers/map.md";
  slug: "reference/task-dispatchers/map";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/task-dispatchers/parallel.md": {
	id: "reference/task-dispatchers/parallel.md";
  slug: "reference/task-dispatchers/parallel";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"reference/task-dispatchers/subflow.md": {
	id: "reference/task-dispatchers/subflow.md";
  slug: "reference/task-dispatchers/subflow";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"welcome.md": {
	id: "welcome.md";
  slug: "welcome";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
};

	};

	type DataEntryMap = {
		
	};

	type AnyEntryMap = ContentEntryMap & DataEntryMap;

	export type ContentConfig = typeof import("../../src/content/config.js");
}
