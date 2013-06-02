使用说明：
1、本工具包由北京师范大学2000级计算机系 张人杰 开发制作 基于多叉树的查找，任何问题请联系：
alex.zhangrj@hotmail.com
2、工具包自带敏感词词库，第一次调用时读入词库，故第一次调用时间可能较长，在类加载后普通pc机上html过滤5000字在80毫秒左右，纯文本35毫秒左右。
3、如需自定义词库，将jar包考入WEB-INF工程的lib目录，在WEB-INF/classes目录下建一个utf-8的words.dict文本文件，在该文件中以“关键字=级别”的方式写入，比如：
中国共产党=4
中国人民=1
0为级别最小，过滤后返回原字符串中出现的最高级别

整理人：
	本项目有本人因工作需要整理为Maven版本项目欢迎frok

已发布到Maven中央库，欢迎依赖。
<dependency>
  <groupId>com.belerweb</groupId>
  <artifactId>alex-word-filter</artifactId>
  <version>1.0</version>
</dependency>
