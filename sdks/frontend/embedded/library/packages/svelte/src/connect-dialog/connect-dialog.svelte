<script context="module" lang="ts">
  interface ConnectDialogProps {
    onClose?: () => void;
  }
</script>

<script lang="ts">
  import { onMount } from "svelte";

  import CloseButton from "./components/close-button.svelte";
  import ConnectButton from "./components/connect-button.svelte";
  import TabsList from "./components/tabs/tabs-list.svelte";
  import TabsItem from "./components/tabs/tabs-item.svelte";
  import fetchIntegration from "../middleware/fetch-integration";
  import { Integration } from "../middleware/model/Integration";
  import WorkflowsList from "./components/workflows/workflows-list.svelte";
  import PoweredBy from "./components/powered-by.svelte";

  export let onClose: ConnectDialogProps["onClose"] = undefined;

  let integration = undefined;

  onMount(() => {
    fetchIntegration().then((response) => {
      console.log(response);
      integration = response;
    });
  });
</script>

<div class="div">
  <div class="div-2">
    <CloseButton onClose={(event) => onClose && onClose()} />
    <div class="div-3">
      <img
        width="48px"
        src={`data:image/svg+xml,${encodeURIComponent(integration?.icon)}`}
        alt={integration?.title}
      />
      <div class="div-4">{integration?.title}</div>
    </div>
    <div class="div-5">
      {#if integration}
        {#if integration?.workflows.length}
          <TabsList
            activeTab="tab1"
            tabs={[
              {
                id: "tab1",
                label: "Overview",
              },
              {
                id: "tab2",
                label: "Configuration",
              },
            ]}
            ><TabsItem id="tab1"
              ><div class="div-9">{integration?.description}</div></TabsItem
            ><TabsItem id="tab2"
              ><WorkflowsList workflows={integration?.workflows} /></TabsItem
            ></TabsList
          >
        {:else}
          <div class="div-6">
            <div class="div-7">Overview</div>
            <div class="div-8">{integration?.description}</div>
          </div>
        {/if}
      {:else}
        Loading...
      {/if}
    </div>
    <ConnectButton /><PoweredBy />
  </div>
</div>

<style>
  .div {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(0, 0, 0, 0.8);
  }
  .div-2 {
    max-width: 480px;
    min-height: 300px;
    margin: 150px auto;
    background-color: #ffff;
    border-radius: 6px;
    padding: 1rem;
    position: relative;
    display: flex;
    flex-direction: column;
    font-family: Arial, sans-serif;
  }
  .div-3 {
    display: flex;
    align-items: center;
    margin-bottom: 1rem;
  }
  .div-4 {
    font-size: 1.3rem;
    margin-left: 0.5rem;
  }
  .div-5 {
    display: flex;
    flex-grow: 1;
  }
  .div-6 {
    display: flex;
    flex-direction: column;
  }
  .div-7 {
    font-size: 1rem;
    margin-bottom: 0.5rem;
  }
  .div-8 {
    color: #737c86;
    font-size: 0.9rem;
    line-height: 1.3;
  }
  .div-9 {
    color: #737c86;
    font-size: 0.9rem;
    line-height: 1.3;
  }
</style>