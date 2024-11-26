<script context="module" lang="ts">
  export interface TabsListProps {
    activeTab: string;
    tabs: {
      id: string;
      label: string;
    }[];
    children: any;
  }
</script>

<script lang="ts">
  import { setContext } from "svelte";

  import Context from "./tabs-list.context";
  import TabsListButton from "./tabs-list-button.svelte";

  export let activeTab: TabsListProps["activeTab"];
  export let tabs: TabsListProps["tabs"];

  let activeTab = activeTab;

  setContext(Context.key, {
    activeTab: activeTab,
  });
</script>

<div class="div">
  <nav class="nav">
    <ul role="tablist" aria-orientation="horizontal" class="ul">
      {#each tabs as tab, index (`tab-${index}`)}
        <li class="li">
          <TabsListButton
            {activeTab}
            id={tab.id}
            label={tab.label}
            onClick={(tabId) => (activeTab = tabId)}
          />
        </li>
      {/each}
    </ul>
  </nav>
  <slot />
</div>

<style>
  .div {
    width: 100%;
  }
  .nav {
    overflow-x: scroll;
    scrollbar-width: none;
    overflow-scrolling: touch;
    -webkit-overflow-scrolling: touch;
  }
  .ul {
    width: fit-content;
    min-width: 100%;
    color: #57606f;
    display: flex;
    gap: 0.5em;
    border-bottom: 2px solid #ddd;
    margin: 0;
    padding: 0;
  }
  .li {
    display: block;
    margin-bottom: -2px;
  }
</style>