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
  ><svg
    xmlns="http://www.w3.org/2000/svg"
    width="20"
    height="20"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="2"
    strokeL-linejoin="round"
    class="lucide lucide-x"><path d="M18 6 6 18" /><path d="m6 6 12 12" /></svg
  ></button
>

<style>
  .button {
    position: absolute;
    background-color: transparent;
    border-radius: 3px;
    cursor: pointer;
    font-size: 0.8rem;
    padding: 0.5rem;
    right: 0.2rem;
    top: 0.4rem;
    outline: none;
    border: none;
    font-weight: 500;
    text-transform: uppercase;
  }
</style>