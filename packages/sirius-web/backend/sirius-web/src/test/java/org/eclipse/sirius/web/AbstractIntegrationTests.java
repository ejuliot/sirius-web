/*******************************************************************************
 * Copyright (c) 2024 Obeo.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.sirius.web;

import org.eclipse.sirius.web.infrastructure.configuration.persistence.JDBCConfiguration;
import org.eclipse.sirius.web.starter.SiriusWebStarterConfiguration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Superclass of all the integration tests used to setup the test environment.
 *
 * <p>
 * NOTE: The annotation @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS) has been set to close jdbc connection after each test class
 * because without this annotation we have reached the max connection number.
 * </p>
 *
 * @author sbegaudeau
 */
@SpringJUnitConfig(classes = { IntegrationTestConfiguration.class, SiriusWebStarterConfiguration.class, JDBCConfiguration.class, EObjectSerializerTestConfig.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractIntegrationTests {
    public static final PostgreSQLContainer<?> POSTGRESQL_CONTAINER;

    static {
        POSTGRESQL_CONTAINER = new PostgreSQLContainer<>("postgres:latest").withReuse(true);
        POSTGRESQL_CONTAINER.start();
    }

    @DynamicPropertySource
    public static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRESQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRESQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRESQL_CONTAINER::getPassword);
    }
}
