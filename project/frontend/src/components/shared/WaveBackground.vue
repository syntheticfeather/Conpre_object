<template>
  <!--
    层叠剪纸波浪背景（Layered Paper-cut Waves）
    结构：wave-tilt（静态旋转/定位） > wave-drift（位移动画） > svg（4 份相同波形）
    纯装饰组件：不拦截鼠标事件，不参与业务逻辑

    无缝 + 不露底的几何保证：
    - wave-tilt 宽 400%、left:-150%，静止时覆盖 [-1.5屏, +2.5屏]
    - svg 内是 4 个完全相同的 1440 周期，每个周期渲染后恰宽 1 屏
    - 动画只移 -25%（= 1 屏 = 1 个波长），终点覆盖 [-2.5屏, +1.5屏]
    - 任意时刻屏幕 [0, 1屏] 都落在覆盖区间内，且首尾波形完全重合
  -->
  <div class="wave-bg" aria-hidden="true">
    <!-- 第 1 层：深蓝紫，最深、带剪纸投影 -->
    <div class="wave-tilt wave-tilt--1">
      <div class="wave-drift">
        <svg viewBox="0 0 5760 900" preserveAspectRatio="none">
          <path d="M0,200 C240,110 480,300 720,210 C960,120 1200,300 1440,200
                   C1680,110 1920,300 2160,210 C2400,120 2640,300 2880,200
                   C3120,110 3360,300 3600,210 C3840,120 4080,300 4320,200
                   C4560,110 4800,300 5040,210 C5280,120 5520,300 5760,200
                   L5760,900 L0,900 Z"/>
        </svg>
      </div>
    </div>

    <!-- 第 2 层：品牌蓝 -->
    <div class="wave-tilt wave-tilt--2">
      <div class="wave-drift">
        <svg viewBox="0 0 5760 900" preserveAspectRatio="none">
          <path d="M0,260 C260,150 460,340 720,250 C980,160 1200,330 1440,260
                   C1700,150 1900,340 2160,250 C2420,160 2640,330 2880,260
                   C3140,150 3340,340 3600,250 C3860,160 4080,330 4320,260
                   C4580,150 4780,340 5040,250 C5300,160 5520,330 5760,260
                   L5760,900 L0,900 Z"/>
        </svg>
      </div>
    </div>

    <!-- 第 3 层：天蓝（反向漂移） -->
    <div class="wave-tilt wave-tilt--3">
      <div class="wave-drift">
        <svg viewBox="0 0 5760 900" preserveAspectRatio="none">
          <path d="M0,230 C200,330 520,120 720,230 C920,340 1240,130 1440,230
                   C1640,330 1960,120 2160,230 C2360,340 2680,130 2880,230
                   C3080,330 3400,120 3600,230 C3800,340 4120,130 4320,230
                   C4520,330 4840,120 5040,230 C5240,340 5560,130 5760,230
                   L5760,900 L0,900 Z"/>
        </svg>
      </div>
    </div>

    <!-- 第 4 层：青色，最浅最缓 -->
    <div class="wave-tilt wave-tilt--4">
      <div class="wave-drift">
        <svg viewBox="0 0 5760 900" preserveAspectRatio="none">
          <path d="M0,300 C240,250 480,360 720,300 C960,240 1200,350 1440,300
                   C1680,250 1920,360 2160,300 C2400,240 2640,350 2880,300
                   C3120,250 3360,360 3600,300 C3840,240 4080,350 4320,300
                   C4560,250 4800,360 5040,300 C5280,240 5520,350 5760,300
                   L5760,900 L0,900 Z"/>
        </svg>
      </div>
    </div>
  </div>
</template>

<script setup>
// 纯 CSS 动效，无需脚本逻辑
</script>

<style scoped>
/* 背景层：铺满父容器，不拦截交互 */
.wave-bg {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
  border-radius: inherit;
}

/* 外层：静态旋转 + 定位（不可与位移动画写在同一层，否则 transform 互相覆盖）
   400% 宽 + 左右各 1.5 屏余量，保证 -12° 旋转后平移全程不露底 */
.wave-tilt {
  position: absolute;
  left: -150%;
  width: 400%;
  height: 78%;
  transform: rotate(-12deg);
}
.wave-tilt--1 { top: -28%; }
.wave-tilt--2 { top: 2%; }
.wave-tilt--3 { top: 32%; }
.wave-tilt--4 { top: 58%; }

/* 内层：平移动画。位移 -25% = 条带宽度的 1/4 = 1 屏 = svg 中一个波形周期，
   起点与终点画面完全重合，实现无缝单向循环 */
.wave-drift {
  height: 100%;
  will-change: transform;
  animation: wave-drift 30s linear infinite;
}
.wave-tilt--1 .wave-drift { animation-duration: 32s; }
.wave-tilt--2 .wave-drift { animation-duration: 44s; }
.wave-tilt--3 .wave-drift { animation-duration: 56s; animation-direction: reverse; }
.wave-tilt--4 .wave-drift { animation-duration: 40s; animation-direction: reverse; }

.wave-drift svg {
  width: 100%;
  height: 100%;
  display: block;
}

/* 深浅交替的半透明蓝/青，叠在深紫蓝渐变底上形成层叠景深 */
.wave-tilt--1 path {
  fill: rgba(20, 44, 116, 0.55);
  filter: drop-shadow(0 14px 20px rgba(3, 12, 43, 0.35));
}
.wave-tilt--2 path { fill: rgba(37, 99, 235, 0.45); }
.wave-tilt--3 path { fill: rgba(56, 189, 248, 0.30); }
.wave-tilt--4 path { fill: rgba(37, 224, 191, 0.22); }

@keyframes wave-drift {
  from { transform: translateX(0); }
  to   { transform: translateX(-25%); }
}

/* 尊重系统"减少动态效果"设置 */
@media (prefers-reduced-motion: reduce) {
  .wave-drift { animation: none; }
}

/* 小屏减少一层，降低绘制开销 */
@media (max-width: 768px) {
  .wave-tilt--4 { display: none; }
}
</style>
