<template>
  <el-icon class="is-loading">
    <svg class="loading-svg" viewBox="0 0 20 20">
      <g class="loading-path" stroke-width="0" style="animation: none; stroke: none">
        <circle r="3.375" class="dot1" cx="0" cy="0" rx="0" ry="0" />
        <circle r="3.375" class="dot2" cx="0" cy="0" rx="0" ry="0" />
        <circle r="3.375" class="dot4" cx="0" cy="0" rx="0" ry="0" />
        <circle r="3.375" class="dot3" cx="0" cy="0" rx="0" ry="0" />
      </g>
    </svg>
  </el-icon>
</template>

<style>
.loading-path .dot1 {
  transform: translate(3.75px, 3.75px);
  fill: var(--el-color-primary);
  animation: custom-spin-move 1s infinite linear alternate;
  opacity: 0.3;
}
.loading-path .dot2 {
  transform: translate(calc(100% - 3.75px), 3.75px);
  fill: var(--el-color-primary);
  animation: custom-spin-move 1s infinite linear alternate;
  opacity: 0.3;
  animation-delay: 0.4s;
}
.loading-path .dot3 {
  transform: translate(3.75px, calc(100% - 3.75px));
  fill: var(--el-color-primary);
  animation: custom-spin-move 1s infinite linear alternate;
  opacity: 0.3;
  animation-delay: 1.2s;
}
.loading-path .dot4 {
  transform: translate(calc(100% - 3.75px), calc(100% - 3.75px));
  fill: var(--el-color-primary);
  animation: custom-spin-move 1s infinite linear alternate;
  opacity: 0.3;
  animation-delay: 0.8s;
}
@keyframes custom-spin-move {
  to {
    opacity: 1;
  }
}
</style>
