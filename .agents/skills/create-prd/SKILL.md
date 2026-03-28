---
name: create-prd
description: Generate structured PRD for admin systems from rough requirements. Use when organizing modules, pages, and flows for development. / 根据后台需求生成结构化 PRD，用于整理模块、页面和流程，作为开发输入。
---

# 目标

将需求转为结构化 PRD，供 API 和代码使用。

## 输入

- 系统说明
- 模块或功能描述

## 输出

包含：

- 角色
- 模块
- 页面清单
- 页面说明（字段/操作）
- 核心流程
- 验收标准

## 规则

- 页面必须包含字段和操作
- 避免模糊描述
- 不确定写 TODO
- 输出结构必须稳定

## 自检

- 是否可直接拆 API
- 是否包含字段与操作
