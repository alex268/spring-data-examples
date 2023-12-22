/*
 * Copyright 2017-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package example.springdata.jdbc.basics.simpleentity;

import java.sql.Connection;
import java.util.concurrent.atomic.AtomicLong;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.data.relational.core.dialect.AnsiDialect;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.data.relational.core.dialect.LockClause;
import org.springframework.data.relational.core.mapping.event.BeforeConvertEvent;
import org.springframework.data.relational.core.mapping.event.BeforeSaveCallback;
import org.springframework.data.relational.core.mapping.event.RelationalEvent;
import org.springframework.data.relational.core.sql.IdentifierProcessing;
import org.springframework.data.relational.core.sql.LockOptions;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

/**
 * Contains infrastructure necessary for creating repositories, listeners and EntityCallbacks.
 * <p>
 * Not that a listener may change an entity without any problem.
 *
 * @author Jens Schauder
 * @author Mark Paluch
 */
@Configuration
@EnableJdbcRepositories
public class CategoryConfiguration extends AbstractJdbcConfiguration {
	final AtomicLong id = new AtomicLong(0);

    @Bean
	public ApplicationListener<?> idSetting() {

		return (ApplicationListener<BeforeConvertEvent>) event -> {

			if (event.getEntity() instanceof Category) {
				setIds((Category) event.getEntity());
			}
		};
	}

	private void setIds(Category category) {
		if (category.getId() == null) {
			category.setId(id.incrementAndGet());
		}
	}

	/**
	 * @return {@link ApplicationListener} for {@link RelationalEvent}s.
	 */
	@Bean
	public ApplicationListener<?> loggingListener() {

		return (ApplicationListener<ApplicationEvent>) event -> {
			if (event instanceof RelationalEvent) {
				System.out.println("Received an event: " + event);
			}
		};
	}

	/**
	 * @return {@link BeforeSaveCallback} for {@link Category}.
	 */
	@Bean
	public BeforeSaveCallback<Category> timeStampingSaveTime() {

		return (entity, aggregateChange) -> {

			entity.timeStamp();

			return entity;
		};
	}

    @Bean
	DataSourceInitializer initializer(DataSource dataSource, @Value("${config.schema}") String schema) {

		var initializer = new DataSourceInitializer();
		initializer.setDataSource(dataSource);

		var script = new ClassPathResource(schema);
		var populator = new ResourceDatabasePopulator(script);
		initializer.setDatabasePopulator(populator);

		return initializer;
	}

    @Override
    public Dialect jdbcDialect(NamedParameterJdbcOperations operations) {
        return operations.getJdbcOperations().execute((Connection con) -> {
            if ("YDB".equals(con.getMetaData().getDatabaseProductName())) {
                return new AnsiDialect() {
                    @Override
                    public LockClause lock() {
                        return new LockClause() {
                            @Override
                            public String getLock(LockOptions lockOptions) {
                                return "";
                            }

                            @Override
                            public LockClause.Position getClausePosition() {
                                return Position.AFTER_ORDER_BY;
                            }
                        };
                    }

                    @Override
                    public IdentifierProcessing getIdentifierProcessing() {
                        return IdentifierProcessing.create(new IdentifierProcessing.Quoting("`", "`"), IdentifierProcessing.LetterCasing.AS_IS);
                    }
                };
            }
            return super.jdbcDialect(operations);
        });
    }
}
