# 来源与第三方组件

## 上游应用

本项目修改自 [gedoor/MyBookshelf](https://github.com/gedoor/MyBookshelf)，版本 `2.21.021620`，提交 `b392b72a416774733758f61b747dbb2385bb6462`，许可证为 GNU GPLv3。

原版作者与贡献者信息保留在 [README.upstream.md](README.upstream.md)；源文件中的原版权声明保持原样。原项目说明其更早的来源为 ZhangQinhao/MONKOVEL。

## 示例与测试书源

[examples/jdread-compat-sources.json](examples/jdread-compat-sources.json) 的基础解析规则来自 [XIU2/Yuedu](https://github.com/XIU2/Yuedu)，该项目同样采用 GPLv3。本次更新了速读谷域名、搜索列表选择器、请求脚本及分组名称，使之适配此次兼容版。

`app/src/androidTest/assets/sources.json` 是验证导入兼容的当日公开 XIU2 书源样本。原规则及其中的来源说明保留。该文件不是可用性承诺，也不是小说内容授权。

本仓库和发布附件不包含小说正文、用户下载的书、登录凭据或用户阅读数据。网站上的内容及其权利归相应权利人所有。

## 主要运行组件

| 组件 | 用途 | 来源 / 许可证 |
|---|---|---|
| Rhino 1.7.15 | JavaScript 解释执行 | [Mozilla Rhino](https://github.com/mozilla/rhino)，MPL 2.0 |
| Conscrypt 2.5.2 | 旧设备的 TLS 实现 | [Google Conscrypt](https://github.com/google/conscrypt)，Apache 2.0 |
| OkHttp 3.12.13 / Okio 1.15.0 | API 19 兼容网络请求 | [Square OkHttp](https://github.com/square/okhttp) / [Okio](https://github.com/square/okio)，Apache 2.0 |
| Retrofit 2.6.4 | 原应用网络接口兼容 | [Square Retrofit](https://github.com/square/retrofit)，Apache 2.0 |

其他依赖沿用上游项目，完整依赖声明见 `app/build.gradle` 和 `basemvplib/build.gradle`。组件的许可证与声明以各自发布包为准；本项目的 GPLv3 不替代第三方组件自身许可证。
