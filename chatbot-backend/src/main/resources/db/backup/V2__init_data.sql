-- 清空现有数据
DELETE FROM chatbotMb.knowledge_base;

-- 重置序列
ALTER SEQUENCE chatbotMb.knowledge_base_id_seq RESTART WITH 1;

-- 插入产品使用说明
INSERT INTO chatbotMb.knowledge_base (title, content, category, created_at, updated_at) VALUES
('产品安装指南', '1. 下载安装包
2. 双击安装文件
3. 按照安装向导完成安装
4. 首次启动需要进行初始化设置
5. 如遇到问题，请查看故障排除指南', '产品使用', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('常见问题解答', 'Q: 如何重置密码？
A: 点击登录页面的"忘记密码"，通过邮箱验证码重置。

Q: 如何导出数据？
A: 在数据管理页面，点击"导出"按钮，选择导出格式和范围。

Q: 系统支持哪些文件格式？
A: 支持PDF、Word、Excel、图片等常见格式。', '常见问题', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('故障排除指南', '1. 系统无法启动
   - 检查网络连接
   - 确认系统要求是否满足
   - 查看错误日志

2. 数据同步失败
   - 检查网络连接
   - 确认账号权限
   - 联系技术支持

3. 界面显示异常
   - 清除浏览器缓存
   - 更新浏览器版本
   - 尝试其他浏览器', '技术支持', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('数据备份说明', '1. 自动备份
   - 系统每天凌晨2点自动备份
   - 备份文件保留30天
   - 可在设置中修改备份策略

2. 手动备份
   - 进入系统设置
   - 选择"数据管理"
   - 点击"立即备份"

3. 备份恢复
   - 选择备份文件
   - 确认恢复时间点
   - 执行恢复操作', '数据管理', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('用户权限说明', '1. 管理员权限
   - 系统配置管理
   - 用户管理
   - 数据管理
   - 日志查看

2. 普通用户权限
   - 数据查看
   - 基础操作
   - 个人设置

3. 访客权限
   - 只读访问
   - 无修改权限', '系统管理', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('系统更新说明', '最新版本：v2.1.0
更新内容：
1. 新增批量导入功能
2. 优化搜索性能
3. 修复已知问题
4. 界面优化

更新方法：
1. 自动更新：系统会自动检测并提示更新
2. 手动更新：在系统设置中点击"检查更新"', '系统更新', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('API接口文档', '1. 认证接口
   POST /api/auth/login
   参数：username, password
   返回：token

2. 数据接口
   GET /api/data
   参数：page, size
   返回：数据列表

3. 文件接口
   POST /api/file/upload
   参数：file
   返回：fileId', '开发文档', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('安全使用指南', '1. 密码安全
   - 使用强密码
   - 定期更换密码
   - 不要共享账号

2. 数据安全
   - 及时备份数据
   - 加密敏感信息
   - 定期清理缓存

3. 设备安全
   - 及时更新系统
   - 安装安全软件
   - 使用安全网络', '安全指南', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP); 