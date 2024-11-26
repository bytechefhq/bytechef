<script context="module" lang="ts">
  export interface TabsItemProps {
    id: string;
    children: any;
  }
</script>

<script lang="ts">
  import { getContext } from "svelte";

  import Context from "./tabs-list.context";

  export let id: TabsItemProps["id"];

  let context = getContext(Context.key);
</script>

{#if context.activeTab === id}
  <div
    role="tabpanel"
    class="div"
    aria-labelledby={`tab-${id}`}
    id={`panel-${id}`}
  >
    <slot />
  </div>
{/if}

<style>
  .div {
    margin-top: 0.7rem;
    margin-bottom: 0.7rem;
    padding-top: 0.5rem;
    padding-bottom: 0.5rem;
    border-radius: 0.5rem;
  }
</style>