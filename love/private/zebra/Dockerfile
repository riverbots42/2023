FROM maven as build
RUN mkdir -p /maven/src/main/java/org/riverbots/zebra
ADD pom.xml /maven
RUN cd /maven && mvn dependency:resolve-plugins dependency:resolve clean package
ADD *.java /maven/src/main/java/org/riverbots/zebra/
RUN cd /maven && mvn clean package && cp target/printer-1.0-jar-with-dependencies.jar zebra-printer.jar
RUN jar -tf /maven/target/printer-1.0.jar

FROM ibmjava:jre
COPY --from=build /maven/zebra-printer.jar /zebra-printer.jar
EXPOSE 8080
CMD ["java", "-jar", "zebra-printer.jar"]
