package sensor;

import java.util.stream.Stream;

import actuator.HumidCmdPublisher;
import actuator.TempCmdPublisher;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.mqtt.MqttClient;
import io.vertx.mysqlclient.MySQLPool;
import sensor.common.DataPool;
import sensor.common.ResourceHandler;
import sensor.data.Humidity;
import sensor.data.Temperature;
import sensor.data.pools.AzureMySQL;
import sensor.data.pools.LocalNonPersistent;

/**
 * Esta clase crea un subrouter por cada tipo de recurso y los une a la raíz
 * de las rutas de la API con el nombre de la clase del tipo de recurso en
 * minúscula como prefijo.
 * 
 * Para introducir las credenciales de acceso a la base de datos de MySQL
 * hay que indicarlas mediante las variables de entorno <b>HOMEECOCLIMATE_DB_USER</b>
 * para el nombre de usuario y <b>HOMEECOCLIMATE_DB_PASS</b> para la contraseña.
 * 
 * Se puede modificar la dirección y puerto por defecto del broker MQTT al
 * que se conectará el servidor, que son <b>localhost</b> y <b>1883</b>
 * respectivamente, con las variables de entorno <b>HOMEECOCLIMATE_MQTT_ADDRESS"</b>
 * y <b>HOMEECOCLIMATE_MQTT_PORT</b>. <em>Si el servidor no puede conectarse al
 * broker no se iniciará.</em>
 */
public class Server extends AbstractVerticle {
	
	MqttClient client;
	
	@Override
	public void start(Promise<Void> startFuture) {
		client = MqttClient.create(getVertx());
		ConfigRetrieverOptions opts = new ConfigRetrieverOptions()
			.addStore(new ConfigStoreOptions().setType("env"));
		
		ConfigRetriever.create(getVertx(), opts)
			.getConfig()
			.flatMap(config ->
				client
					.connect(
						config.getInteger("HOMEECOCLIMATE_MQTT_PORT", 1883),
						config.getString("HOMEECOCLIMATE_MQTT_ADDRESS", "localhost")
					)
					.map(config)
					.map(this::configToResourceHandlers)
					.flatMap(this::publishResourceHandlers)
			)
			.onSuccess(x -> startFuture.complete())
			.onFailure(startFuture::fail);
	}
	
	private Stream<ResourceHandler<?>> configToResourceHandlers(JsonObject config) {
		final String dbUser = config.getString("HOMEECOCLIMATE_DB_USER");
		final String dbPass = config.getString("HOMEECOCLIMATE_DB_PASS");
		
		TempCmdPublisher tempCmdPub = new TempCmdPublisher(client);
		HumidCmdPublisher humidCmdPub = new HumidCmdPublisher(client);
		
		DataPool<Humidity> humidData;
		DataPool<Temperature> tempData;
		if (dbUser == null || dbPass == null) {
			System.err.println(
				"Info: Ejecutando con almacenamiento de registros no persistente"
			);
			
			humidData = new LocalNonPersistent<>();
			tempData = new LocalNonPersistent<>();
		} else {
			MySQLPool connPool = AzureMySQL.createConnPool(getVertx(), dbUser, dbPass);
			
			humidData = new AzureMySQL<>(connPool, "humidity", Humidity::new);
			tempData = new AzureMySQL<>(connPool, "temperature", Temperature::new);
		}
		
		return Stream.of(
			new ResourceHandler<>(Humidity.class, humidData, humidCmdPub::sendCmds),
			new ResourceHandler<>(Temperature.class, tempData, tempCmdPub::sendCmds)
		);
	}
	
	private Future<HttpServer> publishResourceHandlers(Stream<ResourceHandler<?>> toPublish) {
		Router router = Router.router(getVertx());
		
		toPublish.forEach(h -> 
			router.mountSubRouter(
				"/" + h.resourceName().toLowerCase(),
				h.getRouter(getVertx())
			)
		);

		return getVertx()
			.createHttpServer()
			.requestHandler(router::handle)
			.listen(8080);
	}

}
