<template>
  <button class="button" @click="async (e) => onClose && onClose(e)">
    Connect
  </button>
</template>

<script lang="ts">
import { defineComponent } from "vue";

interface CloseButtonProps {
  onClose?: (e: any) => void;
}

export default defineComponent({
  name: "connect-button",

  props: ["onClose"],
});
</script>

<style scoped>
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