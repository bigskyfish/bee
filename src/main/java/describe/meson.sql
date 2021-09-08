create table meson_server (
    id int  not null,
    ip int null,
    user int null,
    password int null,
    port int null,
    status int default 0 null,
    primary key (id)
) comment '服务信息表';