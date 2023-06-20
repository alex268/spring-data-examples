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

import java.util.List;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * A repository for {@link LegoSet}.
 *
 * @author Jens Schauder
 */
interface LegoSetRepository extends CrudRepository<LegoSet, Integer> {

	@Query("""
			SELECT m."NAME" model_name, m.description, l.name set_name
			  FROM "MODEL" m
			  JOIN "LEGO_SET" l
			  ON m.lego_set = l.id
			  WHERE :age BETWEEN l."MIN_AGE" and l."MAX_AGE"
			""")
	List<ModelReport> reportModelForAge(@Param("age") int age);

	/**
	 * See https://stackoverflow.com/questions/52978700/how-to-write-a-custom-query-in-spring-data-jdbc
	 * @param name
	 * @return
	 */
	@Query("""
			select a.*, b."HANDBUCH_ID" as manual_handbuch_id, b.author as manual_author, b.text as manual_text from "LEGO_SET" a
			join "HANDBUCH" b on a.id = b."HANDBUCH_ID"
			where a.name = :name
			""")
	List<LegoSet> findByName(@Param("name") String name);

	@Modifying
	@Query("UPDATE \"MODEL\" set \"NAME\" = lower(\"NAME\") WHERE \"NAME\" <> lower(\"NAME\")")
	int lowerCaseMapKeys();
}
