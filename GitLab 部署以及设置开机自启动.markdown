# GitLab 部署与开机自启动配置流程

本文档详细描述了在 Ubuntu 系统上部署 GitLab（社区版，CE）的流程，以及如何配置 systemd 服务以实现 GitLab 的开机自启动。以下步骤基于 GitLab CE 最新版本（截至 2025 年 4 月）和 Ubuntu 系统，适用于类似环境。

## 环境要求
- **操作系统**：Ubuntu（或其他支持 systemd 的 Linux 发行版）
- **依赖软件**：
  - Docker
  - Docker Compose
- **硬件要求**：
  - 至少 4GB 内存（建议 8GB+）
  - 足够磁盘空间（建议 50GB+，具体取决于项目和用户数据）
- **网络**：开放所需端口（默认 HTTP 端口 80、HTTPS 端口 443、SSH 端口 22）

## 部署 GitLab

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

### 2. 创建 GitLab 部署目录
1. **创建目录**：
   ```bash
   mkdir -p /home/$USER/services/gitlab
   cd /home/$USER/services/gitlab
   ```

2. **创建 Docker Compose 配置文件**：
   ```bash
   nano docker-compose.yml
   ```

   添加以下内容：
   ```yaml
   version: '3.7'
   services:
     gitlab:
       image: gitlab/gitlab-ce:latest
       container_name: gitlab
       restart: always
       hostname: 'gitlab.example.com'
       environment:
         GITLAB_OMNIBUS_CONFIG: |
           external_url 'http://gitlab.example.com:9080'
           gitlab_rails['gitlab_shell_ssh_port'] = 2223
       ports:
         - '9080:9080'
         - '443:443'
         - '2223:22'
       volumes:
         - './config:/etc/gitlab'
         - './logs:/var/log/gitlab'
         - './data:/var/opt/gitlab'
       shm_size: '256m'
   ```

   说明：
   - `hostname` 和 `external_url`：替换为你的服务器 IP（例如 `192.168.x.x`）或域名。
   - `ports`：映射 HTTP 端口到 9080（避免与默认 80 冲突），SSH 端口到 2223。
   - `volumes`：持久化 GitLab 配置、日志和数据。
   - `shm_size`：设置共享内存大小，防止内存不足问题。

### 3. 启动 GitLab
1. **启动 GitLab 容器**：
   ```bash
   sudo /usr/local/bin/docker-compose -f docker-compose.yml up -d
   ```

2. **验证容器状态**：
   ```bash
   docker ps
   ```

   应看到 `gitlab` 容器，状态为 `Up` 或 `health: starting`。初始化可能需要几分钟。

3. **获取初始管理员密码**：
   GitLab 首次启动会生成一个随机管理员密码，存储在配置文件中：
   ```bash
   sudo cat /home/$USER/services/gitlab/config/initial_root_password
   ```

   记录密码（默认用户为 `root`）。

4. **访问 GitLab**：
   在浏览器访问 `http://<hostname>:9080`（例如 `http://192.168.x.x:9080`）。使用 `root` 用户和初始密码登录。

   如果无法访问，检查：
   - 端口是否开放：
     ```bash
     sudo netstat -tuln | grep 9080
     sudo ufw status
     ```
   - 容器日志：
     ```bash
     docker logs gitlab
     ```

### 4. 配置 GitLab（可选）
1. **修改外部 URL**：
   如果需要更改访问地址或启用 HTTPS，编辑 `docker-compose.yml` 中的 `GITLAB_OMNIBUS_CONFIG`：
   ```yaml
   external_url 'https://gitlab.example.com'
   nginx['ssl_certificate'] = '/etc/gitlab/ssl/cert.pem'
   nginx['ssl_certificate_key'] = '/etc/gitlab/ssl/key.pem'
   ```

   将 SSL 证书和密钥放入 `./config/ssl/` 目录，然后重启：
   ```bash
   sudo /usr/local/bin/docker-compose -f docker-compose.yml down
   sudo /usr/local/bin/docker-compose -f docker-compose.yml up -d
   ```

2. **备份配置**：
   备份 `docker-compose.yml` 和 `./config` 目录：
   ```bash
   cp docker-compose.yml ~/docker-compose.yml.bak
   tar -czf ~/gitlab-config-backup.tar.gz config
   ```

## 配置开机自启动

### 1. 创建 systemd 服务文件
1. **编辑服务文件**：
   ```bash
   sudo nano /etc/systemd/system/gitlab.service
   ```

2. **添加以下内容**：
   ```ini
   [Unit]
   Description=GitLab Community Edition
   After=docker.service systemd-networkd.service systemd-resolved.service
   Requires=docker.service
   Documentation=https://docs.gitlab.com/

   [Service]
   Type=oneshot
   RemainAfterExit=yes
   Restart=on-failure
   RestartSec=5
   WorkingDirectory=/home/$USER/services/gitlab
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
   sudo systemctl enable gitlab.service
   ```

2. **启动服务**：
   ```bash
   sudo systemctl start gitlab.service
   ```

3. **检查服务状态**：
   ```bash
   systemctl status gitlab.service
   ```

   预期输出显示 `Active: active (exited)`，并且 `docker ps` 列出 `gitlab` 容器。

### 4. 验证开机自启动
重启系统并检查：
```bash
sudo reboot
systemctl status gitlab.service
docker ps
```

如果服务未自动启动，重新运行 `sudo systemctl enable gitlab.service`。

## 常见问题排查
1. **容器启动缓慢或不健康**：
   - 查看容器日志：
     ```bash
     docker logs gitlab
     ```
   - 确保内存充足（至少 4GB），检查：
     ```bash
     free -m
     ```

2. **端口冲突**：
   - 检查端口占用：
     ```bash
     sudo netstat -tuln | grep '9080\|443\|2223'
     ```
   - 解决：修改 `docker-compose.yml` 中的端口映射，重新启动服务。

3. **初始密码丢失**：
   - 如果丢失 `initial_root_password`，重置密码：
     ```bash
     docker exec -it gitlab gitlab-rails console
     user = User.find_by_username('root')
     user.password = 'newpassword'
     user.password_confirmation = 'newpassword'
     user.save!
     exit
     ```

4. **服务启动失败**：
   - 检查 systemd 日志：
     ```bash
     journalctl -xeu gitlab.service -b
     ```
   - 确认 `WorkingDirectory` 存在且包含 `docker-compose.yml`：
     ```bash
     ls -ld /home/$USER/services/gitlab
     ls /home/$USER/services/gitlab/docker-compose.yml
     ```

## 总结
通过以上步骤，你可以在 Ubuntu 上成功部署 GitLab CE 并配置开机自启动。关键点包括：
- 使用 Docker Compose 简化 GitLab 部署和配置。
- 配置 systemd 服务确保 GitLab 随系统启动。
- 定期备份配置和检查容器状态以维护 GitLab 的正常运行。

如需进一步优化（例如 SMTP 配置、备份策略），请参考 [GitLab 官方文档](https://docs.gitlab.com/)。