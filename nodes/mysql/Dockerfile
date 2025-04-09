FROM mysql:5.7
RUN yum install -y php-cli
RUN rm -rf /var/lib/apt/lists/*
COPY setup.php /tmp/
COPY setup.sh /docker-entrypoint-initdb.d/
