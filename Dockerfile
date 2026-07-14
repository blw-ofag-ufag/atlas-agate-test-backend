FROM amazoncorretto:25.0.2-alpine3.23

ENV LANGUAGE='en_US:en'

# We make four distinct layers so if there are application changes the library layers can be re-used
COPY target/quarkus-app/lib/ /deployments/lib/
COPY target/quarkus-app/*.jar /deployments/
COPY target/quarkus-app/app/ /deployments/app/
COPY target/quarkus-app/quarkus/ /deployments/quarkus/

RUN mkdir -p /deployments/certs && chown 185 /deployments/certs

EXPOSE 8900
USER 185
ENV JAVA_OPTS="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"

ENTRYPOINT ["sh","-c", "if [ -n \"$MTLS_CLIENT_KEYSTORE_P12_BASE64\" ]; then echo \"$MTLS_CLIENT_KEYSTORE_P12_BASE64\" | base64 -d > /deployments/certs/agate-client.p12; fi; exec java $JAVA_OPTS -jar $JAVA_APP_JAR"]
