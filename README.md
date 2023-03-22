<p align="center">
    <img alt="logo" src="https://oscimg.oschina.net/oscnet/up-d3d0a9303e11d522a06cd263f3079027715.png">
</p>
<h1 align="center" style="margin: 30px 0 30px; font-weight: bold;">RuoYi v3.8.5</h1>
<h4 align="center">基于SpringBoot+Vue前后端分离的Java快速开发框架</h4>
<p align="center">
    <a href="https://gitee.com/y_project/RuoYi-Vue/stargazers"><img src="https://gitee.com/y_project/RuoYi-Vue/badge/star.svg?theme=dark"></a>
    <a href="https://gitee.com/y_project/RuoYi-Vue"><img src="https://img.shields.io/badge/RuoYi-v3.8.5-brightgreen.svg"></a>
    <a href="https://gitee.com/y_project/RuoYi-Vue/blob/master/LICENSE"><img src="https://img.shields.io/github/license/mashape/apistatus.svg"></a>
</p>

## 项目说明

本项目是在RuoYi开发框架中集成Nop平台的示例应用。后端基于[RuoYi v3.8.5](https://gitee.com/y_project/RuoYi-Vue)。

后端代码： [nop-for-ruoyi](https://gitee.com/canonical-entropy/nop-for-ruoyi)
前端代码: [nop-for-ruoyi-vue3](https://gitee.com/canonical-entropy/nop-for-ruoyi-vue3)

## 集成步骤

### 1. 在pom文件中引入依赖
修改项目根目录下的pom文件，将nop平台的nop-spring-web-start模块和nop-sys-web模块和nop加入依赖管理

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.entropy-cloud</groupId>
            <artifactId>nop-spring-web-starter</artifactId>
            <version>${nop-entropy.version}</version>
        </dependency>

        <dependency>
            <groupId>io.github.entropy-cloud</groupId>
            <artifactId>nop-sys-web</artifactId>
            <version>${nop-entropy.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```


### 2. 在启动工程项目中引入nop-spring-web-starter模块。

在ruoyi-admin模块中引入nop-spring-web-starter和nop-sys-web模块，其中nop-spring-web-starter模块是实现Springboot与Nop平台集成所必须的，
而引入nop-sys-web模块是为了使用NopSysSequence顺序号生成器表以及演示如何集成Nop平台生产的模块。
```xml
<dependencies>
    <dependency>
        <groupId>io.github.entropy-cloud</groupId>
        <artifactId>nop-spring-web-starter</artifactId>
    </dependency>

    <dependency>
        <groupId>io.github.entropy-cloud</groupId>
        <artifactId>nop-sys-web</artifactId>
    </dependency>
</dependencies>
```

在application.yaml中增加配置
```yaml
nop:
  debug: true
  orm:
    init-database-schema: true
```

这样会以调试模式启动，并自动根据ORM模型创建NopSysSequence等所需要的数据库表。


* **与SpringBoot集成的实现原理**：
在nop-spring-web-starter模块的NopSpringWebAutoConfig配置类中会监听Spring容器的启动事件，当Spring容器启动完毕后再启动Nop平台。

一般情况下基于Nop平台开发的模块并不依赖于Spring框架，可以独立开发并使用自动生成的xxx-app来测试，最后再在ruoyi工程中引入对应模块来打包为spring服务。

### 3. 覆盖Nop平台中的DataSource配置，直接使用Spring的DataSource
增加`resources/_vfs/_delta/default/nop/dao/beans/dao-defaults.beans.xml`，通过可逆计算取消Nop平台内置的nopDataSource，并将Spring中的dynamicDataSource映射为nopDataSource。

```xml
<beans x:schema="/nop/schema/beans.xdef" xmlns:x="/nop/schema/xdsl.xdef"
       x:extends="super" x:dump="true">
    <bean id="nopDataSource" x:override="remove" />

    <bean id="nopHikariConfig" x:override="remove" />

    <alias name="dynamicDataSource" alias="nopDataSource" />
</beans>

```

### 4. 集成Ruoyi的用户登录认证机制
在ruoyi-framework模块中引入nop-spring-web-starter依赖
```xml

        <dependency>
            <groupId>io.github.entropy-cloud</groupId>
            <artifactId>nop-spring-web-starter</artifactId>
        </dependency>
```

在JwtAuthenticationTokenFilter中增加登录后用户上下文的初始化代码
```java
    void initUserContext(LoginUser loginUser) {
        UserContextImpl userContext = new UserContextImpl();
        String userName = loginUser.getUsername();
        userContext.setUserName(userName);
        userContext.setUserId(loginUser.getUserId());
        userContext.setDeptId(String.valueOf(loginUser.getDeptId()));
        userContext.setAccessToken(loginUser.getToken());
        userContext.setSessionId(loginUser.getToken());
        IUserContext.set(userContext);
    }

```


### 5. 集成Ruoyi平台的权限校验机制
在ruoyi-framework模块中增加IActionAuthChecker的实现类

```java
public class NopActionAuthChecker implements IActionAuthChecker {

    @Inject
    PermissionService permissionService;

    @Override
    public boolean isPermitted(String permission, IUserContext iUserContext) {
        return permissionService.hasPermi(permission);
    }
}
```

在定制的dao-defaults.beans.xml中增加配置
```xml
<bean id="actionAuthChecker" class="com.ruoyi.framework.web.service.PermissionService" />
```



## 开发说明
Nop平台是在Spring框架初始化完毕之后创建的，因此在Nop平台开发的模块中可以通过 @Inject标准注解来直接注入Spring容器所管理的类。

而在Spring容器所创建的服务类中，不能自动注入Nop平台所管理的类，但是可以通过BeanContainer获取，例如
```
 IGraphQLEngine engine = BeanContainer.instance().getBeanByType(IGraphQLEngine.class);

 BeanContainer.instance().getBean("myBean");
```

建议使用Nop平台开发的时候，尽量不直接使用Spring相关的类，可以封装为接口后注入到模块中使用。通过这种方式，可以使得相应功能摆脱Spring依赖，未来可以移植到Quarkus框架中使用。

