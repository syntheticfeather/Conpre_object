<template>
  <div class="page-stage" :class="{ 'stage--detail': active }">
    <!-- 列表面板：active=false 时占文档流决定舞台高度，详情打开后整页左滑出场 -->
    <div class="stage-pane pane-list">
      <slot name="list" />
    </div>
    <!-- 详情面板：初始在屏外右侧，打开时整页右滑占满 -->
    <div class="stage-pane pane-detail">
      <slot name="detail" />
    </div>
  </div>
</template>

<script setup>
/**
 * Push 整页转场舞台
 * 几何公理：列表静止位 X=0，详情静止位 X=+100%（屏外右侧），两页等宽、非分栏。
 * 激活 pane 用 relative 占文档流（决定舞台高度）+ z-index:2；
 * 非激活 pane 用 absolute;inset:0 叠放 + z-index:1；切换时两者交换，视觉无跳动。
 */
defineProps({
  active: {
    type: Boolean,
    default: false
  }
})
</script>

<style>
/* 非 scoped：slot 内容由父组件渲染，scoped 选择器无法命中；page-stage 为专属前缀类名 */
.page-stage {
  position: relative;
  overflow: hidden;
  /* 进场/退场时长与缓动统一引用 base.css 全局 token：
     --page-stage-enter / --page-stage-leave / --page-stage-ease */
}

.page-stage .stage-pane {
  width: 100%;
  will-change: transform;
}

/* 列表面板：默认在前台（关闭详情时它进场，用进场节奏） */
.page-stage .pane-list {
  position: relative;
  z-index: 2;
  transform: translateX(0);
  visibility: visible;
  transition: transform var(--page-stage-enter) var(--page-stage-ease),
              visibility 0s;
}

/* 详情面板：默认屏外右侧（关闭详情时它离场，用退场节奏，动画结束再隐藏） */
.page-stage .pane-detail {
  position: absolute;
  inset: 0;
  z-index: 1;
  background: var(--html-bg, #f5f7fa);
  transform: translateX(100%);
  visibility: hidden;
  overflow-y: auto;
  transition: transform var(--page-stage-leave) var(--page-stage-ease),
              visibility 0s linear var(--page-stage-leave);
}

/* 打开详情：列表离场（略快） */
.page-stage.stage--detail .pane-list {
  position: absolute;
  inset: 0;
  z-index: 1;
  transform: translateX(-100%);
  visibility: hidden;
  pointer-events: none;
  transition: transform var(--page-stage-leave) var(--page-stage-ease),
              visibility 0s linear var(--page-stage-leave);
}

/* 打开详情：详情进场（从容），立即显示 */
.page-stage.stage--detail .pane-detail {
  position: relative;
  z-index: 2;
  transform: translateX(0);
  visibility: visible;
  transition: transform var(--page-stage-enter) var(--page-stage-ease),
              visibility 0s;
}

/* 尊重系统「减少动态效果」偏好：关闭位移，仅保留瞬时切换 */
@media (prefers-reduced-motion: reduce) {
  .page-stage .pane-list,
  .page-stage .pane-detail,
  .page-stage.stage--detail .pane-list,
  .page-stage.stage--detail .pane-detail {
    transition-duration: 1ms;
  }
}
</style>
