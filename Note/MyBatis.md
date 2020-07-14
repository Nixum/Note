# ORM

普通JDBC使用sql的prepareStatement + sql + 占位符 + 参数的方式执行sql语句

ORM其实就是在先编写类与数据库表字段的映射，可以是xml配置，也可以是注解配置，之后再使用JDBC执行sql时，通过对类的反射获得其属性的值和对应的字段名，拼接sql+占位符+属性值，执行sql语句

# MyBatis

#{}和${}区别

#{}：预编译，底层是PrepareStatement ，可防止SQL注入

${}：参数替换，不能防止SQL注入

## dao接口与mapper的映射

* 通过动态代理实现
* 实际上XML在定义Mapper的时候就相当于在编写dao类了，dao接口类相当于编写调用入口
* XML中的配置信息会被存放在Configuration类中，SqlSessionFactoryBuilder会读取Configuration类中的信息创建SqlSessionFactory，之后由SqlSessionFactory创建sqlSession
* 在启动时先扫描并加载所有定义Mapper的XML，解析XML，根据XML中的namespace找到对应的接口，为这些接口生成对应的代理工厂MapperProxyFactory。
* sqlSession.getMapper时，会通过传入的类/接口名（即被代理类）找到对应的MapperProxyFactory，生成代理类MapperProxy，并注入sqlSession，MapperProxy实现InvocationHandler接口进行代理，通过MapperMethod执行SQL，MapperMethod使用注入的sqlSession和解析XML中配置的SQL语句得到的参数，调用对应的executor执行SQL

## 缓存

- **一级缓存的作用域是同一个SqlSession**，在同一个sqlSession中两次执行相同的sql语句，第一次执行完毕会将数据库中查询的数据写到缓存（内存），第二次会从缓存中获取数据将不再从数据库查询，从而提高查询效率。当一个sqlSession结束后该sqlSession中的一级缓存也就不存在了。Mybatis**默认开启一级缓存**。
- **二级缓存是mapper级别的缓存**，多个SqlSession去操作同一个Mapper的sql语句，多个SqlSession去操作数据库得到数据会存在二级缓存区域，多个SqlSession可以共用二级缓存，二级缓存是跨SqlSession的。不同的sqlSession两次执行相同namespace下的sql语句且向sql中传递参数也相同即最终执行相同的sql语句，第一次执行完毕会将数据库中查询的数据写到缓存（内存），第二次会从缓存中获取数据将不再从数据库查询，从而提高查询效率。Mybatis默认没有开启二级缓存需要在setting全局参数中配置开启二级缓存

# 参考

[MyBatis的通俗理解：SqlSession.getMapper()源码分析](https://blog.csdn.net/qq_40645822/article/details/101844675)