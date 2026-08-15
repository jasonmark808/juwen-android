# 聚闻

聚闻是面向红米 Pad Pro 的个人新闻阅读器。GitHub Actions 定时执行免费的规则化新闻管线，Android 客户端提供横屏双栏、分类、搜索、收藏、离线缓存和本地通知。

核心功能不依赖 DeepSeek 或任何 AI。可选 AI 增强采用供应商中立的 OpenAI 兼容接口，并在失败时回退到规则模式。

## 快速验证

```powershell
python -m unittest discover -s pipeline/tests -v
./gradlew testDebugUnitTest assembleDebug
```

完整安装与部署步骤见 `outputs/安装与部署说明.md`。

