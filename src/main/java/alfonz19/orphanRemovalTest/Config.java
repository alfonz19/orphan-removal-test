package alfonz19.orphanRemovalTest;

import alfonz19.orphanRemovalTest.jpa.ExtendedJpaRepositoryImpl;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = {"alfonz19.orphanRemovalTest.jpa"}, repositoryBaseClass = ExtendedJpaRepositoryImpl.class)
@EntityScan({"alfonz19.orphanRemovalTest.jpa.entities"})
public class Config {

	@Bean
	public DataSource customDataSource(DataSourceProperties properties) {
		HikariDataSource dataSource = properties
				.initializeDataSourceBuilder().type(HikariDataSource.class).build();

		return ProxyDataSourceBuilder.create(dataSource)
				.asJson()
				.name(properties.getName())
				.logQueryBySlf4j(SLF4JLogLevel.DEBUG)
				.build();
	}
}
