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
 * This class creates a subrouter for each resource type and binds them to the root
 * of the API routes with the class name of the resource type in lowercase as a prefix.
 * 
 * To provide the MySQL database access credentials, specify them through the environment variables 
 * <b>HOMEECOCLIMATE_DB_USER</b> for the username and <b>HOMEECOCLIMATE_DB_PASS</b> for the password.
 * 
 * You can modify the default address and port of the MQTT broker to which the server will connect, 
 * which are <b>localhost</b> and <b>1883</b> respectively, using the environment variables 
 * <b>HOMEECOCLIMATE_MQTT_ADDRESS</b> and <b>HOMEECOCLIMATE_MQTT_PORT</b>. 
 * <em>If the server cannot connect to the broker, it will not start.</em>
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
