# HomeEcoClimate (Server)

Home automation system for indoor climate control based on energy-saving principles.  
Server component with a REST API and MQTT-based client control.

## Execution

From the project root, with JDK 11+ installed, run the following command in the terminal:

```sh
$ vertx run sensor.Server
```

Other execution options can be found at <https://vertx.io/docs/vertx-core/java/#_the_vertx_command_line>.

## Configuration

The server recognizes the following environment variables for its configuration:

* `HOMEECOCLIMATE_DB_USER`: Database username. _Required_.
* `HOMEECOCLIMATE_DB_PASS`: Database password. _Required_.
* `HOMEECOCLIMATE_MQTT_ADDRESS`: MQTT broker address. _Optional_, defaults to `localhost`.
* `HOMEECOCLIMATE_MQTT_PORT`: MQTT broker port. _Opcional_, defaults to `1883`.

If the server cannot access a persistent database or the required variables are not specified, it will use a local in-memory database. **The server will not start if it cannot connect to the MQTT broker.**

## REST API Endpoints

* **GET /:resource_type** &rarr; Retrieve all records of the specified resource. If a `lastseconds` query parameter is provided, limits the query to records within the given number of seconds from now.
* **GET /:resource_type/:group_id/:sensor_id** &rarr; Retrieve all records for the specified resource with the given group and sensor IDs.
* **GET /:resource_type/:group_id/:sensor_id/:time** &rarr; Retrieve the specific record with the given group ID, sensor ID, and timestamp. This record must be unique.
* **POST /:resource_type** &rarr; Add a new record of type `resource_type`. The `time` field in the JSON body is optional but will be overwritten with the server's timestamp if provided. Each call also sends a command via MQTT to update the desired state of the climate control devices.

The possible `resource_type` values correspond to records for temperature (_temperature_) or humidity (_humidity_). All of them share common fields such as group ID, sensor ID, and timestamp. However, depending on the type of measurement, they will have different fields. The structure for each case is defined both in the schema located in `sql_server/create_tables.sql` and in the classes found in the `sensor.data` package of the project.

## MQTT Command Publishing

Commands to the actuators are common within each group ID and device type, named according to the magnitude they control. Therefore, the message labels follow the structure `group_id/actuator_type`. The `actuator_type` can be `fanSpeed` for the fan, `tempIndex` for temperature regulation (controlled by a servo motor), or `angle` for the blind angle.

The value(s) sent are based on the calculation of an average index of all records from the last 60 seconds within each group. Each sensor type has an assigned handler for this business logic in the `actuator` package, which inherits from the abstract class `actuator.common.CommandPublisher`.

For more details, the project is documented in the source code with Javadoc annotations.
