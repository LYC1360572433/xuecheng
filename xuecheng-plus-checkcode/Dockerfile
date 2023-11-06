# Docker 镜像构建
# 该docker镜像依赖于哪个基础镜像
FROM maven:3.5-jdk-8-alpine as builder

# Copy local code to the container image.
# 指定镜像的工作目录，所有代码都放在该目录中
WORKDIR /app

ADD xuecheng-plus-checkcode-0.0.1-SNAPSHOT.jar xuecheng-plus-checkcode.jar
# Run the web service on container startup.
# 运行镜像默认的执行命令
CMD ["java","-jar","-Dfile.encoding=utf-8","/xuecheng-plus-checkcode-0.0.1-SNAPSHOT.jar"]
EXPOSE 63075