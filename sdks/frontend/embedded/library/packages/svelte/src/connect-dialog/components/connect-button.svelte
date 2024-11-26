<script context="module" lang="ts">
  interface CloseButtonProps {
    onClose?: (e: any) => void;
  }
</script>

<script lang="ts">
  export let onClose: CloseButtonProps["onClose"] = undefined;
</script>

<button
  class="button"
  on:click={(e) => {
    onClose && onClose(e);
  }}
>
  Connect
</button>

<style>
  .button {
    background-color: #000;
    border-radius: 10px;
    cursor: pointer;
    font-size: 1rem;
    padding: 0.7rem;
    right: 0.4rem;
    top: 0.4rem;
    outline: none;
    border: none;
    font-weight: 500;
    color: #fff;
    width: 100%;
  }
</style>