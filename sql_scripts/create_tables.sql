DROP TABLE homeecoclimate.humidity;
DROP TABLE homeecoclimate.temperature;

CREATE TABLE homeecoclimate.humidity (
    groupId VARCHAR(36) NOT NULL,
    sensorId VARCHAR(36) NOT NULL,
    time BIGINT NOT NULL,
    ratio DOUBLE NOT NULL,
    PRIMARY KEY (groupId, sensorId, time)
);

CREATE TABLE homeecoclimate.temperature (
    groupId VARCHAR(36) NOT NULL,
    sensorId VARCHAR(36) NOT NULL,
    time BIGINT NOT NULL,
    celsius DOUBLE NOT NULL,
    PRIMARY KEY (groupId, sensorId, time)
);