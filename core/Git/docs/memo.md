

---
update libgit2 procedure 20250816:
1. replace header files: delete previous and copy latest headers to `src/main/jni/include`
2. copy .so libs to repo for developing: build .so libs on github workflow then put them in to `src/main/jniLibs` (only for development, when build release version, will build the .so libs from source of libgit2 and it's dependencies)


---
为内置Editor关联文件 流程 procedure 20250718：
1 在 `MimeTypeMapCompat.kt` 添加对应的mime
2 在 `MimeTypeIconKt.mimeTypeToIconMap` 关联对应的图标


3 为内置文本编辑器关联文件
方法1 在 `SettingsCons.getEditor_defaultFileAssociationList` 添加文件后缀名：但需要写迁移代码合并用户自定义的扩展名，否则用户若修改过关联列表，必须重置才会包含开发者后来新增的扩展名。但并不推荐写迁移代码，因为如果用户编辑过关联列表，自然能想到再关联新增的类型，我再写迁移代码意义不大，反而没准增加新bug。

方法2 修改Mime类型： 在 `MimeTypeConversionExtensionsKt.mimeTypeToIntentMimeTypeMap` 将文件后缀名转换为 "text/your_type" 类型，内置编辑器会默认打开所有mime为text/*的文件。这种方式的缺点是对用户来说不可控，用户无法通过用户界面取消关联，但这种方法不需要写设置项迁移代码，而且用户总是可通过open as来使用外部app打开文件，所以这种方法也可接受。
