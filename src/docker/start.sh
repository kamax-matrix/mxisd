#!/usr/bin/env bash
if [[ -n "$CONF_FILE_PATH" ]] && [ ! -f "$CONF_FILE_PATH" ]; then
    echo "Generating config file $CONF_FILE_PATH"
    touch "CONF_FILE_PATH"

    if [[ -n "$MATRIX_DOMAIN" ]]; then
        echo "Setting matrix domain to $MATRIX_DOMAIN"
        echo "matrix.domain: $MATRIX_DOMAIN" >> "$CONF_FILE_PATH"
    fi

    if [[ -n "$SIGN_KEY_PATH" ]]; then
        echo "Setting signing key path to $SIGN_KEY_PATH"
        echo "key.path: $SIGN_KEY_PATH" >> "$CONF_FILE_PATH"
    fi

    if [[ -n "$SQLITE_DATABASE_PATH" ]]; then
        echo "Setting SQLite DB path to $SQLITE_DATABASE_PATH"
        echo "storage.provider.sqlite.database: $SQLITE_DATABASE_PATH" >> "$CONF_FILE_PATH"
    fi

    echo "Starting mxisd..."
    echo
fi

exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /mxisd.jar
