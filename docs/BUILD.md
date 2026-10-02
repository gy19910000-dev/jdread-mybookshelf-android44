# 构建与验证

## 环境

本次成功构建使用 JDK 11.0.25、Gradle Wrapper 6.5、Android Gradle Plugin 4.1.2、Android SDK Platform 29 和 Build Tools 29.0.2。最低运行版本为 Android 4.4 / API 19，targetSdkVersion 为 29。

不要直接用 JDK 17/21 运行这套旧 Gradle/AGP。依赖下载需要访问 Google Maven、Maven Central、JitPack 及构建文件中配置的镜像。原工程的 Firebase 配置和本地签名配置已移除，公开构建不需要任何原作者或维护者的私钥。

设置 `JAVA_HOME` 指向 JDK 11，`ANDROID_HOME` / `ANDROID_SDK_ROOT` 指向已安装的 Android SDK，也可以自行创建不入库的 `local.properties`：

```properties
sdk.dir=/absolute/path/to/android-sdk
```

## 构建

Linux / macOS：

```sh
./gradlew --no-daemon testReleaseUnitTest assembleRelease
```

Windows PowerShell：

```powershell
.\gradlew.bat --no-daemon testReleaseUnitTest assembleRelease
```

APK 生成在 `app/build/outputs/apk/release/`。当前构建脚本使用构建日期生成 versionName，文件名会随构建时间改变，不保证字节级复现发布包。自行构建后用自己的证书签名；证书不同则不能覆盖已有安装。

9 项回归测试覆盖现代语法、JS 库、标准数组操作、GBK 请求、特殊字符插值、混合 URL/脚本、并发作用域及源地址规范化。测试报告在 `app/build/test-results/testReleaseUnitTest/`。

## 设备定向验证

设备测试针对随包提供的公开样本，报告结果数量和错误类型；不导出设备的私人数据库或正文。**测试会导入/更新样本书源；请先备份设备的应用数据。**

```sh
./gradlew --no-daemon assembleReleaseAndroidTest
```

应用 APK 和测试 APK 需要使用相同证书签名后安装。仅验证脚本运行和样本导入：

```sh
adb shell am instrument -w -e sources __runtime__ com.gedoor.monkeybook.test/com.kunfei.bookshelf.compat.CompatibilityInstrumentation
```

验证仓库的当前兼容示例：

```sh
adb push examples/jdread-compat-sources.json /sdcard/Download/jdread-compat-sources.json
adb shell am instrument -w -e fixture current -e key 凡人 -e read true com.gedoor.monkeybook.test/com.kunfei.bookshelf.compat.CompatibilityInstrumentation
```

参数 `-e ui true` 额外验证真实搜索界面。参数 `-e configure true` 将搜索分组设为“JDRead实测”，属于测试的显式配置操作。正文验证会通过应用正常的章节缓存逻辑保存首章，只输出字符数。验证后可卸载临时测试包 `com.gedoor.monkeybook.test`。

2026-10-02 在 JDRead-1 / Android 4.4.2 上记录：

```text
DEVICE_RUNTIME PASS api=19
SEARCH PASS 速读谷（JDRead兼容） count=31
TOC PASS 速读谷（JDRead兼容） chapters=1281
CONTENT PASS 速读谷（JDRead兼容） chars=3683
UI SEARCH PASS visible_results=31
COMPLETE searches_pass=1 searches_failed=0
```

## 发布安装包校验

`JDRead-Reader-JS-v0.1.0.apk` SHA256：

```text
1fe522f95222ed4a9f504dbbf6dd765361d32bbf50bee99e664dcf3a47897b0e
```

签名证书 SHA256：

```text
f173dfec3defe611a5f1131ddff0d8a65d4f2ecb8d96f8dc631544d7992b1b3c
```

签名私钥不在本仓库中。
