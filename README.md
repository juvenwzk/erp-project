# ERP 进销存管理系统

基于 **Spring Boot 3 + MyBatis + MySQL + Vue 3** 的进销存系统，支持 **Docker Compose 一键部署**。

- 商品 / 客户 / 供应商 / 采购 / 销售 / 库存
- JWT 登录与角色权限
- Spring AI 智能助手（DeepSeek，可选）
- 前端 Nginx 反向代理，访问端口 **100**

---

## 环境要求

| 方式 | 需要安装 |
|------|----------|
| **Docker 部署（推荐）** | [Docker Desktop](https://www.docker.com/products/docker-desktop/) |
| **本地开发** | JDK 21、Maven 3.9+、MySQL 8.0 |

---

## Docker 一键启动（他人电脑同样步骤）

### 1. 获取项目

```bash
git clone https://github.com/juvenwzk/erp-project.git
cd erp-project
```

或解压你收到的 ZIP 包后进入项目根目录。

### 2. 配置环境变量

**Windows PowerShell：**

```powershell
copy .env.example .env
```

**Linux / macOS：**

```bash
cp .env.example .env
```

编辑 `.env`：

```env
MYSQL_USER=ERP_USER
MYSQL_PASSWORD=你的强密码
MYSQL_ROOT_PASSWORD=你的强密码
MYSQL_DATABASE=erp
DEEPSEEK_API_KEY=你的DeepSeek密钥   # 不用 AI 可留空
```

应用连接数据库使用 **`ERP_USER` 业务账号**，不再使用 root。若之前已用其他用户跑过 Docker，需执行一次 `docker compose down -v` 后重新 `up` 以创建新用户。

### 3. 启动

```powershell
docker compose up -d --build
```

首次启动会：拉取镜像 → 在容器内 Maven 编译 → 初始化 MySQL → 启动后端与 Nginx。  
约 **5～15 分钟**（视网络而定）。

### 4. 查看状态

```powershell
docker compose ps
```

应看到 `erp-mysql`、`erp-app`、`erp-nginx` 均为 `running`。

### 5. 访问

浏览器打开：

```
http://localhost:100/login.html
```

默认账号（与 `docker/mysql/init/mysqldump.sql` 一致）：

| 用户名 | 密码 |
|--------|------|
| admin | 123456 |

### 6. 常用命令

```powershell
# 查看日志
docker compose logs -f erp-app

# 停止
docker compose stop

# 停止并删除容器（保留数据库卷）
docker compose down

# 清空数据库并重新初始化（修改 SQL 后使用）
docker compose down -v
docker compose up -d --build
```

---

## 局域网访问（同一 WiFi 下其他设备）

1. 本机已 `docker compose up -d`
2. 查本机 IP：`ipconfig`（Windows）→ 如 `192.168.1.105`
3. 其他设备浏览器访问：`http://192.168.1.105:100/login.html`
4. 若无法访问，在 Windows 防火墙中放行 **100** 端口

---

## 本地开发（不用 Docker）

### 1. 数据库

- 安装 MySQL 8.0，创建库 `erp`
- 导入 `docker/mysql/init/mysqldump.sql`

### 2. 配置

**方式 A（推荐）**：复制本地私密配置（已 gitignore，不会提交）：

```powershell
copy erp-server\src\main\resources\application-local.yaml.example erp-server\src\main\resources\application-local.yaml
```

编辑 `application-local.yaml` 中的 `password`，与 MySQL 里 `ERP_USER` 的密码一致。启动时会自动加载。

**方式 B**：在 IDEA 运行配置 **Environment variables** 中设置：

```
MYSQL_USER=ERP_USER;MYSQL_PASSWORD=你的密码;MYSQL_DATABASE=erp
```

若本地尚未创建 `ERP_USER`，用 root 执行 [`scripts/sql/create_erp_user.sql`](scripts/sql/create_erp_user.sql)（执行前请修改脚本中的密码）。该用户仅拥有 `erp` 库的 **SELECT / INSERT / UPDATE / DELETE**，不能建表、删库或授权。

### 3. 启动后端

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
cd erp-project
mvn clean package -Dmaven.test.skip=true
java -jar erp-server/target/erp-server-1.0-SNAPSHOT.jar
```

### 4. 前端

将 `docker/nginx/html/` 配置到本地 Nginx（端口 100），或使用项目内已配置的 Nginx 目录。

---

## 项目结构

```
erp-project/
├── erp-pojo/          # 实体与 DTO
├── erp-utils/         # 工具类
├── erp-server/        # Spring Boot 主模块
├── docker/
│   ├── mysql/init/    # 数据库初始化 SQL
│   └── nginx/         # 前端静态页 + 反向代理配置
├── Dockerfile
├── docker-compose.yml
└── pom.xml
```

---

## 技术栈

- Java 21、Spring Boot 3.5、MyBatis、MySQL 8、JWT
- Vue 3（打包静态资源）
- Docker、Nginx、Spring AI（DeepSeek）

---

## 常见问题

| 现象 | 处理 |
|------|------|
| 端口 100 被占用 | 修改 `docker-compose.yml` 中 `"100:100"` 为 `"8088:100"`，访问 `localhost:8088` |
| 登录失败 | 使用 `admin` / `123456`；或 `docker compose down -v` 重建库 |
| AI 无响应 | 检查 `.env` 中 `DEEPSEEK_API_KEY` |
| 构建很慢 | 首次需下载 Maven 依赖与 Docker 镜像，属正常现象 |

---

## 作者

王照康 · 广东白云学院

GitHub: https://github.com/juvenwzk/erp-project
