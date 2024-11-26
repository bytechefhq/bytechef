<script context="module" lang="ts">
  export type Props = {
    getValues?: (input: string) => Promise<any[]>;
    renderChild?: any;
    transformData?: (item) => string;
  };
</script>

<script lang="ts">
  export let getValues: Props["getValues"] = undefined;
  export let transformData: Props["transformData"] = undefined;
  export let renderChild: Props["renderChild"] = undefined;

  function setInputValue(value: string) {
    inputVal = value;
  }
  function handleClick(item) {
    setInputValue(transform(item));
    showSuggestions = false;
  }
  function fetchVals(city: string) {
    if (getValues) {
      return getValues(city);
    }
    return fetch(
      `http://universities.hipolabs.com/search?name=${city}&country=united+states`
    ).then((x) => x.json());
  }
  function transform(x) {
    return transformData ? transformData(x) : x.name;
  }

  let showSuggestions = false;
  let suggestions = [];
  let inputVal = "";

  function onUpdateFn_0(..._args: any[]) {
    fetchVals(inputVal).then((newVals) => {
      if (!newVals?.filter) {
        console.error("Invalid response from getValues:", newVals);
        return;
      }
      suggestions = newVals.filter((data) =>
        transform(data).toLowerCase().includes(inputVal.toLowerCase())
      );
    });
  }

  $: onUpdateFn_0(...[inputVal, getValues]);
</script>

<div class="div">
  Autocomplete:
  <div class="div-2">
    <input
      placeholder="Search for a U.S. university"
      class="input"
      on:focus={(event) => {
        showSuggestions = true;
      }}
      bind:value={inputVal}
    /><button
      class="button"
      on:click={(event) => {
        inputVal = "";
        showSuggestions = false;
      }}
    >
      X
    </button>
  </div>
  {#if suggestions.length > 0 && showSuggestions}
    <ul class="ul">
      {#each suggestions as item, idx (idx)}
        <li
          class="li"
          on:click={(event) => {
            handleClick(item);
          }}
        >
          {#if renderChild}
            <svelte:component this={renderChild} {item} />
          {:else}
            <span>{transform(item)}</span>
          {/if}
        </li>
      {/each}
    </ul>
  {/if}
</div>

<style>
  .div {
    padding: 10px;
    max-width: 700px;
  }
  .div-2 {
    position: relative;
    display: flex;
    gap: 16px;
    align-items: stretch;
  }
  .input {
    padding-top: 0.5rem;
    padding-bottom: 0.5rem;
    padding-left: 1rem;
    padding-right: 1rem;
    border-radius: 0.25rem;
    border-width: 1px;
    border-color: #000000;
    width: 100%;
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1),
      0 2px 4px -1px rgba(0, 0, 0, 0.06);
  }
  .button {
    cursor: pointer;
    padding-top: 0.5rem;
    padding-bottom: 0.5rem;
    padding-left: 1rem;
    padding-right: 1rem;
    border-radius: 0.25rem;
    color: #ffffff;
    background-color: #ef4444;
  }
  .ul {
    border-radius: 0.25rem;
    height: 10rem;
    margin: unset;
    padding: unset;
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1),
      0 2px 4px -1px rgba(0, 0, 0, 0.06);
  }
  .li {
    display: flex;
    padding: 0.5rem;
    align-items: center;
    border-bottom-width: 1px;
    border-color: #e5e7eb;
    cursor: pointer;
  }
  .li:hover {
    background-color: #f3f4f6;
  }
</style>