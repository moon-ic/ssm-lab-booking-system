# 基于SSM框架的高校实验室设备预约管理系统设计与实现

> 说明：本文档为论文技术章节草稿（第2章～第6章），供修改润色使用。

---

## 第2章 相关技术介绍

### 2.1 SSM框架概述

SSM框架是目前Java Web开发领域应用广泛的技术组合，由Spring、Spring MVC和持久层映射框架三部分构成，三者分别负责依赖注入与业务逻辑管理、HTTP请求处理与路由分发、以及数据库访问与对象关系映射，共同实现分层解耦的企业级应用架构。

本系统后端以Spring Boot 3为基础进行构建。Spring Boot在传统Spring框架之上提供了自动配置（Auto-Configuration）机制，内嵌了Spring MVC作为Web层实现，并引入Spring JDBC作为持久层访问组件，同时整合Spring Security承担认证与授权职责。相较于传统手工配置XML的SSM模式，Spring Boot通过约定优于配置的思想大幅降低了项目搭建成本，同时完整保留了Spring生态的分层架构理念：Controller层负责接收请求、Service层封装业务逻辑、Repository层执行SQL操作，三层之间通过Spring IoC容器实现解耦。因此，本系统整体上遵循SSM分层架构的设计思想，只是以Spring Boot作为统一的启动与配置入口。

#### 2.1.1 Spring框架

Spring是整个SSM技术体系的核心，其最重要的设计思想是控制反转（IoC，Inversion of Control）和依赖注入（DI，Dependency Injection）。在传统开发中，对象的创建和依赖关系的管理由代码本身负责，耦合度较高；而Spring通过IoC容器统一管理Bean的生命周期，各层组件只需声明所需的依赖，容器在运行时完成注入，从而实现组件之间的松耦合。

本系统中，所有的Controller、Service、Repository均以`@RestController`、`@Service`、`@Repository`等注解标注，由Spring IoC容器统一管理，并通过构造函数注入（Constructor Injection）的方式完成依赖装配，以保证依赖关系清晰且易于测试。

#### 2.1.2 Spring MVC

Spring MVC是Spring框架中用于处理HTTP请求的Web层组件，基于前端控制器（Front Controller）模式实现。其核心是`DispatcherServlet`，负责接收所有请求并将其分发到对应的Controller方法，Controller处理完毕后将结果通过`@ResponseBody`序列化为JSON格式返回给客户端。

本系统所有后端接口均以RESTful风格设计，使用`@RestController`注解声明控制器，`@GetMapping`、`@PostMapping`、`@PutMapping`、`@DeleteMapping`等注解映射HTTP方法与路径，`@RequestParam`、`@PathVariable`、`@RequestBody`等注解处理请求参数的绑定与校验，整体符合Spring MVC的标准使用规范。

#### 2.1.3 Spring JDBC

本系统持久层采用Spring JDBC作为数据库访问组件。Spring JDBC对原生JDBC进行了封装，通过`JdbcTemplate`提供了更简洁的API，自动处理连接的获取与释放、异常的统一转换等繁琐操作，同时保留了SQL的直接控制权，适合对SQL有精确要求的场景。

与MyBatis相比，Spring JDBC无需额外的XML Mapper文件或注解映射，SQL直接以字符串形式内嵌在Repository方法中，结合`RowMapper`完成结果集到Java对象的映射。本系统在`JdbcRepositorySupport`基类中封装了通用的增删改查方法，各业务Repository继承该基类，实现了代码复用与统一的异常处理。

#### 2.1.4 Spring Security

Spring Security是Spring生态中负责安全认证与授权的框架。本系统采用无状态（Stateless）的Token认证方案：用户登录成功后，服务端生成唯一Token并存储于内存（或可选的Redis中），客户端在后续请求的`Authorization`请求头中携带`Bearer <token>`，服务端通过自定义过滤器`AuthTokenFilter`解析Token、查询对应用户信息，并将认证对象写入`SecurityContextHolder`，从而实现身份识别。

权限控制方面，系统定义了四种角色（`SUPER_ADMIN`、`ADMIN`、`TEACHER`、`STUDENT`），Spring Security的`SecurityFilterChain`配置了路由级别的访问控制，业务方法内部则通过代码逻辑进一步校验角色权限，确保不同角色只能访问其被授权的资源。

### 2.2 前端技术栈

#### 2.2.1 Vue 3

Vue 3是一款渐进式JavaScript前端框架，采用组合式API（Composition API）设计，具有响应式数据绑定、组件化开发和虚拟DOM等特性。相较于Vue 2，Vue 3通过Proxy实现了更高效的响应式系统，并引入`setup()`函数使逻辑组织更加灵活。本系统前端以Vue 3为核心框架，结合TypeScript进行开发，提升了代码的类型安全性与可维护性。

#### 2.2.2 Vite

Vite是面向现代浏览器的前端构建工具，利用浏览器原生ES模块支持实现开发模式下的极速热重载（HMR），相较于Webpack具有更快的冷启动速度。本系统前端以Vite作为开发服务器与构建工具，并配置了代理规则，将`/api`路径转发至后端服务（`http://localhost:8081`），实现前后端分离开发下的接口联调。

#### 2.2.3 Element Plus

Element Plus是基于Vue 3的企业级UI组件库，提供了表格、表单、弹窗、分页、导航菜单等丰富的通用组件。本系统管理界面的所有页面均基于Element Plus构建，统一了视觉风格，并有效降低了界面开发成本。

#### 2.2.4 Vue Router与Pinia

Vue Router是Vue 3官方的路由管理库，本系统使用其实现前端路由配置与权限守卫（Navigation Guard），根据用户角色动态控制可访问的页面路由，未授权访问将被重定向至403页面。

Pinia是Vue 3官方推荐的状态管理库，本系统使用Pinia管理全局用户认证状态（Token、角色、用户信息等），各页面组件通过访问Store获取当前登录用户信息。

### 2.3 数据库技术

#### 2.3.1 MySQL

MySQL是目前应用最广泛的开源关系型数据库管理系统，支持ACID事务、丰富的索引类型和完善的SQL标准。本系统使用MySQL 8作为数据存储层，数据库名为`lab_booking`，通过版本化迁移脚本（`V1__init_schema.sql`、`V2__seed_data.sql`）管理表结构与初始数据，保证数据库状态的可重现性。

#### 2.3.2 Redis（可选）

Redis是基于内存的高性能键值存储系统，常用于缓存、会话管理等场景。本系统对Redis的集成采用可插拔设计：默认情况下，Token存储和缓存均使用内存实现（`InMemoryTokenStore`、`InMemoryAppCacheService`）；当配置项`APP_REDIS_ENABLED=true`时，系统自动切换到Redis实现（`RedisTokenStore`、`RedisAppCacheService`），适合多实例部署场景。

---

## 第3章 系统需求分析

### 3.1 系统背景与问题分析

高校实验室设备是开展教学科研工作的重要基础设施。传统的实验室设备管理多依赖纸质登记表或人工口头协调，存在以下突出问题：设备使用状态不透明，学生和教师无法实时了解设备是否可用；预约信息分散，管理员难以统一审核和调度；借用归还流程缺乏记录，设备丢失或损坏时责任难以追溯；维修工单缺乏系统化管理，设备维护效率低下。

为解决上述问题，本系统设计并实现了一套基于B/S架构的高校实验室设备预约管理系统，将设备信息、预约审批、借用归还、维修管理等核心业务数字化，并结合角色权限控制，实现不同身份用户的差异化功能访问。

### 3.2 系统角色定义

本系统定义了四类用户角色，各角色的职责划分如下：

| 角色 | 代码 | 主要职责 |
|------|------|---------|
| 超级管理员 | SUPER_ADMIN | 系统全局管理，包含管理员的全部权限，并可管理其他管理员账号 |
| 管理员 | ADMIN | 设备管理、最终预约审批、借用归还处理、维修管理、统计查看 |
| 教师 | TEACHER | 查看设备、提交预约、对所管学生的预约进行初审、管理个人中心 |
| 学生 | STUDENT | 查看设备、提交预约申请、借用归还操作、查看消息通知 |

### 3.3 功能需求

#### 3.3.1 认证与用户管理

- 用户可通过登录标识（工号/学号/账号）和密码登录系统
- 新用户首次登录后必须修改密码方可使用其他功能
- 管理员可创建、编辑、禁用用户账号，支持重置密码
- 用户可在个人中心查看并修改自己的基本信息和密码

#### 3.3.2 角色与权限管理

- 系统预置四种角色，支持对角色的权限配置进行调整
- 超级管理员可管理管理员及以下角色的账号
- 管理员可管理教师和学生账号

#### 3.3.3 设备管理

- 管理员可对设备进行新增、编辑、删除操作
- 设备信息包括：名称、编号、类别、状态、位置、图片、描述
- 设备状态包括：可用（AVAILABLE）、已预约（RESERVED）、借出（BORROWED）、维修中（REPAIRING）、损坏（DAMAGED）、已停用（DISABLED）
- 支持按关键字、类别、状态进行分页筛选查询
- 支持批量导入设备（CSV格式）

#### 3.3.4 预约管理

- 学生和教师可对状态为"可用"的设备发起预约申请，填写预约时间段和使用目的
- 系统自动检测时间冲突，不允许同一设备同一时间段存在多条有效预约
- 教师角色的预约直接进入已审批状态，跳过初审环节
- 预约审批流程分两级：教师初审（PENDING→APPROVED）、管理员终审（APPROVED→PICKUP_PENDING）
- 终审通过后系统自动生成借用记录，设备状态变更为"已预约"
- 申请人可取消处于待审核或已初审状态的预约
- 管理员可将超时未取用的预约标记为已失效

#### 3.3.5 借用归还管理

- 借用记录在预约终审通过后自动生成
- 管理员确认用户取用设备后，将借用记录状态更新为"借出中"，设备状态变更为"借出"
- 学生归还设备后，管理员录入设备状况，将借用记录标记为已归还，设备恢复可用
- 支持按用户、设备、状态进行借用记录查询

#### 3.3.6 维修管理

- 管理员或用户可对损坏设备提交维修申请，填写故障描述
- 管理员可更新维修进度（处理中→已完成），并填写处理结论
- 维修完成后，设备状态可由系统自动或人工恢复为可用

#### 3.3.7 消息通知

- 系统在关键业务节点自动向相关用户推送通知消息（预约状态变更、借用到期提醒等）
- 用户可在消息中心查看历史通知，支持标记已读

#### 3.3.8 统计分析

- 管理员可查看设备使用频次排行、预约量趋势、借用时长统计等数据报表

### 3.4 非功能需求

- **安全性**：接口采用Token认证，Token携带于请求头，后端无状态处理；角色权限控制到接口级别，防止越权访问
- **数据一致性**：预约审批、借用归还等状态变更操作需保证业务状态机的一致性
- **可用性**：系统提供健康检查接口（`/api/health`），便于运维监控
- **可扩展性**：Token存储和缓存组件采用接口抽象，支持在内存实现与Redis实现之间切换，适应单机和集群两种部署模式

---

## 第4章 系统设计

### 4.1 总体架构设计

本系统采用前后端分离的B/S架构。前端基于Vue 3构建单页应用（SPA），通过浏览器访问；后端基于Spring Boot 3提供RESTful API，两者通过HTTP协议通信，数据格式采用JSON。

```
┌─────────────────────────────────┐
│           浏览器（客户端）         │
│   Vue 3 + Element Plus + Pinia  │
│      Vue Router 路由守卫          │
└────────────┬────────────────────┘
             │ HTTP/REST (JSON)
             │ /api/**
┌────────────▼────────────────────┐
│         Spring Boot 后端         │
│  ┌──────────────────────────┐   │
│  │   Spring MVC Controller  │   │
│  ├──────────────────────────┤   │
│  │      Service（业务层）     │   │
│  ├──────────────────────────┤   │
│  │   Repository（数据访问层） │   │
│  │      Spring JDBC          │   │
│  └──────────┬───────────────┘   │
│             │                   │
│  Spring Security（认证/授权）     │
└────────────┬────────────────────┘
             │
┌────────────▼────────────────────┐
│          MySQL 数据库             │
│         lab_booking              │
└─────────────────────────────────┘
```

后端遵循三层架构：
- **Controller层**：接收HTTP请求，参数校验，调用Service，返回统一格式响应
- **Service层**：封装核心业务逻辑，处理状态流转、权限校验、跨实体操作
- **Repository层**：封装SQL操作，通过Spring JDBC执行数据库读写

### 4.2 安全认证设计

系统采用基于Bearer Token的无状态认证方案，整体流程如下：

1. 客户端发送登录请求（`POST /api/auth/login`），携带登录标识和密码
2. `AuthService`校验账号密码，生成随机UUID作为Token，存入Token存储（内存或Redis），返回Token给客户端
3. 客户端将Token存入本地，后续所有请求在`Authorization`头中携带`Bearer <token>`
4. `AuthTokenFilter`（继承`OncePerRequestFilter`）在每次请求时解析Token，从存储中查找对应用户，将用户信息写入`SecurityContextHolder`
5. `SecurityFilterChain`配置接口的认证要求，未携带有效Token的请求返回401
6. 首次登录的用户，`AuthTokenFilter`检测到`firstLoginRequired`标志后，拦截除修改密码之外的所有请求，返回特定提示

Token存储采用接口抽象（`TokenStore`），默认使用内存实现（`InMemoryTokenStore`），开启Redis后自动切换至`RedisTokenStore`，Token默认有效期12小时。

### 4.3 数据库设计

数据库名：`lab_booking`，共包含以下数据表：

#### 4.3.1 用户与权限相关表

**sys_user（用户表）**

| 字段 | 类型 | 说明 |
|------|------|------|
| user_id | BIGINT | 主键 |
| name | VARCHAR(100) | 姓名 |
| account | VARCHAR(100) | 账号（唯一） |
| login_id | VARCHAR(100) | 登录标识，学生为学号、教师为工号（唯一） |
| phone | VARCHAR(30) | 手机号 |
| role_code | VARCHAR(50) | 角色代码 |
| status | VARCHAR(30) | 账号状态（ENABLED/DISABLED） |
| credit_score | INT | 信用积分 |
| password | VARCHAR(100) | 密码 |
| first_login_required | TINYINT(1) | 是否首次登录需改密码 |
| manager_id | BIGINT | 所属教师ID（学生角色） |
| deleted | TINYINT(1) | 逻辑删除标志 |

**sys_role（角色表）**、**sys_permission（权限表）**、**sys_role_permission（角色权限关联表）**、**sys_menu（菜单表）**、**sys_role_menu（角色菜单关联表）**构成完整的RBAC权限体系。

#### 4.3.2 业务核心表

**lab_device（设备表）**

| 字段 | 类型 | 说明 |
|------|------|------|
| device_id | BIGINT | 主键 |
| device_name | VARCHAR(100) | 设备名称 |
| device_code | VARCHAR(100) | 设备编号（唯一） |
| category | VARCHAR(100) | 设备类别 |
| status | VARCHAR(50) | 设备状态 |
| location | VARCHAR(100) | 存放位置 |
| image_url | VARCHAR(255) | 图片路径 |
| description | VARCHAR(255) | 描述 |

**lab_reservation（预约记录表）**

| 字段 | 类型 | 说明 |
|------|------|------|
| reservation_id | BIGINT | 主键 |
| applicant_id | BIGINT | 申请人ID |
| device_id | BIGINT | 设备ID |
| start_time | DATETIME | 预约开始时间 |
| end_time | DATETIME | 预约结束时间 |
| purpose | VARCHAR(255) | 使用目的 |
| status | VARCHAR(50) | 预约状态 |
| reviewer_id | BIGINT | 审核人ID |
| review_comment | VARCHAR(255) | 审核意见 |
| created_at | DATETIME | 创建时间 |

预约状态枚举：`PENDING`（待审核）→ `APPROVED`（已初审）→ `PICKUP_PENDING`（待取用）→ `EXPIRED`（已失效）/ `REJECTED`（已驳回）/ `CANCELLED`（已取消）

**lab_borrow_record（借用记录表）**

| 字段 | 类型 | 说明 |
|------|------|------|
| record_id | BIGINT | 主键 |
| reservation_id | BIGINT | 关联预约ID |
| user_id | BIGINT | 借用人ID |
| device_id | BIGINT | 设备ID |
| status | VARCHAR(50) | 借用状态 |
| pickup_time | DATETIME | 取用时间 |
| expected_return_time | DATETIME | 预计归还时间 |
| return_time | DATETIME | 实际归还时间 |
| device_condition | VARCHAR(50) | 归还时设备状况 |

借用状态枚举：`PICKUP_PENDING`（待取用）→ `BORROWED`（借出中）→ `RETURNED`（已归还）/ `OVERDUE`（已逾期）

**lab_repair（维修记录表）**、**lab_notification（通知消息表）**、**lab_task_execution_log（定时任务日志表）** 分别支持维修管理、消息中心和定时任务等业务模块。

#### 4.3.3 表关系说明

- `lab_reservation.applicant_id` → `sys_user.user_id`
- `lab_reservation.device_id` → `lab_device.device_id`
- `lab_borrow_record.reservation_id` → `lab_reservation.reservation_id`（一对一）
- `lab_borrow_record.device_id` → `lab_device.device_id`
- `lab_notification.user_id` → `sys_user.user_id`
- `sys_user.manager_id` → `sys_user.user_id`（自引用，学生指向所属教师）

### 4.4 接口设计规范

所有后端接口遵循RESTful设计风格，统一响应结构如下：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

错误时返回对应的错误码（400参数错误、401未认证、403无权限、404资源不存在、409业务冲突）和错误描述，由`GlobalExceptionHandler`统一捕获并格式化。

主要接口模块汇总：

| 模块 | 路径前缀 | 主要操作 |
|------|---------|---------|
| 认证 | /api/auth | 登录、获取当前用户、修改密码 |
| 用户管理 | /api/users | 列表、新增、编辑、禁用、重置密码 |
| 角色权限 | /api/roles | 角色列表、权限配置 |
| 设备管理 | /api/devices | CRUD、状态管理、图片上传 |
| 设备导入 | /api/devices/import | CSV批量导入 |
| 预约管理 | /api/reservations | 发起、查询、审批、取消、失效 |
| 借用归还 | /api/borrow-records | 取用确认、归还登记、查询 |
| 维修管理 | /api/repairs | 新增、查询、状态更新 |
| 消息通知 | /api/notifications | 查询、标记已读 |
| 统计分析 | /api/statistics | 使用频次、趋势报表 |
| 菜单配置 | /api/menu-config | 菜单与图标管理 |

### 4.5 前端模块设计

前端采用单页应用架构，路由由Vue Router统一管理，主要模块与对应视图文件如下：

| 模块 | 视图文件 | 可访问角色 |
|------|---------|----------|
| 登录 | LoginView.vue | 全部 |
| 用户管理 | UserManagementView.vue | SUPER_ADMIN, ADMIN |
| 角色权限 | RolePermissionView.vue | SUPER_ADMIN, ADMIN |
| 设备管理 | DeviceManagementView.vue | SUPER_ADMIN, ADMIN |
| 设备导入 | DeviceImportView.vue | SUPER_ADMIN, ADMIN |
| 预约管理 | ReservationManagementView.vue | 全部 |
| 借用归还 | （集成在预约/设备模块） | SUPER_ADMIN, ADMIN |
| 维修管理 | RepairManagementView.vue | SUPER_ADMIN, ADMIN |
| 消息中心 | MessageCenterView.vue | 全部 |
| 统计分析 | StatisticsView.vue | SUPER_ADMIN, ADMIN |
| 个人中心 | ProfileCenterView.vue | 全部 |
| 菜单配置 | （管理员配置菜单） | SUPER_ADMIN, ADMIN |

路由守卫在`router/access.ts`中实现，通过读取Pinia中的认证状态判断用户是否已登录及是否有权访问目标路由，未通过则重定向至登录页或403页面。

---

## 第5章 系统实现

### 5.1 项目结构

#### 5.1.1 后端项目结构

```
admin-server/src/main/java/com/lab/booking/
├── AdminServerApplication.java      # 启动类
├── common/
│   ├── ApiException.java             # 业务异常类
│   ├── GlobalExceptionHandler.java   # 全局异常处理
│   └── Result.java                   # 统一响应封装
├── config/
│   ├── SecurityConfig.java           # Spring Security配置
│   ├── AuthTokenFilter.java          # Token认证过滤器
│   ├── DatabaseBootstrap.java        # 数据库初始化
│   └── StaticResourceConfig.java     # 静态资源配置
├── controller/                       # 控制器层
├── service/                          # 业务逻辑层
├── repository/                       # 数据访问层（Spring JDBC）
├── model/                            # 数据库实体类
├── dto/                              # 数据传输对象
└── infrastructure/
    ├── session/                      # Token存储（内存/Redis）
    └── cache/                        # 缓存服务（内存/Redis）
```

#### 5.1.2 前端项目结构

```
admin-ui/src/
├── main.ts                  # 入口文件
├── App.vue                  # 根组件
├── router/
│   ├── index.ts             # 路由配置
│   └── access.ts            # 路由守卫
├── store/                   # Pinia状态管理
├── views/                   # 页面视图组件
├── components/              # 公共组件
├── layouts/                 # 布局组件
├── mock/                    # Mock数据
└── types/                   # TypeScript类型定义
```

### 5.2 认证模块实现

#### 5.2.1 登录接口

登录接口由`AuthController`接收请求，调用`AuthService.login()`处理业务逻辑。核心实现如下：

```java
// AuthService.java
public Map<String, Object> login(AuthDtos.LoginRequest request) {
    UserEntity user = authRepository.findByLoginId(request.loginId())
            .orElseThrow(() -> new ApiException(401, "账号或密码错误"));
    if (!user.getPassword().equals(request.password())) {
        throw new ApiException(401, "账号或密码错误");
    }
    if (user.getStatus() != UserStatus.ENABLED) {
        throw new ApiException(403, "账号已禁用");
    }
    String token = authRepository.createToken(user.getUserId());
    // 返回token及用户信息
    ...
}
```

登录成功后返回Token、Token类型、过期时间、首次登录标志及用户基本信息。客户端将Token持久化到本地存储，并在请求拦截器中自动添加到请求头。

#### 5.2.2 Token认证过滤器

`AuthTokenFilter`继承Spring的`OncePerRequestFilter`，保证每次请求只执行一次过滤逻辑：

```java
// AuthTokenFilter.java
@Override
protected void doFilterInternal(HttpServletRequest request, 
        HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
    String authorization = request.getHeader("Authorization");
    if (authorization != null && authorization.startsWith("Bearer ")) {
        String token = authorization.substring(7);
        authRepository.findByToken(token).ifPresent(user -> {
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                    user, token,
                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRoleCode().name()))
                );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        });
    }
    // 首次登录拦截逻辑
    ...
    filterChain.doFilter(request, response);
}
```

过滤器从请求头解析Token，查找对应用户并构建认证对象写入安全上下文。后续的业务代码通过`SecurityContextHolder.getContext().getAuthentication()`即可获取当前用户信息。

#### 5.2.3 首次登录强制改密

系统对首次登录用户设置了强制修改密码机制。`AuthTokenFilter`在身份认证通过后，检查用户的`firstLoginRequired`标志，若为`true`则拦截除`/api/auth/me`和`/api/auth/password`之外的所有请求，返回特定的业务错误码，前端收到后跳转至修改密码页面。

### 5.3 设备管理模块实现

设备管理由`DeviceController`和`DeviceService`协作实现，主要功能包括设备的增删改查、状态管理和图片上传。

设备查询支持关键字（设备名称或编号模糊匹配）、类别、状态三个维度的组合筛选，并实现了内存级分页：

```java
// DeviceService.java（节选）
public Map<String, Object> listDevices(String keyword, String category, 
        DeviceStatus status, Integer pageNum, Integer pageSize) {
    authService.currentUser();  // 验证已登录
    List<Map<String, Object>> filtered = deviceRepository.findAll().stream()
            .filter(device -> keyword == null || matchesKeyword(device, keyword))
            .filter(device -> category == null || category.equals(device.getCategory()))
            .filter(device -> status == null || status == device.getStatus())
            .sorted(Comparator.comparing(DeviceEntity::getDeviceId))
            .map(this::toDeviceSummary)
            .toList();
    // 分页截取
    ...
}
```

设备图片上传由`DeviceService`处理，将文件保存至服务器的`uploads/devices/`目录，并将相对路径存入数据库的`image_url`字段。静态资源通过`StaticResourceConfig`对外暴露，前端可直接通过URL访问图片。

### 5.4 预约管理模块实现

预约管理是本系统的核心业务模块，涵盖预约发起、两级审批、取消和失效处理，业务状态流转较为复杂。

#### 5.4.1 发起预约

```java
// ReservationService.java（节选）
public Map<String, Object> createReservation(
        ReservationDtos.CreateReservationRequest request) {
    UserEntity applicant = requireRoles(RoleCode.STUDENT, RoleCode.TEACHER);
    DeviceEntity device = getExistingDevice(request.deviceId());
    LocalDateTime startTime = parseDateTime(request.startTime(), "startTime");
    LocalDateTime endTime = parseDateTime(request.endTime(), "endTime");
    if (!endTime.isAfter(startTime)) {
        throw new ApiException(400, "预约结束时间必须晚于开始时间");
    }
    ensureReservable(device);           // 校验设备可预约
    ensureNoTimeConflict(request.deviceId(), startTime, endTime, null); // 时间冲突检测
    
    ReservationEntity reservation = new ReservationEntity();
    // 教师预约直接进入APPROVED状态，学生预约进入PENDING状态
    reservation.setStatus(applicant.getRoleCode() == RoleCode.TEACHER 
            ? ReservationStatus.APPROVED : ReservationStatus.PENDING);
    reservationRepository.save(reservation);
    return toReservationDetail(reservation);
}
```

时间冲突检测逻辑遍历同一设备的所有有效预约，判断是否存在时间区间重叠：若新预约的开始时间早于已有预约的结束时间，且新预约的结束时间晚于已有预约的开始时间，则认定为时间冲突，拒绝预约请求。

#### 5.4.2 两级审批流程

预约审批分为教师初审和管理员终审两级：

- **教师初审**：教师只能审核`PENDING`状态的预约，且只能审核所管学生的预约。通过后状态变为`APPROVED`；驳回需填写驳回原因，状态变为`REJECTED`。

- **管理员终审**：管理员只能审核`APPROVED`状态的预约（即已通过教师初审）。终审通过后，系统执行以下操作：
  1. 再次检查设备状态和时间冲突（防止并发场景下的数据不一致）
  2. 将预约状态更新为`PICKUP_PENDING`
  3. 将设备状态更新为`RESERVED`
  4. 自动创建对应的借用记录（状态为`PICKUP_PENDING`）

```java
// ReservationService.java（终审通过核心逻辑节选）
private void approveReservationInternal(ReservationEntity reservation, 
        UserEntity reviewer, String comment) {
    // ... 教师初审逻辑省略
    if (reservation.getStatus() != ReservationStatus.APPROVED) {
        throw new ApiException(409, "请先由教师审核通过");
    }
    DeviceEntity device = getExistingDevice(reservation.getDeviceId());
    ensureReservableForApproval(device, reservation);
    ensureNoTimeConflict(reservation.getDeviceId(), 
            reservation.getStartTime(), reservation.getEndTime(), 
            reservation.getReservationId());
    reservation.setStatus(ReservationStatus.PICKUP_PENDING);
    device.setStatus(DeviceStatus.RESERVED);
    deviceRepository.save(device);
    createBorrowRecord(reservation);  // 自动生成借用记录
}
```

#### 5.4.3 数据可见性控制

不同角色对预约记录的查看范围有所不同：
- 超级管理员/管理员：可查看全部预约
- 教师：可查看自己的预约，以及所管学生的预约
- 学生：只能查看自己的预约

此逻辑在`visibleTo()`方法中实现，通过`manager_id`字段判断学生与教师的从属关系。

### 5.5 借用归还模块实现

借用记录在预约终审通过时由系统自动创建，后续由管理员操作其状态流转：

1. **取用确认**：管理员确认用户取走设备后，将借用记录状态从`PICKUP_PENDING`更新为`BORROWED`，设备状态更新为`BORROWED`，记录实际取用时间。

2. **归还登记**：用户归还设备时，管理员选择设备状况（完好/轻微损坏/严重损坏），将借用记录状态更新为`RETURNED`，记录归还时间；若设备完好则将设备状态恢复为`AVAILABLE`，若损坏则更新为`DAMAGED`并可触发维修流程。

### 5.6 前端路由与权限控制实现

前端路由守卫在`router/access.ts`中实现，在每次路由跳转前执行认证与权限检查：

- 未登录用户访问任何非登录页路由时，重定向至登录页
- 已登录用户访问无权限的路由时，重定向至403页面
- 首次登录用户在完成密码修改前，只允许访问修改密码相关页面

前端通过Pinia Store保存用户Token和角色信息，路由守卫和各页面组件通过访问Store获取当前用户身份，控制界面上的操作按钮是否显示。

---

## 第6章 系统测试

### 6.1 测试策略

本系统前后端均设有自动化测试，测试类型包括单元测试和集成测试，分别覆盖核心业务逻辑和关键交互链路。

### 6.2 后端测试

后端测试使用JUnit 5和Spring Boot Test框架，集成测试直接访问真实数据库（测试库），不使用Mock数据库，以保证测试结果与生产环境行为一致。测试覆盖率通过JaCoCo插件统计，报告输出至`admin-server/target/site/jacoco/index.html`。

运行命令：

```bash
cd admin-server
mvn clean test
```

### 6.3 前端测试

前端测试使用Vitest框架，覆盖以下层次：

- **页面级测试**：对每个视图组件（`*.spec.ts`）进行渲染和基本交互测试
- **布局层测试**：验证侧边栏、顶部导航等布局组件的渲染正确性
- **路由守卫测试**：验证未登录、无权限等场景下的路由重定向行为
- **认证状态测试**：验证Pinia Store中Token和用户信息的存取逻辑
- **Mock聚合测试**：通过Mock Server模拟后端接口，验证前端关键业务链路的端到端行为

运行命令：

```bash
cd admin-ui
npm run test             # 运行测试
npm run test:coverage    # 生成覆盖率报告
```

### 6.4 真实服务验收

系统完成联调后，对以下关键链路进行了人工验收测试：

| 测试项 | 预期结果 | 实际结果 |
|--------|---------|---------|
| `GET /api/health` | 返回 `UP` | 通过 |
| 使用`SA001/000000`登录 | 登录成功，返回Token | 通过 |
| 学生访问管理员接口 | 返回403 | 通过 |
| 学生发起设备预约 | 创建PENDING状态预约 | 通过 |
| 教师初审预约通过 | 状态变为APPROVED | 通过 |
| 管理员终审预约通过 | 状态变为PICKUP_PENDING，自动生成借用记录 | 通过 |
| 管理员确认取用 | 借用记录变为BORROWED，设备状态变为BORROWED | 通过 |
| 用户归还设备 | 借用记录变为RETURNED，设备恢复AVAILABLE | 通过 |
| 时间冲突预约检测 | 返回409冲突错误 | 通过 |
| 首次登录修改密码拦截 | 未改密码前返回修改密码提示 | 通过 |

所有关键验收项均已通过，系统核心业务链路运行正常。

---

*（文献综述、致谢等章节待补充）*
