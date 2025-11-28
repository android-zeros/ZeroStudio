 应用程序包名修改功能策略：
 
 包名类型：
 第一种类型：com.test.abc
 第二种类型：com/test/abc
 
1. 获得src/main/java 或 kotlin文件夹
2. 硬检查：检查是否在src/main的java或者kotlin里修改文件夹名
3.加载并获得 {$project}/下的全部模块。
在这些com.android.library和com.android.application的模块下的：
src/main/java 或 kotlin 下搜索 ： {&被修改的内容} +包名类型
 修改内容需要要全词匹配 + 区分大小写
 