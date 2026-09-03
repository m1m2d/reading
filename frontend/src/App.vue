<template>
  <router-view />
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
html, body, #app {
  height: 100%;
  font-family: 'PingFang SC', 'Microsoft YaHei', 'Helvetica Neue', Arial, sans-serif;
  background: #f5f7fa;
}

/* 全局卡片：直角、无阴影、1px 边框 */
.el-card,
.el-card.is-always-shadow {
  border-radius: 0;
  box-shadow: none !important;
  border: 1px solid #e4e7ed;
}
</style>
