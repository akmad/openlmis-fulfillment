FROM anapsix/alpine-java:jre8

COPY build/libs/*.jar /service.jar
# TODO: uncomment when demo data will be added
# COPY build/demo-data /demo-data

EXPOSE 8080
CMD java $JAVA_OPTS -jar /service.jar