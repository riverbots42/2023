FROM tomcat:9

RUN apt update && apt install -y vim
RUN mkdir -p /usr/local/tomcat/webapps/ROOT/META-INF
ADD love.properties /usr/local/tomcat/webapps/ROOT/META-INF/
RUN mkdir -p /usr/local/tomcat/webapps/ROOT/WEB-INF/lib
ADD lib/*.jar /usr/local/tomcat/webapps/ROOT/WEB-INF/lib/
ADD webapps/* /usr/local/tomcat/webapps/ROOT/

EXPOSE 8080
