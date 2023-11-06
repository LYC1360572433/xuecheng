# Docker 镜像构建
# 该docker镜像依赖于哪个基础镜像
FROM maven:3.5-jdk-8-alpine as builder

# Copy local code to the container image.
# 指定镜像的工作目录，所有代码都放在该目录中
WORKDIR /app
# 把电脑的代码复制到容器中
COPY src ./src

# Build a release artifact.
# 执行mvn打包命令 如果觉得慢的话 可以直接copy本地的jar过去

# Run the web service on container startup.
# 运行镜像默认的执行命令
CMD ["java","-jar","/app/target/user-center-backend-0.0.1-SNAPSHOT.jar","--spring.profiles.active=prod"]