package sensor.data.pools;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlResult;
import sensor.common.DataPool;
import sensor.common.Reading;

public class AzureMySQL<T extends Reading> implements DataPool<T> {

	final private Function<Row, T> sqlRowMapper;
	final private MySQLPool mySqlClient;
	final private String tableName;
	
	public static MySQLPool createConnPool(Vertx vertx, String user, String pass) {
		return MySQLPool.pool(
			vertx,
			new MySQLConnectOptions()
				.setPort(3306)
				.setHost("homeecoclimate-db.mysql.database.azure.com")
				.setDatabase("homeecoclimate")
				.setUser(user)
				.setPassword(pass),
			new PoolOptions()
		);
	}
	
	public AzureMySQL(MySQLPool mySqlClient, String tableName, Function<Row, T> sqlRowMapper) {
		super();
		this.tableName = tableName;
		this.sqlRowMapper = sqlRowMapper;
		this.mySqlClient = mySqlClient;
	}
	
	@Override
	public Future<Set<T>> getAll() {
		return mySqlClient
			.query("SELECT * FROM " + tableName + ";")
			.collecting(Collectors.mapping(sqlRowMapper, Collectors.toUnmodifiableSet()))
			.execute()
			.map(SqlResult::value);
	}
	
	@Override
	public Future<Set<T>> getLast(long lastSeconds) {
		long lastTime = Instant.now().getEpochSecond() - lastSeconds;
		
		return mySqlClient
			.query("SELECT * FROM " + tableName + " WHERE time >= " + lastTime + ";")
			.collecting(Collectors.mapping(sqlRowMapper, Collectors.toUnmodifiableSet()))
			.execute()
			.map(SqlResult::value);
	}
	
	@Override
	public Future<Set<T>> getById(String groupId, String sensorId) {
		return mySqlClient
			.query(
				"SELECT * FROM " + tableName
				+ " WHERE groupId = '" + groupId
				+ "' AND sensorId = '" + sensorId
				+ "';")
			.collecting(Collectors.mapping(sqlRowMapper, Collectors.toUnmodifiableSet()))
			.execute()
			.map(SqlResult::value);
	}

	@Override
	public Future<Optional<T>> getByIdAndTime(
		String groupId,
		String sensorId,
		long time
	) {
		return mySqlClient
			.query(
				"SELECT * FROM " + tableName
				+ " WHERE groupId = '" + groupId
				+ "' AND sensorId = '" + sensorId
				+ "' AND time = " + time + ";"
			)
			.execute()
			.map(r ->
				r.iterator().hasNext()
					? Optional.of(r.iterator().next())
					: Optional.<Row>empty()
			)
			.map(o -> o.map(sqlRowMapper));
	}
	
	@Override
	public Future<Void> add(T elem) {
		return mySqlClient
			.query(elem.asSQLInsertQuery(tableName))
			.execute()
			.map((Void) null);
	}

}
