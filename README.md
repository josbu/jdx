# JDX

## 📢 特别声明:

1. 本仓库涉及的任何解锁和解密分析脚本或代码，仅用于测试和学习研究，禁止用于商业用途，不能保证其合法性，准确性，完整性和有效性，请根据情况自行判断.
2. 请勿将本项目的任何内容用于商业或非法目的，否则后果自负.
3. 如果任何单位或个人认为该项目的脚本可能涉嫌侵犯其权利，则应及时通知并提供身份证明，所有权证明，我们将在收到认证文件后删除相关代码.
4. 任何以任何方式查看此项目的人或直接或间接使用本仓库项目的任何脚本的使用者都应仔细阅读此声明。JDX 保留随时更改或补充此免责声明的权利。一旦使用并复制了任何本仓库项目的规则，则视为您已接受此免责声明.
5. 您必须在下载后的24小时内从计算机或手机中完全删除以上内容.
6. 您使用或者复制了本仓库且本人制作的任何脚本，则视为已接受此声明，请仔细阅读

## 🐳 安装说明

本项目已打包成`docker`镜像，拉取配置即可使用
> docker安装方法不再赘述

### 1. 拉取并运行docker

```dockerfile
docker run -d \
    -v <config dir>:/jdx/config \
    -p <port>:80 \
    --restart=always \
    --name jdx registry.cn-hangzhou.aliyuncs.com/yiidii-hub/jdx:v0.2.2
```
> 这里命令自行替换卷和端口映射
> 
> 例如：
> ```dockerfile
> docker run -d \
>   -v  /data/jdx/config:/jdx/config \
>   -p 5702:80 \
>   --restart=always \
>   --name jdx registry.cn-hangzhou.aliyuncs.com/yiidii-hub/jdx:v0.2.2
> ```
> 
注意：
 - 记得放行端口

### 2. 前台访问
这时候访问 `http://ip:port/` 就能访问了

### 3. 后台登录
<p style="color: red; font-size: 32px"> 所有涉及配置修改的都在后台管理操作并修改，侧滑即可（不要在修改文件了！！！） </p>
<p style="color: red; font-size: 32px"> 所有涉及配置修改的都在后台管理操作并修改，侧滑即可（不要在修改文件了！！！） </p>
<p style="color: red; font-size: 32px"> 所有涉及配置修改的都在后台管理操作并修改，侧滑即可（不要在修改文件了！！！） </p>
后台地址： `http://ip:port/admin`
首次登录用户名：`admin`, 密码：`123465`, **千万记得修改密码！！！！！**

## 📌 一对一推送
 脚本参考[ccwav/QLScript2](https://github.com/ccwav/QLScript2) ，直接拉库即可

--------------
**接下来就是一对一的配置**

1. 这里一对一的配置需要`wxPusher`的`appToken`，在`wxPusher`后台修改应用回调地址（`wxPusher后台` -> `应用管理` -> `应用信息` -> `事件回调地址`），修改格式如下
    `http://ip:port/api/third/wxPusher/follow/callback`
    > 这里的`ip:port`是`JDX`的`ip:port`
   
    原理是：在扫码获取Cookie的时候，会根据pt_pin生成一个二维码，用户扫码关注后，`wxPusher`会调用`JDX`的该接口，JDX会在青龙备注修改用户的`UID`，格式为`ccwav`采用通知的格式，`@@`分割。


2. 在青龙后台配置`wxPusher`的`appToken`
    ```javascript
    export WP_APP_TOKEN_ONE="AT_xxx"
    ```

## 财富岛api
`http://ip:port/api/cfd`

## 🥂 更新说明
1. 停止并删除容器
```shell
docker stop jdx && docker rm jdx
```
2. 根据最新版本号跑一个新的容器即可

## 讨论群组
[🔗 t.me/jdx_discuss](https://t.me/jdx_discuss)
