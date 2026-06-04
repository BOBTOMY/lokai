# LokAI SDK Change Log

## v1.0.1 (2026-06-04)

### 🎉 新增功能

- ✅ 实现流式输出功能 `completeStream()`，支持实时Token回调
- ✅ 实现会话控制功能 `stop()` 和 `reset()`
- ✅ 实现模型管理功能 `getLoadedModels()` 和 `isModelLoaded()`
- ✅ 添加 `getBosToken()` 和 `getEosToken()` 方法，从模型动态获取Token值
- ✅ 支持聊天功能 `chat()` 和 `chatStream()`
- ✅ 添加完整的异常体系（`LokAIException`）
- ✅ 实现Tokenizer功能（`encode`/`decode`）

### 🐛 修复问题

- ✅ 修复Tokenizer BOS/EOS Token硬编码问题
- ✅ 修复聊天模板格式不一致问题（统一使用 `<system>/<user>/<assistant>` 格式）
- ✅ 修复文本解码乱码问题（使用 `common_token_to_piece` 正确解码）
- ✅ 修复采样器链缺失导致的崩溃问题

### 🔧 技术改进

- ✅ 使用 `channelFlow` 实现流式输出
- ✅ 添加 `StreamCallback` 接口支持流式回调
- ✅ 优化推理循环，添加停止标志检查
- ✅ 完善内存管理和资源释放
- ✅ 支持多线程推理（自动检测CPU核心数）

### 📚 文档更新

- ✅ 更新 API参考文档 [api_reference.md](./api_reference.md)
- ✅ 更新 技术路线图 [../../doc/planning/roadmap.md]
- ✅ 更新 任务拆分文档 [../../doc/planning/v1.0_task_breakdown.md]
- ✅ 添加 修复报告 [REPAIR_REPORT_20260604.md]

### ✅ 单元测试

- ✅ 搭建Native层单元测试框架（Google Test）
- ✅ 编写Kotlin层单元测试（JUnit）
- ✅ 测试覆盖：引擎管理、模型管理、推理会话、异常处理

### 📊 性能指标

| 指标 | 值 |
|------|------|
| 模型加载时间 | ~15-20秒（视设备） |
| 推理速度 | ~5-10 tokens/s（CPU） |
| 内存占用 | ~1.5GB（MiniCPM5 1B Q4） |
| 上下文窗口 | 2048 tokens（可配置） |

---

## Unreleased (2026-06-02)

### Added
- ✅ 项目搭建与基础配置
- ✅ llama.cpp集成（通过Git Submodule）
- ✅ Native层推理引擎实现 ([native-lib.cpp](../android/sdk/src/main/cpp/native-lib.cpp))
- ✅ Kotlin SDK API设计与实现 ([LokAISDK.kt](../android/sdk/src/main/kotlin/com/lokai/sdk/LokAISDK.kt))
- ✅ SDK AAR包编译成功

---

**最后更新**：2026-06-04