# Harbor 部署与开机自启动配置流程

本文档详细描述了在 Ubuntu 系统上部署 Harbor 容器注册中心（Container Registry）的流程，以及如何配置 systemd 服务以实现 Harbor 的开机自启动。以下步骤基于 Harbor v2.4.0 和 Ubuntu 系统，适用于类似环境。

## 环境要求
- **操作系统**：Ubuntu（或其他支持 systemd 的 Linux 发行版）
- **依赖软件**：
  - Docker
  - Docker Compose
- **硬件要求**：
  - 至少 2GB 内存
  - 足够磁盘空间（建议 20GB+，具体取决于镜像存储需求）
- **网络**：开放所需端口（默认 HTTP 端口 8080 或自定义端口，如 8081）

## 部署 Harbor

### 1. 安装 Docker 和 Docker Compose
1. **安装 Docker**：
   ```bash
   sudo apt update
   sudo apt install -y docker.io
   sudo systemctl start docker
   sudo systemctl enable docker
   ```

2. **安装 Docker Compose**：
   下载最新版本的 Docker Compose（以下以 v2.x 为例，具体版本请参考官方文档）：
   ```bash
   sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
   sudo chmod +x /usr/local/bin/docker-compose
   ```

   验证安装：
   ```bash
   docker-compose --version
   ```

### 2. 下载并解压 Harbor
1. **下载 Harbor 离线安装包**：
   访问 [Harbor GitHub Releases 页面](https://github.com/goharbor/harbor/releases)，下载最新版本的离线安装包（例如 `harbor-offline-installer-v2.4.0.tgz`）：
   ```bash
   wget https://github.com/goharbor/harbor/releases/download/v2.4.0/harbor-offline-installer-v2.4.0.tgz
   ```

2. **解压安装包**：
   ```bash
   tar xvf harbor-offline-installer-v2.4.0.tgz
   mv harbor /home/$USER/services/harbor
   cd /home/$USER/services/harbor
   ```

   解压后，目录 `/home/$USER/services/harbor` 应包含以下关键文件：
   - `docker-compose.yml`：Harbor 的 Docker Compose 配置文件
   - `harbor.yml.tmpl`：Harbor 配置模板
   - `install.sh`：安装脚本
   - `prepare`：配置生成脚本

### 3. 配置 Harbor
1. **复制并编辑配置文件**：
   ```bash
   cp harbor.yml.tmpl harbor.yml
   nano harbor.yml
   ```

   修改以下关键字段：
   - `hostname`：设置 Harbor 的访问地址（例如 `192.168.x.x` 或域名）
   - `http.port`：设置 HTTP 端口（默认 8080，可改为 8081 等）
   - `harbor_admin_password`：设置管理员密码（默认用户为 `admin`）
   - （可选）`https`：为生产环境配置 SSL 证书

   示例配置片段：
   ```yaml
   hostname: 192.168.x.x
   http:
     port: 8081
   harbor_admin_password: YourSecurePassword
   ```

2. **生成配置**：
   运行 `prepare` 脚本更新 `docker-compose.yml`：
   ```bash
   ./prepare
   ```

### 4. 启动 Harbor
1. **手动启动 Harbor**：
   ```bash
   sudo /usr/local/bin/docker-compose -f docker-compose.yml up -d
   ```

2. **验证容器状态**：
   ```bash
   docker ps
   ```

   应看到 9 个 Harbor 容器（`harbor-core`、`nginx`、`harbor-db` 等），状态为 `Up` 或 `health: starting`。

3. **访问 Harbor**：
   在浏览器访问 `http://<hostname>:<port>`（例如 `http://192.168.x.x:8081`）。使用 `admin` 用户和 `harbor.yml` 中设置的密码登录。

   如果无法访问，检查：
   - 端口是否开放：
     ```bash
     sudo netstat -tuln | grep 8081
     sudo ufw status
     ```
   - 容器日志：
     ```bash
     docker logs nginx
     docker logs harbor-core
     ```

## 配置开机自启动

### 1. 创建 systemd 服务文件
1. **编辑服务文件**：
   ```bash
   sudo nano /etc/systemd/system/harbor.service
   ```

2. **添加以下内容**：
   ```ini
   [Unit]
   Description=Harbor Container Registry
   After=docker.service systemd-networkd.service systemd-resolved.service
   Requires=docker.service
   Documentation=http://github.com/vmware/harbor

   [Service]
   Type=oneshot
   RemainAfterExit=yes
   Restart=on-failure
   RestartSec=5
   WorkingDirectory=/home/$USER/services/harbor
   ExecStart=/usr/local/bin/docker-compose -f docker-compose.yml up -d
   ExecStop=/usr/local/bin/docker-compose -f docker-compose.yml down
   TimeoutStartSec=0

   [Install]
   WantedBy=multi-user.target
   ```

   注意：
   - 将 `WorkingDirectory` 中的 `$USER` 替换为实际用户名（例如 `johnqing`）。
   - `Type=oneshot` 适合 `docker-compose up -d` 的行为，因为它启动容器后退出。
   - `RemainAfterExit=yes` 确保 systemd 认为服务在命令退出后仍处于活动状态。

### 2. 重新加载 systemd 配置
```bash
sudo systemctl daemon-reload
```

### 3. 启用并启动服务
1. **启用服务（开机自启动）**：
   ```bash
   sudo systemctl enable harbor.service
   ```

2. **启动服务**：
   ```bash
   sudo systemctl start harbor.service
   ```

3. **检查服务状态**：
   ```bash
   systemctl status harbor.service
   ```

   预期输出显示 `Active: active (exited)`，并且 `docker ps` 列出所有 Harbor 容器。

### 4. 验证开机自启动
重启系统并检查：
```bash
sudo reboot
systemctl status harbor.service
docker ps
```

如果服务未自动启动，重新运行 `sudo systemctl enable harbor.service`。

## 常见问题排查
1. **CHDIR 错误**：
   - 错误：`harbor.service: Changing to the requested working directory failed: No such file or directory`
   - 解决：确认 `WorkingDirectory` 路径存在（例如 `/home/johnqing/services/harbor`）且包含 `docker-compose.yml`：
     ```bash
     ls -ld /home/johnqing/services/harbor
     ls /home/johnqing/services/harbor/docker-compose.yml
     ```

2. **端口冲突**：
   - 检查端口占用：
     ```bash
     sudo netstat -tuln | grep 8081
     ```
   - 解决：修改 `harbor.yml` 中的 `http.port` 或 `docker-compose.yml` 中的端口映射，重新运行 `./prepare` 和重启服务。

3. **容器未启动或不健康**：
   - 查看容器日志：
     ```bash
     docker logs harbor-core
     docker logs harbor-db
     ```
   - 确保所有镜像已正确拉取，且 `harbor.yml` 配置无误。

4. **服务文件冲突**：
   - 如果 `/usr/lib/systemd/system/harbor.service` 存在，备份或删除以避免与 `/etc/systemd/system/harbor.service` 冲突：
     ```bash
     sudo mv /usr/lib/systemd/system/harbor.service /usr/lib/systemd/system/harbor.service.bak
     sudo systemctl daemon-reload
     ```

## 总结
通过以上步骤，你可以在 Ubuntu 上成功部署 Harbor 并配置开机自启动。关键点包括：
- 正确配置 `harbor.yml` 和 `docker-compose.yml`。
- 使用 systemd 服务确保 Harbor 随系统启动。
- 定期检查容器状态和日志以维护 Harbor 的正常运行。

如需进一步优化（例如 HTTPS 配置、存储扩展），请参考 [Harbor 官方文档](https://goharbor.io/docs/)。