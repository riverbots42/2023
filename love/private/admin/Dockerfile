FROM tomcat:9

RUN apt update && apt install -y vim
ADD love.properties.enc /love.properties.enc
RUN mkdir -p /usr/local/tomcat/webapps/ROOT/META-INF
RUN mkdir -p /usr/local/tomcat/webapps/ROOT/WEB-INF/lib
ADD lib/*.jar /usr/local/tomcat/webapps/ROOT/WEB-INF/lib/
ADD webapps/* /usr/local/tomcat/webapps/ROOT/
ADD start.sh /
RUN chmod 0755 /start.sh

CMD ["/start.sh"]

EXPOSE 8080
