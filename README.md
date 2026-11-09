# 📚 E-Library 电子图书馆

一个基于 **Spring Boot + MyBatis** 的电子图书馆后端服务，内置自包含单页前端。它不只是一个「能借还书」的 CRUD 项目，更是把 **Java 并发编程**（信号量限流、线程池、并发容器、异步事件总线、并行聚合）落到真实业务场景里的完整实现。

启动后打开 [http://localhost:8080](http://localhost:8080) 即可在浏览器里体验全部功能。

---

## 一、功能一览

| 功能 | 说明 | 前端入口 / 接口 |
|------|------|----------------|
| 📖 书籍浏览 | 分页 + 书名/作者模糊搜索 | 首页「书籍列表」Tab / `GET /api/books` |
| 🔍 书籍详情 | 查看 ISBN、作者、馆藏、简介 | 卡片「详情」按钮 / `GET /api/books/{id}` |
| ➕ 新增书籍 | ISBN 唯一校验，成功返回 201 | 「新增书籍」Tab / `POST /api/books` |
| 📤 借阅 | 原子扣减库存，防重复借阅、防超卖 | 卡片「借阅」按钮 / `POST /api/loans` |
| 📥 归还 | 校验归属（`X-Member-Id`），回补库存 | 「我的借阅」Tab 「归还」按钮 / `POST /api/loans/{id}/return` |
| 📋 借阅记录 | 按会员查看，含逾期状态 | 「我的借阅」Tab / `GET /api/loans` |
| 📊 首页统计 | 书籍/会员/借阅中三项计数 **并行聚合** | 顶部统计栏 / `GET /api/stats/overview` |
| 📝 操作记录 | 借还事件 **异步落内存**，实时只读快照 | 「操作记录」Tab / `GET /api/events/*` |
| 🧪 并发探针 | `SELECT SLEEP` 确定性证明 DB 并发上限为 10 | 无 UI，命令行调用 `GET /api/probe/db` |

> 顶部统计栏在「借阅 / 归还 / 新增书籍」后会自动刷新；「操作记录」Tab 会展示当前会员的记录与全局动态。

---

## 二、技术栈

### 基础栈

| 项 | 选型 | 版本 | 说明 |
|----|------|------|------|
| 语言 | Java | 8 | `pom.xml` 中 `java.version=1.8` |
| 框架 | Spring Boot | 2.3.12.RELEASE | `spring-boot-starter-parent` |
| 持久层 | MyBatis（XML Mapper） | `mybatis-spring-boot-starter` 1.3.2（MyBatis 3.4.6） | 手写 SQL，不用 JPA/Hibernate |
| 连接池 | Druid | 1.1.17（`druid-spring-boot-starter`） | `max-active` 与信号量对齐为 10 |
| 工具 | Lombok | 1.18.44 | 简化 VO/DTO 样板代码 |
| 数据库 | MySQL | 8.0 | 远程库，`mysql-connector-java` 8.0.33 |
| 构建 | Maven Wrapper | — | 免装 Maven，clone 即跑（`./mvnw`） |
| 前端 | 原生 HTML/CSS/JS（单文件） | — | `static/index.html`，Spring Boot 作为欢迎页直接 serving |

### 并发技术点（`java.util.concurrent`）

| 技术点 | 用在哪里 | 作用 |
|--------|----------|------|
| **`Semaphore`（公平）** | `DbConcurrencyInterceptor` | 限制同一时刻访问 MySQL 的并发数 ≤ 10，超时返回 503 |
| **`ThreadPoolExecutor`** | `ThreadPoolConfig`（`bizExecutor`） | 业务线程池：核心 4 / 最大 8 / 有界队列 100 / `CallerRunsPolicy` 背压 |
| **`BlockingQueue` / `LinkedBlockingQueue`** | `LoanEventBus` | 事件缓冲，请求线程 `offer` 非阻塞，满则丢弃告警 |
| **`ConcurrentHashMap`** | `LoanEventBus` | 会员 ID → 事件队列的映射；`newKeySet()` 构造并发 `Set` |
| **`ConcurrentLinkedQueue`** | `LoanEventBus` | 每个会员的无锁事件队列 |
| **`CopyOnWriteArrayList`** | `LoanEventBus` | 全局最近事件（读多写少，读不加锁） |
| **`CompletableFuture`** | `StatsServiceImpl` | 三个 COUNT 查询并行提交到线程池，`allOf` 汇总 |
| **`AtomicInteger`** | `NamedThreadFactory` | 线程命名计数 |
| **`volatile`** | `LoanEventBus` | 消费者运行状态标志，保证可见性 |
| **MyBatis `Interceptor` 插件** | `DbConcurrencyInterceptor` | `@Intercepts` 拦截 `Executor.query/update`，在真正发 SQL 前抢许可 |

---

## 三、整体架构设计

### 3.1 分层架构

```
┌─────────────────────────────────────────────────────────┐
│  前端  static/index.html（单页：书籍 / 借阅 / 新增 / 记录） │
└───────────────────────────┬─────────────────────────────┘
                            │ HTTP / JSON（统一 ResultDto 包装）
┌───────────────────────────▼─────────────────────────────┐
│  Controller 层  接收请求、参数校验、返回 ResultDto         │
│  （构造器注入 + final 字段，风格统一）                      │
└───────────────────────────┬─────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────┐
│  Service 层     业务逻辑 + 事务边界（@Transactional）      │
│  ├─ LoanServiceImpl   借/还 + 发布事件                     │
│  ├─ StatsServiceImpl  CompletableFuture 并行聚合          │
│  └─ ...                                                 │
└───────┬───────────────────────┬─────────────────────────┘
        │                       │ 异步 publish（非阻塞）
┌───────▼───────┐       ┌───────▼─────────────────────────┐
│  Mapper 层     │       │  LoanEventBus 事件总线            │
│  MyBatis XML   │       │  单消费者线程（bizExecutor）       │
│  手写 SQL       │       │  分发到 5 种并发容器 ← 前端读快照  │
└───────┬───────┘       └──────────────────────────────────┘
        │  经过 Semaphore(10) 闸门（MyBatis Interceptor）
┌───────▼──────────────────────────────────────────────────┐
│  MySQL 8   （Druid 连接池 max-active=10，双层限流对齐）     │
└──────────────────────────────────────────────────────────┘
```

### 3.2 并发控制架构（核心）

```
请求线程 ──► Controller ──► Service ──► Mapper
                                         │
                          ┌──────────────▼──────────────┐
                          │ DbConcurrencyInterceptor     │
                          │  Semaphore(10).tryAcquire()  │
                          │  拿到许可 → 发 SQL → release │
                          │  拿不到(3s超时) → 抛 503     │
                          └──────────────┬──────────────┘
                                         ▼
                                   MySQL（连接池上限 10）

        ┌──────────────────────────────────────────────┐
        │  旁路：Service 借/还成功后 publish 事件        │
        │  LoanEventBus.queue.offer(...)  ← 非阻塞       │
        │       │                                       │
        │       ▼  bizExecutor 消费线程（单消费者）       │
        │  dispatch → ConcurrentHashMap / Concurrent-   │
        │             LinkedQueue / CopyOnWriteArrayList│
        │             / newKeySet（Set）                 │
        │       ▲                                       │
        │       │ 前端只读快照（不阻塞写）                 │
        └──────────────────────────────────────────────┘
```

---

## 四、核心设计思路

### 4.1 数据库并发硬上限 —— Semaphore 闸门

**问题**：高并发下大量请求同时打向数据库，可能拖垮 DB 或耗尽连接池。

**做法**：在 MyBatis 真正执行 SQL 的位置（`Executor.query` / `Executor.update`）挂一个插件 `DbConcurrencyInterceptor`，用公平 `Semaphore(10)` 做闸门：

- 请求进入 → `tryAcquire(3000ms)` 抢许可；
- 抢到 → 执行 SQL → `finally` 里 `release()`（保证不泄漏）；
- 超时抢不到 → 抛 `BusinessException(DB_BUSY)` → 全局异常处理器返回 **HTTP 503**。

**为什么是双层**：Druid 连接池 `max-active=10` + 信号量 `10` 对齐。连接池管「物理连接数」，信号量管「同时在跑的 SQL 数」，二者配合把对 MySQL 的并发压力牢牢锁在 10。

> 公平信号量（`fair=true`）保证先到先得，避免高并发下的饥饿。

### 4.2 请求与事件处理解耦 —— 异步事件总线

**问题**：借/还接口如果同步写「操作记录」，会拖慢接口、且记录的写与读耦合在一起。

**做法**：`LoanServiceImpl` 借/还成功后只调用 `eventBus.publish(...)` —— 往 `LinkedBlockingQueue` 里非阻塞 `offer` 一下就返回，**不影响接口延迟**。一个运行在 `bizExecutor` 上的单消费者线程后台轮询队列，把事件分发到 5 种并发容器：

| 容器 | 用途 | 选型理由 |
|------|------|----------|
| `BlockingQueue`（`LinkedBlockingQueue`） | 事件缓冲 | 有界、阻塞、生产者消费者天然解耦 |
| `ConcurrentHashMap<Long, ConcurrentLinkedQueue<LoanEvent>>` | 会员 → 该会员的事件队列 | `computeIfAbsent` 保证只创建一次 |
| `ConcurrentLinkedQueue<LoanEvent>` | 单会员事件列表 | 无锁写、多线程读安全 |
| `CopyOnWriteArrayList<LoanEvent>` | 全局最近事件 | 读多写少，读不加锁 |
| `Set<Long>`（`ConcurrentHashMap.newKeySet()`） | 产生过事件的会员集合 | JDK 没有ConcurrentHashSet，用此构造并发 Set |

前端「操作记录」Tab 调 `/api/events/recent`、`/api/events/members/{id}` 只读快照，与写完全隔离。每个容器都有容量上限（会员 50 条 / 全局 100 条 / 队列 1024），溢出自动淘汰，防止内存无限增长。

### 4.3 统计查询并行化 —— CompletableFuture + 线程池

**问题**：首页要展示「书籍总数 / 会员总数 / 借阅中」三个 COUNT，串行执行要 3 次往返。

**做法**：`StatsServiceImpl` 把三个查询用 `CompletableFuture.supplyAsync(..., bizExecutor)` 提交到业务线程池并行执行，再 `allOf(...).join()` 汇总：

- 三个查询各占一个信号量许可，同时只占用 3/10，**体现线程池与信号量的协同**；
- 显式指定 `bizExecutor` 而非默认 `ForkJoinPool.commonPool`，避免 I/O 密集任务阻塞公共池；
- 捕获 `CompletionException` 并解包 —— 线程内抛出的 `DB_BUSY(503)` 能还原成业务异常，按 503 返回而不是被吞成 500。

### 4.4 并发安全的借还库存 —— 条件原子 UPDATE

MyBatis 没有 JPA 的 `@Version` 乐观锁，改用数据库条件更新 + InnoDB 行锁保证并发安全：

- **借出扣库存**：`UPDATE book SET available_copies = available_copies - 1 WHERE id = ? AND available_copies > 0`，受影响行数 0 即无可借馆藏（409）。
- **归还改状态**：`UPDATE loan SET status='RETURNED' ... WHERE id = ? AND status='ACTIVE'`，受影响行数 0 即已归还 / 并发冲突。
- **防重复借阅**：`INSERT ... WHERE NOT EXISTS (SELECT 1 FROM loan WHERE ... AND status='ACTIVE')`，并发下受影响行数 0 即为重复借阅。

比「先查后写」安全，借助行锁保证不超卖、不重复归还。

### 4.5 统一响应与全局异常

所有接口返回 `ResultDto<T>`：`{ "code": "0", "msg": "success", "data": {...} }`。`@RestControllerAdvice` 全局异常处理器把业务异常映射到对应 HTTP 状态码（404 / 409 / 400 / 403 / **503** / 500），前端只需判断 `code === "0"`。

---

## 五、快速开始

### 5.1 环境要求

- **JDK 8**（当前机器默认 `/usr/bin/java` 是 JRE 无 `javac`，需指定 JDK 8，如 Corretto 8）
- **MySQL 8.0** 可访问实例
- 无需安装 Maven（用 `./mvnw`）

### 5.2 建库（含种子数据）

`src/main/resources/schema-mysql.sql` 会建库建表，并写入种子数据（3 本书 + 2 个会员），`INSERT IGNORE` 重复执行安全：

```bash
mysql -h <host> -P 3306 -u root -p < src/main/resources/schema-mysql.sql
```

> 无本地 mysql 客户端时，可用 `python3 + pymysql` 执行该脚本。

### 5.3 配置数据源

修改 `src/main/resources/application.yaml` 中 `spring.datasource.druid.elibrary` 的 `url` / `username` / `password` 为你的库地址与账号：

```yaml
spring:
  datasource:
    druid:
      elibrary:
        url: jdbc:mysql://<host>:3306/elibrary?...
        username: root
        password: <password>
        max-active: 10   # 与 Semaphore(10) 对齐
```

### 5.4 设置 JDK 8 并启动

```bash
export JAVA_HOME=/path/to/jdk8          # 例如 Corretto 8
cd elibrary
./mvnw spring-boot:run
```

看到日志 `Started ElibraryApplication` 即启动成功。

### 5.5 打开前端

浏览器访问 👉 **[http://localhost:8080](http://localhost:8080)**

---

## 六、启动后可访问的链接

| 链接 | 说明 |
|------|------|
| [http://localhost:8080](http://localhost:8080) | 前端首页（书籍列表 / 我的借阅 / 新增书籍 / 操作记录 + 顶部统计栏） |
| [http://localhost:8080/api/books](http://localhost:8080/api/books) | 书籍列表 JSON |
| [http://localhost:8080/api/members](http://localhost:8080/api/members) | 会员列表 JSON |
| [http://localhost:8080/api/stats/overview](http://localhost:8080/api/stats/overview) | 统计概览（并行聚合三项计数） |
| [http://localhost:8080/api/events/recent](http://localhost:8080/api/events/recent) | 全局最近操作记录（事件总线快照） |
| [http://localhost:8080/api/events/active-count](http://localhost:8080/api/events/active-count) | 产生过事件的会员数 |
| [http://localhost:8080/api/probe/db?sec=0.5](http://localhost:8080/api/probe/db?sec=0.5) | 并发探针：持有 DB 连接 0.5 秒 |

---

## 七、并发特性验证

### 7.1 正常流量不受影响

并发闸门只在「同时超过 10 个 DB 访问」时才触发，普通请求全部 200：

```bash
curl -s http://localhost:8080/api/stats/overview
# {"code":"0","msg":"success","data":{"bookCount":3,"memberCount":2,"activeLoanCount":0}}
```

### 7.2 证明并发上限 = 10（出现 503）

并发探针 `GET /api/probe/db?sec=0.8` 会让每个请求持有一个 DB 连接 + 一个信号量许可约 0.8 秒。同时打 20 个，前 10 个返回 200，其余在 3 秒内拿不到许可即返回 **503**：

```bash
# Python：20 并发请求探针，观察 200 与 503 的分布
python3 - <<'PY'
import concurrent.futures, urllib.request, json
def hit(i):
    try:
        with urllib.request.urlopen("http://localhost:8080/api/probe/db?sec=0.8", timeout=10) as r:
            return r.status
    except urllib.error.HTTPError as e:
        return e.code
with concurrent.futures.ThreadPoolExecutor(max_workers=20) as ex:
    codes = list(ex.map(hit, range(20)))
from collections import Counter
print("状态码分布:", Counter(codes))   # 预期约 10 个 200 + 10 个 503
PY
```

预期输出类似 `状态码分布: Counter({200: 10, 503: 10})`，503 的响应体为：
```json
{"code":"503","msg":"数据库繁忙，并发访问已达上限，请稍后重试","data":null}
```

### 7.3 验证事件流

1. 前端选一个会员，借一本书，再切到「操作记录」Tab —— 会看到「张三 借阅了 Java并发编程实战」；
2. 归还后再看 —— 出现「张三 归还了 Java并发编程实战」；
3. 命令行确认：
   ```bash
   curl -s http://localhost:8080/api/events/recent | python3 -m json.tool
   ```

> 事件由消费者线程异步分发，前端刷新可能有亚秒级延迟，属正常。

### 7.4 验证统计并行

看启动日志（MyBatis `StdOutImpl` 打印所有 SQL），访问 `/api/stats/overview` 时会看到 **3 条 COUNT 在 `biz-2` / `biz-3` / `biz-4` 线程上并发执行**，而非串行。

---

## 八、API 一览

基础路径 `/api`，统一响应体 `{ "code", "msg", "data" }`，`code === "0"` 为成功。

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/books?keyword=&page=0&size=10` | 浏览书籍（分页 + 模糊搜索） |
| GET | `/api/books/{id}` | 书籍详情 |
| POST | `/api/books` | 新增书籍（201） |
| GET | `/api/members` | 会员列表 |
| POST | `/api/loans` | 借阅（body：`{"bookId":1,"memberId":1}`，201） |
| POST | `/api/loans/{id}/return` | 归还（header：`X-Member-Id: 1` 校验归属） |
| GET | `/api/loans?memberId=1&activeOnly=false` | 会员借阅记录 |
| GET | `/api/stats/overview` | 统计概览（并行聚合） |
| GET | `/api/events/recent` | 全局最近操作记录 |
| GET | `/api/events/members/{memberId}` | 某会员的操作记录 |
| GET | `/api/events/active-count` | 产生过事件的会员数 |
| GET | `/api/probe/db?sec=0.5` | 并发探针（诊断用） |

错误码与 HTTP 状态映射：

| 业务码 | HTTP | 场景 |
|--------|------|------|
| BOOK_NOT_FOUND / MEMBER_NOT_FOUND / LOAN_NOT_FOUND | 404 | 资源不存在 |
| BOOK_NOT_AVAILABLE / ALREADY_BORROWED / ISBN_DUPLICATE | 409 | 无可借 / 重复借阅 / 唯一键冲突 |
| LOAN_NOT_ACTIVE | 400 | 归还非活跃记录 |
| NOT_OWNER | 403 | 归还他人记录 |
| **DB_BUSY** | **503** | **DB 并发达上限（信号量）** |
| 参数校验失败 | 400 | `@Valid` 不通过 |
| 兜底 | 500 | 未预期异常 |

---

## 九、领域模型

```
Member (会员) 1 ──────< Loan (借阅记录) >────── 1 Book (书籍)
```

- **Book**：`isbn` 唯一；`total_copies` 总册数 / `available_copies` 可借册数；`version` 版本号。
- **Member**：轻量实体，仅支撑借阅归属（不含密码，鉴权用 `X-Member-Id` header 简化）。
- **Loan**：`loan_date` 借出 / `due_date` 应还（+14 天）/ `return_date` 实还 / `status`（ACTIVE / RETURNED）。

约束：同一会员对同一本书存在 ACTIVE 记录时不可重复借阅（应用层 + DB `NOT EXISTS` 双重校验）。

---

## 十、目录结构

```
elibrary/
├── mvnw / mvnw.cmd                 # Maven Wrapper，免装 Maven
├── pom.xml
├── README.md
└── src/main/
    ├── java/com/example/elibrary/
    │   ├── ElibraryApplication.java       # 启动类
    │   ├── common/
    │   │   ├── dto/                       # 请求/响应 DTO（含入参校验）
    │   │   ├── exception/                 # BusinessException + GlobalExceptionHandler
    │   │   └── response/                  # ResultDto / Code / ResultCode / PageResult / LoanStatus
    │   ├── config/
    │   │   ├── concurrency/
    │   │   │   ├── ThreadPoolConfig.java          # 业务线程池 bizExecutor
    │   │   │   └── DbConcurrencyInterceptor.java  # Semaphore(10) DB 并发闸门
    │   │   ├── ElibraryDataSourceConfig.java      # Druid 数据源
    │   │   └── ElibraryMybatisSourceConfig.java   # MyBatis 装配 + 注册插件
    │   ├── controller/                  # REST 控制器（构造器注入统一风格）
    │   ├── mapper/                      # MyBatis Mapper 接口（含 SystemMapper 探针）
    │   ├── service/
    │   │   ├── event/                   # LoanEvent / LoanEventBus（5 种并发容器）
    │   │   └── impl/                    # 业务实现 + 事务 + 并行聚合
    │   └── vo/                          # DB 映射对象（Lombok @Data + fluent）
    └── resources/
        ├── application.yaml             # Druid + MyBatis 配置（端口 8080，max-active=10）
        ├── mapper/                      # MyBatis XML（含 SystemMapper.xml）
        ├── static/index.html            # 单页前端
        └── schema-mysql.sql             # 建表 DDL + 种子数据（含中文 COMMENT）
```
