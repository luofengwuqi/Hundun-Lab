<script setup>
</script>

<template>
  <div class="card">
    <div class="header">
      <slot name="head">标题</slot>
    </div>
    <el-divider />
    <div class="main">
      <slot name="main">主体主体主体主体主体主体主体主体主体主体主体主体主体主体</slot>
    </div>
    <el-divider />
    <div class="footer">
      <slot name="footer"></slot>
    </div>
  </div>

</template>

<style scoped lang="less">
.card{
  z-index: 999;
  background-color: #FFF8E8;
  border-radius: 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  overflow: hidden;
  .header{
    height:50px;
    font-size: 16px;
    font-weight: 600;
    line-height: 50px;
    text-align: center;
  }
  .footer{
    button{
      width: 80%;
      height: 30px;
      margin: 5px 10% 5px 10%;
      background-color: #C3272B;
      border: none;
      border-radius: 15px;

      color: #fff;
      line-height: 30px;
    }
    button:hover {
      background-color: #D64A4F;
    }
  }

  .main{
    padding:10px;
  }
  :deep(.el-divider) {
    margin: 0;
  }
}


</style>
