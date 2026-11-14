CREATE TABLE admin_account (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 username VARCHAR(64) NOT NULL UNIQUE,
 password_hash VARCHAR(255) NOT NULL,
 role VARCHAR(20) NOT NULL,
 enabled BOOLEAN NOT NULL DEFAULT TRUE,
 auth_version INT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE content (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 type VARCHAR(20) NOT NULL,
 title VARCHAR(180) NOT NULL,
 category VARCHAR(80) NOT NULL DEFAULT '',
 summary VARCHAR(600) NOT NULL DEFAULT '',
 body LONGTEXT NOT NULL,
 cover_id BIGINT,
 metadata LONGTEXT NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
 sort_order INT NOT NULL DEFAULT 0,
 featured BOOLEAN NOT NULL DEFAULT FALSE,
 pinned BOOLEAN NOT NULL DEFAULT FALSE,
 archived BOOLEAN NOT NULL DEFAULT FALSE,
 version INT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 published_at TIMESTAMP NULL,
 INDEX idx_content_public (status, type, sort_order, id)
);
CREATE TABLE media_file (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 stored_name VARCHAR(80) NOT NULL UNIQUE,
 original_name VARCHAR(255) NOT NULL,
 mime_type VARCHAR(80) NOT NULL,
 byte_size BIGINT NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE content_media (
 content_id BIGINT NOT NULL,
 media_id BIGINT NOT NULL,
 PRIMARY KEY (content_id, media_id),
 FOREIGN KEY (content_id) REFERENCES content(id),
 FOREIGN KEY (media_id) REFERENCES media_file(id)
);
CREATE TABLE audit_log (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 actor VARCHAR(64) NOT NULL,
 action VARCHAR(64) NOT NULL,
 target VARCHAR(160) NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO content(type,title,category,summary,body,metadata,status) VALUES
 ('PAGE','关于我们','about','用技术连接想法，用实践探索人工智能。','<h2>人工智能社</h2><p>社团介绍、竞赛科研方向、组织架构、联系方式：待补充。</p><p>指导老师及相关资料将在获得公开授权后更新。</p>','{}','PUBLISHED'),
 ('PAGE','加入我们','join','与对人工智能充满好奇的同学一起探索。','<h2>从好奇开始</h2><p>招新对象、技术方向、加入方式：待补充。</p><h3>常见问题</h3><p>是否需要编程基础？招新条件待补充，以社团正式通知为准。</p><p>报名链接与联系二维码：待补充。</p>','{}','PUBLISHED'),
 ('PAGE','人工智能社','home','让好奇发生，让想法生长。','<p>聚焦人工智能、竞赛实践与科研探索，连接每一个热爱技术的你。</p>','{}','PUBLISHED');
