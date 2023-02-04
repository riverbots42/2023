FROM tomcat:9

RUN mkdir -p /usr/local/tomcat/webapps/ROOT/META-INF
ADD love.properties /usr/local/tomcat/webapps/ROOT/META-INF/
ADD webapps/* /usr/local/tomcat/webapps/ROOT/

EXPOSE 8080
