# LokAI SDK

移动端端侧AI推理SDK，基于llama.cpp，支持Android和鸿蒙系统。

---

## 📁 目录结构

```
lokai/
├── doc/                    # SDK项目文档
│   ├── SDK_Development_Guide.md  # SDK开发文档
│   ├── api_reference.md    # API参考文档
│   ├── Quick_Start_Guide.md # 用户接入指南
│   ├── Examples_Guide.md   # 示例代码文档
│   └── CHANGELOG.md        # 更新日志
│
├── android/                # Android平台SDK
│   ├── sdk/                # SDK核心模块
│   └── sample/             # 内置样例App
│
├── harmony/                # 鸿蒙平台SDK (v2.0)
│
└── shared/                 # 跨平台共享代码
```

---

## 📚 文档导航

| 文档 | 位置 | 说明 |
|------|------|------|
| **快速开始** | [Quick_Start_Guide.md](./doc/Quick_Start_Guide.md) | 用户接入指南，快速集成SDK |
| **示例代码** | [Examples_Guide.md](./doc/Examples_Guide.md) | 完整示例代码与最佳实践 |
| **API参考** | [api_reference.md](./doc/api_reference.md) | API接口详细说明 |
| **SDK开发文档** | [SDK_Development_Guide.md](./doc/SDK_Development_Guide.md) | SDK详细设计与实现 |
| **CHANGELOG** | [CHANGELOG.md](./doc/CHANGELOG.md) | 版本更新记录 |

---

## ✨ 核心特性

- **端侧推理** - 无需服务器，本地完成AI推理
- **高性能** - 基于llama.cpp优化，支持量化模型
- **流式输出** - 支持实时Token流式回调
- **对话管理** - 内置多轮对话支持
- **性能监控** - 实时统计推理速度、内存占用
- **跨平台** - Android优先，鸿蒙规划中

---

## 🚀 快速开始

### 1. 添加依赖

```gradle
dependencies {
    implementation 'com.lokai:sdk:1.0.0'
}
```

### 2. 初始化SDK

```kotlin
// 在Application中初始化
LokAIEngine.initialize(context)
```

### 3. 加载模型并推理

```kotlin
// 加载模型
val model = LokAIEngine.getModelManager()
    .loadModel("/path/to/model.gguf")
    .getOrThrow()

// 创建会话
val session = model.createSession()

// 流式推理
session.completeStream("你好")
    .collect { result -> 
        print(result.token)
    }
```

详细步骤请参考 [快速开始指南](./doc/Quick_Start_Guide.md)

---

## 📋 当前状态

| 阶段 | 版本 | 状态 |
|------|------|------|
| Android SDK | v1.0 | ✅ 已完成 |
| 鸿蒙 SDK | v2.0 | 📋 规划中 |

**v1.0 已完成功能：**
- ✅ 引擎初始化与生命周期管理
- ✅ GGUF模型加载与卸载
- ✅ 同步推理与流式推理
- ✅ 多轮对话支持
- ✅ Tokenizer分词功能
- ✅ 推理参数配置
- ✅ 性能监控统计
- ✅ 异常处理机制
- ✅ Sample测试App

---

## 🔗 项目总览

- **总文档**：[../../doc/README.md](../../doc/README.md)
- **系统架构**：[../../doc/architecture/system_architecture.md](../../doc/architecture/system_architecture.md)
- **技术路线**：[../../doc/planning/roadmap.md](../../doc/planning/roadmap.md)
- **需求分析**：[../../doc/requirements/requirement_specification.md](../../doc/requirements/requirement_specification.md)

---

**最后更新**：2026-06-04