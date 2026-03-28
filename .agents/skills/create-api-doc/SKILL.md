---
name: create-api-doc
description: Generate structured API docs from PRD or requirement inputs. Use when defining endpoints for backend and frontend integration. / 基于 PRD 或需求生成结构化 API 文档，用于前后端对接。
---

# 目标

生成可用于开发与联调的 API 文档。

## 输入

- PRD 或结构化需求

## 输出

按模块输出接口定义。

## 工作流

1. 提取资源与模块
2. 生成接口清单
3. 补充参数、返回、权限、规则

## 接口结构

- 名称
- 方法
- 路径
- 参数/请求体
- 返回
- 权限
- 规则

## 规则

- 不脱离需求扩展
- 不虚构字段或接口
- 权限与状态必须写清
- 不确定写 TODO
- 保持简洁

## 自检

- 是否覆盖核心模块
- 是否可用于前后端开发
