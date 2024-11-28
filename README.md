# HomeEcoClimate (servidor)

Sistema domótico de control del clima interior de un hogar bajo criterios de ahorro energético.
Parte de servidor con API REST y control de los clientes mediante MQTT.

## Ejecución

Desde la raíz del proyecto, y con JDK 11+ instalado, introducir la siguiente instrucción en el terminal:

```sh
$ vertx run sensor.Server
```

Otras formas de ejecución pueden encontrarse en <https://vertx.io/docs/vertx-core/java/#_the_vertx_command_line>.

## Configuración

El servidor reconoce las siguientes variables de entorno para su configuración:

* `HOMEECOCLIMATE_DB_USER`: Nombre de usuario de acceso a la base de datos. _Requerido_.
* `HOMEECOCLIMATE_DB_PASS`: Contraseña de acceso a la base de datos. _Requerido_.
* `HOMEECOCLIMATE_MQTT_ADDRESS`: Dirección del broker MQTT. _Opcional_, `localhost` por defecto.
* `HOMEECOCLIMATE_MQTT_PORT`: Puerto del broker MQTT. _Opcional_, `1883` por defecto.

En caso de que no se pueda acceder a la base de datos persistente o no se especifiquen los datos
requeridos, el servidor empleará una base de datos local en memoria. **El servidor no se iniciará
si no se puede conectarse al broker MQTT.**

## Rutas de la API REST

* **GET /:resource_type** &rarr; Recolectar todos los registros de este recurso. Si se añade un
parámetro de query `lastseconds` con el número de segundos a abarcar en la consulta desde ahora
al pasado se puede limitar el rango temporal.
* **GET /:resource_type/:group_id/:sensor_id** &rarr; Recolectar todos los registros de este recurso
con los identificadores de grupo y sensor indicados.
* **GET /:resource_type/:group_id/:sensor_id/:time** &rarr; Devolver el registro de este recurso con
los identificadores de grupo y sensor y la marca de tiempo indicados. Debe ser único.
* **POST /:resource_type** &rarr; Añadir un registro del tipo `resource_type`. No requiere del campo
`time` en el cuerpo JSON, aunque si se envía igualmente será reescrito por la marca de tiempo del
servidor. Cada vez que es llamado envía un comando como mensaje MQTT para actualizar el estado
requerido de los dispositivos de climatización.

Los posibles `resource_type` pueden ser los registros correspondientes a la temperatura (_temperature_)
o la humedad (_humidity_). Todos tienen como campos comunes el ID de grupo, el ID de sensor y la marca
de tiempo. Sin embargo, según el tipo de medida, tendrá unos campos u otros. La estructura para cada
caso está definida tanto en el esquema definido en `sql_server/create_tables.sql` como en las clases
que se encuentran en el proyecto en `sensor.data`.

## Emisión de comandos por MQTT

Las instrucciones a los actuadores son comunes dentro de cada ID de grupo y tipo de dispositivo dentro
de él nombrado según qué magnitud controle. Por esto, las etiquetas de los mensajes siguen la
estructura `group_id/actuator_type`. Este último puede ser `fanSpeed` para el ventilador, `tempIndex`
para la regulación de temperatura (manifestada por un motor servo) o `angle` para el ángulo de la
persiana.

La(s) cifra(s) enviada(s) se basa(n) en el cálculo de un índice promedio de todos los registros de los
últimos 60 segundos dentro de cada grupo. Cada tipo de sensor tiene asignado un gestor de esta lógica
de negocio en el paquete `actuator` que hereda de la clase abstracta `actuator.common.CommandPublisher`.

Para más detalles, el proyecto se encuentra documentado en el código fuente mediante anotaciones Javadoc.
