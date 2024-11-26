<script context="module" lang="ts">
  export interface TabsListButtonProps {
    activeTab: string;
    id: string;
    label: string;
    onClick: (tabId: string) => void;
  }
</script>

<script lang="ts">
  export let id: TabsListButtonProps["id"];
  export let activeTab: TabsListButtonProps["activeTab"];
  export let onClick: TabsListButtonProps["onClick"];
  export let label: TabsListButtonProps["label"];
  function stringifyStyles(stylesObj) {
    let styles = "";
    for (let key in stylesObj) {
      const dashedKey = key.replace(/[A-Z]/g, function (match) {
        return "-" + match.toLowerCase();
      });
      styles += dashedKey + ":" + stylesObj[key] + ";";
    }
    return styles;
  }
</script>

<button
  style={stringifyStyles({
    color: activeTab === id ? "#2563EB" : "inherit",
    borderColor: activeTab === id ? "#2563EB" : "transparent",
  })}
  role="tab"
  class="button"
  key={`tab-btn-${id}`}
  id={`tab-${id}`}
  aria-controls={`panel-${id}`}
  aria-selected={activeTab === id}
  on:click={(event) => {
    onClick(id);
  }}>{label}</button
>

<style>
  .button {
    font-size: 0.9rem;
    padding: 0.75em 1em;
    background-color: transparent;
    border: 2px solid #ddd;
    border-width: 0 0 2px;
    cursor: pointer;
    white-space: nowrap;
  }
</style>