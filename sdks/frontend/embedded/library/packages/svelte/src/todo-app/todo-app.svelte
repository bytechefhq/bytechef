<script lang="ts">
  function addItem() {
    if (!newItemName) {
      return;
    }
    list = [...list, newItemName];
  }
  function deleteItem(idx: number) {
    list = list.filter((x, i) => i !== idx);
  }

  let list = ["hello", "world"];
  let newItemName = "";
</script>

<div class="div">
  <span>TO-DO list:</span>
  <div class="div-2">
    <input
      placeholder="Add a new item"
      class="input"
      bind:value={newItemName}
    /><button
      class="button"
      on:click={(event) => {
        addItem();
      }}
    >
      Add
    </button>
  </div>
  <div class="div-3">
    <ul class="ul">
      {#each list as item, idx (idx)}
        <li class="li">
          <span>{item}</span><button
            class="button-2"
            on:click={(event) => {
              deleteItem(idx);
            }}
          >
            Delete
          </button>
        </li>
      {/each}
    </ul>
  </div>
</div>

<style>
  .div {
    padding: 10px;
    max-width: 700px;
  }
  .div-2 {
    display: flex;
    width: 100%;
    gap: 16px;
    align-items: stretch;
  }
  .input {
    padding-top: 0.5rem;
    padding-bottom: 0.5rem;
    padding-left: 1rem;
    padding-right: 1rem;
    border-radius: 0.25rem;
    flex-grow: 1;
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1),
      0 2px 4px -1px rgba(0, 0, 0, 0.06);
  }
  .button {
    padding-top: 0.5rem;
    padding-bottom: 0.5rem;
    padding-left: 1rem;
    padding-right: 1rem;
    border-radius: 0.25rem;
    font-weight: 700;
    color: #ffffff;
    background-color: #3b82f6;
    cursor: pointer;
  }
  .div-3 {
    margin-top: 1rem;
  }
  .ul {
    border-radius: 0.25rem;
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1),
      0 2px 4px -1px rgba(0, 0, 0, 0.06);
    margin: unset;
    padding: unset;
  }
  .li {
    display: flex;
    padding: 0.625rem;
    align-items: center;
    border-bottom-width: 1px;
    border-color: #e5e7eb;
    gap: 16px;
  }
  .button-2 {
    cursor: pointer;
    padding-top: 0.5rem;
    padding-bottom: 0.5rem;
    padding-left: 1rem;
    padding-right: 1rem;
    border-radius: 0.25rem;
    color: #ffffff;
    background-color: #ef4444;
  }
</style>