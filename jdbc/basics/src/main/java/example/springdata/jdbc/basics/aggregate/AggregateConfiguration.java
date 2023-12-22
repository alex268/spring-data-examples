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
package example.springdata.jdbc.basics.aggregate;

import java.sql.Clob;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.data.relational.core.dialect.AnsiDialect;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.data.relational.core.dialect.LockClause;
import org.springframework.data.relational.core.mapping.event.BeforeConvertEvent;
import org.springframework.data.relational.core.sql.IdentifierProcessing;
import org.springframework.data.relational.core.sql.LockOptions;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.lang.Nullable;

import static java.util.Arrays.*;

/**
 * @author Jens Schauder
 * @author Mark Paluch
 */
@Configuration
@EnableJdbcRepositories
public class AggregateConfiguration extends AbstractJdbcConfiguration {

	final AtomicInteger id = new AtomicInteger(0);

	@Bean
	public ApplicationListener<?> idSetting() {

		return (ApplicationListener<BeforeConvertEvent>) event -> {

			if (event.getEntity() instanceof LegoSet) {
				setIds((LegoSet) event.getEntity());
			}
		};
	}

    private void setIds(LegoSet legoSet) {

		if (legoSet.getId() == 0) {
			legoSet.setId(id.incrementAndGet());
		}

		var manual = legoSet.getManual();

		if (manual != null) {
			manual.setId((long) legoSet.getId());
		}
	}

	@Override
	public JdbcCustomConversions jdbcCustomConversions() {

		return new JdbcCustomConversions(asList(new Converter<Clob, String>() {

			@Nullable
			@Override
			public String convert(Clob clob) {

				try {

					return Math.toIntExact(clob.length()) == 0 //
							? "" //
							: clob.getSubString(1, Math.toIntExact(clob.length()));

				} catch (SQLException e) {
					throw new IllegalStateException("Failed to convert CLOB to String.", e);
				}
			}
		}));
	}

	@Bean
	public NamedParameterJdbcTemplate namedParameterJdbcTemplate(JdbcOperations operations) {
		return new NamedParameterJdbcTemplate(operations);
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
                                return LockClause.Position.AFTER_ORDER_BY;
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
